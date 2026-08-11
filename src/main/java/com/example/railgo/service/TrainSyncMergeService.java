package com.example.railgo.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.railgo.data.po.Station;
import com.example.railgo.config.TrainSyncProperties;
import com.example.railgo.data.po.Train;
import com.example.railgo.data.po.TrainRun;
import com.example.railgo.data.po.TrainStop;
import com.example.railgo.mapper.StationMapper;
import com.example.railgo.mapper.TrainMapper;
import com.example.railgo.mapper.TrainRunMapper;
import com.example.railgo.mapper.TrainStopMapper;
import com.example.railgo.service.source.SourceStop;
import com.example.railgo.service.source.SourceTrain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainSyncMergeService {

    private final TrainMapper trainMapper;
    private final TrainStopMapper trainStopMapper;
    private final TrainRunMapper trainRunMapper;
    private final StationMapper stationMapper;
    private final TrainSyncProperties properties;

    @Transactional
    public MergeResult merge(
            LocalDate date,
            SourceTrain source,
            List<SourceStop> sourceStops
    ) {
        if (sourceStops.size() < 2) {
            throw new IllegalArgumentException(
                    source.trainNo() + "经停站少于2个"
            );
        }

        LocalDateTime now = LocalDateTime.now();
        Train train = trainMapper.selectOne(
                Wrappers.<Train>lambdaQuery()
                        .eq(Train::getTrainNo, source.trainNo())
        );
        boolean created = train == null;
        if (created) {
            train = new Train();
            train.setTrainNo(source.trainNo());
            train.setStatus("ACTIVE");
            train.setCreatedAt(now);
        }

        train.setSourceTrainCode(source.sourceTrainCode());
        train.setTrainType(source.trainType());
        train.setOriginStationId(
                requireEndpointStation(
                        source.fromStationCode(),
                        sourceStops.getFirst().stationName(),
                        "始发站"
                ).getId()
        );
        train.setDestinationStationId(
                requireEndpointStation(
                        source.toStationCode(),
                        sourceStops.getLast().stationName(),
                        "终到站"
                ).getId()
        );
        train.setUpdatedAt(now);

        if (created) {
            trainMapper.insert(train);
        } else {
            trainMapper.updateById(train);
        }

        List<TrainStop> oldStops = trainStopMapper.selectList(
                Wrappers.<TrainStop>lambdaQuery()
                        .eq(TrainStop::getTrainId, train.getId())
                        .orderByAsc(TrainStop::getStopSeq)
        );
        String newHash = scheduleHash(sourceStops);
        boolean scheduleChanged = !newHash.equals(scheduleHash(oldStops));

        if (scheduleChanged) {
            Long initializedRuns = trainRunMapper.selectCount(
                    Wrappers.<TrainRun>lambdaQuery()
                            .eq(TrainRun::getTrainId, train.getId())
                            .eq(TrainRun::getInventoryInitialized, true)
            );
            if (initializedRuns > 0 && !oldStops.isEmpty()) {
                throw new IllegalStateException(
                        source.trainNo()
                                + "时刻表发生变化，但已有运行实例初始化库存；请人工处理"
                );
            }

            trainStopMapper.delete(
                    Wrappers.<TrainStop>lambdaQuery()
                            .eq(TrainStop::getTrainId, train.getId())
            );
            for (SourceStop sourceStop : sourceStops) {
                TrainStop stop = new TrainStop();
                stop.setTrainId(train.getId());
                stop.setStationId(
                        requireStationByName(sourceStop.stationName()).getId()
                );
                stop.setStopSeq(sourceStop.stopSeq());
                stop.setArrivalTime(sourceStop.arrivalTime());
                stop.setArrivalDayOffset(sourceStop.arrivalDayOffset());
                stop.setDepartureTime(sourceStop.departureTime());
                stop.setDepartureDayOffset(sourceStop.departureDayOffset());
                stop.setDistanceKm(0);
                trainStopMapper.insert(stop);
            }
        }

        TrainRun run = trainRunMapper.selectOne(
                Wrappers.<TrainRun>lambdaQuery()
                        .eq(TrainRun::getTrainId, train.getId())
                        .eq(TrainRun::getRunDate, date)
        );
        if (run == null) {
            run = new TrainRun();
            run.setTrainId(train.getId());
            run.setRunDate(date);
            run.setSaleStatus("NOT_ON_SALE");
            run.setInventoryInitialized(false);
            run.setCreatedAt(now);
        }
        run.setSourceManaged(true);
        run.setSourceStatus("CONFIRMED");
        run.setSourceCheckedAt(now);
        run.setSourceLastSeenAt(now);
        run.setSourceValidUntil(now.plus(properties.getFreshness()));
        run.setSourceHash(newHash);
        run.setUpdatedAt(now);

        if (run.getId() == null) {
            trainRunMapper.insert(run);
        } else {
            trainRunMapper.updateById(run);
        }

        return new MergeResult(scheduleChanged, sourceStops.size());
    }

    /**
     * 12306余票接口中的始发/终到电报码偶尔会使用车次内部编码，
     * 尤其是环线车次和运行图刚调整的车次。此时仍以经停站接口
     * 返回的首站、末站名称作为可信回退，避免整趟车同步失败。
     */
    private Station requireEndpointStation(
            String stationCode,
            String stationName,
            String endpointLabel
    ) {
        Station byCode = findStationByCode(stationCode);
        if (byCode != null) {
            return byCode;
        }

        Station byName = findStationByName(stationName);
        if (byName != null) {
            log.warn(
                    "{}电报码{}在station表中不存在，已按名称{}匹配到stationId={}、现有电报码={}",
                    endpointLabel,
                    stationCode,
                    stationName,
                    byName.getId(),
                    byName.getStationCode()
            );
            return byName;
        }

        throw new IllegalStateException(
                "station表缺少" + endpointLabel
                        + "：三字码=" + stationCode
                        + "，名称=" + stationName
        );
    }

    private Station findStationByCode(String stationCode) {
        String code = stationCode == null
                ? ""
                : stationCode.trim().toUpperCase(Locale.ROOT);
        if (code.isBlank()) {
            return null;
        }

        Station station = stationMapper.selectOne(
                Wrappers.<Station>lambdaQuery()
                        .eq(Station::getStationCode, code)
                        .last("LIMIT 1")
        );
        return station;
    }

    private Station requireStationByName(String rawName) {
        Station station = findStationByName(rawName);
        if (station != null) {
            return station;
        }

        throw new IllegalStateException(
                "station表缺少车站：" + rawName
        );
    }

    private Station findStationByName(String rawName) {
        String name = normalizeStationName(rawName);
        if (name.isBlank()) {
            return null;
        }

        Station station = stationMapper.selectOne(
                Wrappers.<Station>lambdaQuery()
                        .and(query -> query.eq(Station::getName, name)
                                .or()
                                .eq(Station::getNormalizedName, name))
                        .last("LIMIT 1")
        );
        if (station != null) {
            return station;
        }

        /*
         * 兼容历史车站数据中的“嘉兴南站(高铁)”等名称。
         * 先限制前缀范围，再在Java中按同一规则规范化后严格比较，
         * 避免使用宽泛的LIKE直接误匹配到其他车站。
         */
        List<Station> candidates = stationMapper.selectList(
                Wrappers.<Station>lambdaQuery()
                        .and(query -> query
                                .likeRight(Station::getName, name)
                                .or()
                                .likeRight(Station::getNormalizedName, name))
                        .last("LIMIT 20")
        );
        return candidates.stream()
                .filter(candidate -> name.equals(
                        normalizeStationName(candidate.getName())
                ) || name.equals(
                        normalizeStationName(candidate.getNormalizedName())
                ))
                .findFirst()
                .orElse(null);
    }

    private String normalizeStationName(String name) {
        String value = name == null
                ? ""
                : name.trim()
                .replace("（", "(")
                .replace("）", ")")
                .replaceAll("\\s+", "");

        for (String suffix : List.of(
                "(高铁)", "(客运)", "(城际)", "(动车)"
        )) {
            if (value.endsWith(suffix)) {
                value = value.substring(
                        0, value.length() - suffix.length()
                );
                break;
            }
        }

        return value.endsWith("站")
                ? value.substring(0, value.length() - 1)
                : value;
    }

    private String scheduleHash(List<?> stops) {
        StringBuilder content = new StringBuilder();
        for (Object item : stops) {
            if (item instanceof SourceStop stop) {
                content.append(stop.stopSeq()).append('|')
                        .append(normalizeStationName(stop.stationName())).append('|')
                        .append(stop.arrivalTime()).append('|')
                        .append(stop.arrivalDayOffset()).append('|')
                        .append(stop.departureTime()).append('|')
                        .append(stop.departureDayOffset()).append(';');
            } else if (item instanceof TrainStop stop) {
                Station station = stationMapper.selectById(stop.getStationId());
                content.append(stop.getStopSeq()).append('|')
                        .append(station == null
                                ? stop.getStationId()
                                : normalizeStationName(station.getName()))
                        .append('|').append(stop.getArrivalTime())
                        .append('|').append(stop.getArrivalDayOffset())
                        .append('|').append(stop.getDepartureTime())
                        .append('|').append(stop.getDepartureDayOffset())
                        .append(';');
            }
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(content.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("计算时刻表摘要失败", exception);
        }
    }

    public record MergeResult(boolean scheduleChanged, int stopCount) {
    }
}