package com.example.railgo.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.railgo.data.po.Station;
import com.example.railgo.data.vo.StationDetailResponse;
import com.example.railgo.data.vo.StationSummaryResponse;
import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import com.example.railgo.mapper.StationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class StationService {

    private static final String ACTIVE_STATUS =
            "ACTIVE";

    private final StationMapper stationMapper;

    /**
     * 根据中文、拼音、拼音首字母联想车站。
     */
    public List<StationSummaryResponse> suggest(
            String keyword,
            int limit) {

        if (keyword == null
                || keyword.isBlank()) {
            throw new BusinessException(
                    ErrorCode.PARAM_ERROR,
                    "车站查询关键字不能为空"
            );
        }

        String trimmedKeyword =
                keyword.trim();

        String normalizedKeyword =
                normalizeKeyword(trimmedKeyword);

        List<Station> stations =
                stationMapper.selectList(
                        Wrappers.<Station>lambdaQuery()
                                .eq(
                                        Station::getStatus,
                                        ACTIVE_STATUS
                                )
                                .eq(
                                        Station::getPassengerService,
                                        true
                                )
                                .and(wrapper -> wrapper
                                        .like(
                                                Station::getName,
                                                escapeLike(
                                                        trimmedKeyword
                                                )
                                        )
                                        .or()
                                        .like(
                                                Station::getNormalizedName,
                                                escapeLike(
                                                        normalizedKeyword
                                                )
                                        )
                                        .or()
                                        .like(
                                                Station::getPinyin,
                                                escapeLike(
                                                        normalizedKeyword
                                                )
                                        )
                                        .or()
                                        .like(
                                                Station::getPinyinInitial,
                                                escapeLike(
                                                        normalizedKeyword
                                                )
                                        )
                                        .or()
                                        .like(
                                                Station::getCity,
                                                escapeLike(
                                                        trimmedKeyword
                                                )
                                        )
                                )
                                .orderByDesc(
                                        Station::getHotScore
                                )
                                .orderByAsc(
                                        Station::getName
                                )
                                .last("LIMIT " + limit)
                );

        return stations.stream()
                .map(this::toSummary)
                .toList();
    }

    /**
     * 查询热门客运车站。
     */
    public List<StationSummaryResponse> getHotStations(
            int limit) {

        List<Station> stations =
                stationMapper.selectList(
                        Wrappers.<Station>lambdaQuery()
                                .eq(
                                        Station::getStatus,
                                        ACTIVE_STATUS
                                )
                                .eq(
                                        Station::getPassengerService,
                                        true
                                )
                                .orderByDesc(
                                        Station::getHotScore
                                )
                                .orderByAsc(
                                        Station::getName
                                )
                                .last("LIMIT " + limit)
                );

        return stations.stream()
                .map(this::toSummary)
                .toList();
    }

    /**
     * 查询启用状态的车站详情。
     */
    public StationDetailResponse getStationDetail(
            Long stationId) {

        Station station =
                stationMapper.selectOne(
                        Wrappers.<Station>lambdaQuery()
                                .eq(
                                        Station::getId,
                                        stationId
                                )
                                .eq(
                                        Station::getStatus,
                                        ACTIVE_STATUS
                                )
                );

        if (station == null) {
            throw new BusinessException(
                    ErrorCode.NOT_FOUND,
                    "车站不存在或已停用"
            );
        }

        return toDetail(station);
    }

    private String normalizeKeyword(
            String keyword) {

        return keyword
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
    }

    /**
     * 防止用户输入的 % 和 _ 被当成 LIKE 通配符。
     */
    private String escapeLike(
            String value) {

        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private StationSummaryResponse toSummary(
            Station station) {

        return new StationSummaryResponse(
                station.getId(),
                station.getStationCode(),
                station.getName(),
                station.getPinyin(),
                station.getPinyinInitial(),
                station.getProvince(),
                station.getCity(),
                station.getAddress()
        );
    }

    private StationDetailResponse toDetail(
            Station station) {

        return new StationDetailResponse(
                station.getId(),
                station.getStationCode(),
                station.getName(),
                station.getNormalizedName(),
                station.getPinyin(),
                station.getPinyinInitial(),
                station.getProvince(),
                station.getCity(),
                station.getDistrict(),
                station.getAddress(),
                station.getRailwayBureau(),
                station.getPassengerService(),
                station.getLuggageService(),
                station.getParcelService(),
                station.getLongitude(),
                station.getLatitude(),
                station.getHotScore(),
                station.getStatus(),
                station.getSourceUrl()
        );
    }
}