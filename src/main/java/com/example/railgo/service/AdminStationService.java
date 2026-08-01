package com.example.railgo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.railgo.data.dto.AdminStationSaveRequest;
import com.example.railgo.data.po.Station;
import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import com.example.railgo.mapper.StationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminStationService {

    private final StationMapper stationMapper;

    public IPage<Station> page(
            long page,
            long size,
            String keyword,
            String status
    ) {
        return stationMapper.selectPage(
                new Page<>(page, size),
                Wrappers.<Station>lambdaQuery()
                        .and(keyword != null && !keyword.isBlank(), wrapper ->
                                wrapper.like(Station::getName, keyword.trim())
                                        .or()
                                        .like(Station::getStationCode, keyword.trim())
                                        .or()
                                        .like(Station::getPinyin, keyword.trim())
                                        .or()
                                        .like(Station::getCity, keyword.trim())
                        )
                        .eq(
                                status != null && !status.isBlank(),
                                Station::getStatus,
                                status
                        )
                        .orderByDesc(Station::getHotScore)
                        .orderByAsc(Station::getId)
        );
    }

    @Transactional
    public Station create(AdminStationSaveRequest request) {
        checkUnique(request.stationCode(), request.name(), null);

        LocalDateTime now = LocalDateTime.now();

        Station station = new Station();
        fill(station, request);
        station.setStatus("ACTIVE");
        station.setCreatedAt(now);
        station.setUpdatedAt(now);

        stationMapper.insert(station);
        return station;
    }

    @Transactional
    public Station update(
            Long stationId,
            AdminStationSaveRequest request
    ) {
        Station station = requireStation(stationId);

        checkUnique(
                request.stationCode(),
                request.name(),
                stationId
        );

        fill(station, request);
        station.setUpdatedAt(LocalDateTime.now());

        stationMapper.updateById(station);
        return station;
    }

    @Transactional
    public void updateStatus(
            Long stationId,
            String status
    ) {
        if (!"ACTIVE".equals(status)
                && !"INACTIVE".equals(status)) {
            throw new BusinessException(
                    ErrorCode.STATION_STATUS_INVALID
            );
        }

        Station station = requireStation(stationId);
        station.setStatus(status);
        station.setUpdatedAt(LocalDateTime.now());
        stationMapper.updateById(station);
    }

    public Station getById(Long stationId) {
        return requireStation(stationId);
    }

    private Station requireStation(Long stationId) {
        Station station = stationMapper.selectById(stationId);

        if (station == null) {
            throw new BusinessException(
                    ErrorCode.STATION_NOT_FOUND
            );
        }

        return station;
    }

    private void checkUnique(
            String stationCode,
            String name,
            Long excludeId
    ) {
        String code = stationCode.trim()
                .toUpperCase(Locale.ROOT);

        Long codeCount = stationMapper.selectCount(
                Wrappers.<Station>lambdaQuery()
                        .eq(Station::getStationCode, code)
                        .ne(excludeId != null, Station::getId, excludeId)
        );

        if (codeCount > 0) {
            throw new BusinessException(
                    ErrorCode.STATION_CODE_EXISTS
            );
        }

        Long nameCount = stationMapper.selectCount(
                Wrappers.<Station>lambdaQuery()
                        .eq(Station::getName, name.trim())
                        .ne(excludeId != null, Station::getId, excludeId)
        );

        if (nameCount > 0) {
            throw new BusinessException(
                    ErrorCode.STATION_NAME_EXISTS
            );
        }
    }

    private void fill(
            Station station,
            AdminStationSaveRequest request
    ) {
        station.setStationCode(
                request.stationCode().trim()
                        .toUpperCase(Locale.ROOT)
        );
        station.setName(request.name().trim());
        station.setNormalizedName(
                request.normalizedName().trim()
        );
        station.setPinyin(trim(request.pinyin()));
        station.setPinyinInitial(
                trim(request.pinyinInitial())
        );
        station.setProvince(trim(request.province()));
        station.setCity(trim(request.city()));
        station.setDistrict(trim(request.district()));
        station.setAddress(trim(request.address()));
        station.setRailwayBureau(
                trim(request.railwayBureau())
        );
        station.setPassengerService(
                request.passengerService() == null
                        || request.passengerService()
        );
        station.setLuggageService(
                Boolean.TRUE.equals(request.luggageService())
        );
        station.setParcelService(
                Boolean.TRUE.equals(request.parcelService())
        );
        station.setLongitude(request.longitude());
        station.setLatitude(request.latitude());
        station.setHotScore(
                request.hotScore() == null
                        ? 0
                        : request.hotScore()
        );
    }

    private String trim(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}