package com.example.railgo.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.railgo.config.TrainSyncProperties;
import com.example.railgo.data.po.Station;
import com.example.railgo.mapper.StationMapper;
import com.example.railgo.service.source.SourceStation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StationAutoRepairService {

    private final StationMapper stationMapper;
    private final StationSourceClient sourceClient;
    private final TrainSyncProperties properties;

    /**
     * 端点同时带有电报码和站名。若二者冲突，以官方主数据中“站名对应
     * 的电报码”为准，因为余票接口在环线或运行图调整时可能返回内部码。
     */
    @Transactional
    public Station resolveEndpoint(String rawCode, String rawName) {
        StationSourceClient.StationDirectory directory =
                loadDirectoryOrNull();
        SourceStation officialByName = directory == null
                ? null
                : directory.findByName(rawName);
        SourceStation officialByCode = directory == null
                ? null
                : directory.findByCode(rawCode);
        SourceStation official = officialByName != null
                ? officialByName
                : officialByCode;

        if (official != null) {
            if (officialByName != null
                    && officialByCode != null
                    && !officialByName.stationCode().equals(
                    officialByCode.stationCode())) {
                log.warn(
                        "来源端点报码与站名冲突：code={}对应{}，name={}对应{}；以站名为准",
                        rawCode,
                        officialByCode.stationName(),
                        rawName,
                        officialByName.stationCode()
                );
            }
            return upsertOfficial(official);
        }

        Station existing = findByCode(rawCode);
        if (existing == null) {
            existing = findByName(rawName);
        }
        if (existing != null) {
            log.warn("12306主数据未找到端点，保留数据库记录：code={}，name={}",
                    rawCode, rawName);
            return existing;
        }
        throw new IllegalStateException(
                "12306车站主数据和station表均缺少端点：三字码="
                        + rawCode + "，名称=" + rawName
        );
    }

    @Transactional
    public Station resolveStop(String rawName) {
        StationSourceClient.StationDirectory directory =
                loadDirectoryOrNull();
        SourceStation official = directory == null
                ? null
                : directory.findByName(rawName);
        if (official != null) {
            return upsertOfficial(official);
        }

        Station existing = findByName(rawName);
        if (existing != null) {
            log.warn("12306主数据未找到经停站{}，保留数据库现有记录", rawName);
            return existing;
        }
        throw new IllegalStateException(
                "12306车站主数据和station表均缺少车站：" + rawName
        );
    }

    private StationSourceClient.StationDirectory loadDirectoryOrNull() {
        try {
            return sourceClient.getDirectory();
        } catch (RuntimeException exception) {
            log.warn(
                    "无法读取12306车站主数据，本次仅使用station表回退：{}",
                    exception.getMessage()
            );
            return null;
        }
    }

    private Station upsertOfficial(SourceStation official) {
        Station byCode = findByCode(official.stationCode());
        Station byName = findByName(official.stationName());

        if (byCode != null && byName != null
                && !byCode.getId().equals(byName.getId())) {
            mergeDuplicateStation(byName, byCode, official);
            byCode = null;
        }

        Station station = byCode != null ? byCode : byName;
        LocalDateTime now = LocalDateTime.now();
        if (station == null) {
            station = new Station();
            applyOfficial(station, official, now);
            station.setCreatedAt(now);
            try {
                stationMapper.insert(station);
                log.info("自动补充车站：{} {}，stationId={}",
                        official.stationCode(), official.stationName(),
                        station.getId());
                return station;
            } catch (DuplicateKeyException exception) {
                // 管理端或另一线程刚好插入时，重新读取后继续校正。
                Station concurrent = findByCode(official.stationCode());
                if (concurrent == null) {
                    concurrent = findByName(official.stationName());
                }
                if (concurrent == null) {
                    throw exception;
                }
                station = concurrent;
            }
        }

        String oldCode = station.getStationCode();
        String oldName = station.getName();
        boolean changed = applyOfficial(station, official, now);
        if (changed) {
            stationMapper.updateById(station);
            log.info("自动校正车站：stationId={}，{} {} -> {} {}",
                    station.getId(), oldCode, oldName,
                    official.stationCode(), official.stationName());
        }
        return station;
    }

    /**
     * 保留按官方站名匹配到的记录，将占用正确报码的重复记录引用迁移后删除。
     * 低ID站名记录通常来自最初的完整车站导入，包含更完整的城市、地址等信息。
     */
    private void mergeDuplicateStation(
            Station keep,
            Station duplicate,
            SourceStation official
    ) {
        int affected = 0;
        affected += stationMapper.replaceTrainOriginStation(
                keep.getId(), duplicate.getId()
        );
        affected += stationMapper.replaceTrainDestinationStation(
                keep.getId(), duplicate.getId()
        );
        affected += stationMapper.replaceTrainStopStation(
                keep.getId(), duplicate.getId()
        );
        affected += stationMapper.replaceOrderItemFromStation(
                keep.getId(), duplicate.getId()
        );
        affected += stationMapper.replaceOrderItemToStation(
                keep.getId(), duplicate.getId()
        );

        int deleted = stationMapper.deleteById(duplicate.getId());
        if (deleted != 1) {
            throw new IllegalStateException(
                    "删除重复车站失败：stationId=" + duplicate.getId()
            );
        }
        log.warn(
                "自动合并重复车站：保留stationId={}，删除stationId={}，"
                        + "官方数据={} {}，迁移引用{}条",
                keep.getId(),
                duplicate.getId(),
                official.stationCode(),
                official.stationName(),
                affected
        );
    }

    private boolean applyOfficial(
            Station station,
            SourceStation official,
            LocalDateTime now
    ) {
        boolean changed = !official.stationCode().equals(station.getStationCode())
                || !official.stationName().equals(station.getName())
                || !official.stationName().equals(station.getNormalizedName())
                || !official.pinyin().equals(station.getPinyin())
                || !official.pinyinInitial().equals(station.getPinyinInitial())
                || station.getCity() == null
                || station.getCity().isBlank()
                || !Boolean.TRUE.equals(station.getPassengerService())
                || station.getLuggageService() == null
                || station.getParcelService() == null
                || station.getHotScore() == null
                || !"ACTIVE".equals(station.getStatus());

        station.setStationCode(official.stationCode());
        station.setName(official.stationName());
        station.setNormalizedName(official.stationName());
        station.setPinyin(official.pinyin());
        station.setPinyinInitial(official.pinyinInitial());
        // station_name.js不提供行政区。新站先以站名作为可检索回退值；
        // 已有车站的人工维护城市信息不会被覆盖。
        if (station.getCity() == null || station.getCity().isBlank()) {
            station.setCity(official.stationName());
        }
        station.setPassengerService(true);
        if (station.getLuggageService() == null) {
            station.setLuggageService(false);
        }
        if (station.getParcelService() == null) {
            station.setParcelService(false);
        }
        if (station.getHotScore() == null) {
            station.setHotScore(0);
        }
        station.setStatus("ACTIVE");
        station.setSourceUrl(properties.getStationNameUrl());
        station.setSourceFetchedAt(now);
        station.setUpdatedAt(now);
        return changed;
    }

    private Station findByCode(String rawCode) {
        String code = StationSourceClient.normalizeCode(rawCode);
        if (code.isBlank()) {
            return null;
        }
        return stationMapper.selectOne(
                Wrappers.<Station>lambdaQuery()
                        .eq(Station::getStationCode, code)
                        .last("LIMIT 1")
        );
    }

    private Station findByName(String rawName) {
        String name = StationSourceClient.normalizeName(rawName);
        if (name.isBlank()) {
            return null;
        }
        Station exact = stationMapper.selectOne(
                Wrappers.<Station>lambdaQuery()
                        .and(query -> query.eq(Station::getName, name)
                                .or().eq(Station::getNormalizedName, name))
                        .last("LIMIT 1")
        );
        if (exact != null) {
            return exact;
        }
        List<Station> candidates = stationMapper.selectList(
                Wrappers.<Station>lambdaQuery()
                        .and(query -> query.likeRight(Station::getName, name)
                                .or().likeRight(
                                        Station::getNormalizedName, name))
                        .last("LIMIT 20")
        );
        return candidates.stream()
                .filter(item -> name.equals(
                        StationSourceClient.normalizeName(item.getName()))
                        || name.equals(StationSourceClient.normalizeName(
                        item.getNormalizedName())))
                .findFirst()
                .orElse(null);
    }
}
