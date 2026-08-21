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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StationService {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private static final String LOCAL_CODE_PREFIX = "LOCAL-";

    private static final int HOT_STATION_RECENT_DAYS = 30;

    private final StationMapper stationMapper;

    /**
     * 根据中文、拼音、拼音首字母联想车站。
     *
     * 会将“三家店站”“三家店”以及
     * “上海南站(既有)”“上海南”等历史别名归并。
     *
     * 同名车站优先返回具有12306正式三字码的记录，
     * 不优先返回LOCAL-开头的历史记录。
     */
    public List<StationSummaryResponse> suggest(
            String keyword,
            int limit
    ) {
        if (keyword == null || keyword.isBlank()) {
            throw new BusinessException(
                    ErrorCode.PARAM_ERROR,
                    "车站查询关键字不能为空"
            );
        }

        int safeLimit = Math.max(1, Math.min(limit, 50));

        String trimmedKeyword = keyword.trim();

        String normalizedKeyword =
                normalizeSearchText(trimmedKeyword);

        /*
         * 必须先查询比最终limit更多的候选记录，
         * 然后再归一化去重。
         *
         * 如果直接在SQL中LIMIT 10，
         * 重复站点会提前占满10条结果。
         */
        int candidateLimit = Math.min(
                Math.max(safeLimit * 20, 100),
                500
        );

        List<Station> candidates =
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
                                                escapeLike(trimmedKeyword)
                                        )
                                        .or()
                                        .like(
                                                Station::getNormalizedName,
                                                escapeLike(normalizedKeyword)
                                        )
                                        .or()
                                        .like(
                                                Station::getPinyin,
                                                escapeLike(normalizedKeyword)
                                        )
                                        .or()
                                        .like(
                                                Station::getPinyinInitial,
                                                escapeLike(normalizedKeyword)
                                        )
                                        .or()
                                        .like(
                                                Station::getCity,
                                                escapeLike(trimmedKeyword)
                                        )
                                )
                                .last(
                                        "LIMIT " + candidateLimit
                                )
                );

        /*
         * 先排序再去重。
         *
         * 排序优先级：
         * 1. 归一化名称完全匹配；
         * 2. 归一化名称前缀匹配；
         * 3. 12306正式三字码；
         * 4. 热度；
         * 5. 名称长度和字典序。
         */
        candidates.sort(
                Comparator
                        .<Station>comparingInt(
                                station -> matchRank(
                                        station,
                                        normalizedKeyword
                                )
                        )
                        .thenComparingInt(
                                this::sourceRank
                        )
                        .thenComparing(
                                (Station station) ->
                                        station.getHotScore() == null
                                                ? 0
                                                : station.getHotScore(),
                                Comparator.reverseOrder()
                        )
                        .thenComparingInt(
                                station ->
                                        canonicalStationName(
                                                station.getName()
                                        ).length()
                        )
                        .thenComparing(
                                station ->
                                        station.getName() == null
                                                ? ""
                                                : station.getName()
                        )
        );

        /*
         * key使用归一化后的标准站名。
         * putIfAbsent保证保留排序后优先级最高的正式车站。
         */
        Map<String, Station> deduplicated =
                new LinkedHashMap<>();

        for (Station station : candidates) {
            String canonicalName =
                    canonicalStationName(
                            firstNotBlank(
                                    station.getNormalizedName(),
                                    station.getName()
                            )
                    );

            if (canonicalName.isBlank()) {
                continue;
            }

            Station existing =
                    deduplicated.get(canonicalName);

            if (existing == null
                    || shouldReplace(existing, station)) {
                deduplicated.put(
                        canonicalName,
                        station
                );
            }
        }

        List<StationSummaryResponse> result =
                new ArrayList<>();

        for (Station station : deduplicated.values()) {
            result.add(toSummary(station));

            if (result.size() >= safeLimit) {
                break;
            }
        }

        return result;
    }

    /**
     * 判断新记录是否应该替换已经选中的同名记录。
     */
    private boolean shouldReplace(
            Station existing,
            Station candidate
    ) {
        boolean existingLocal =
                isLocalStation(existing);

        boolean candidateLocal =
                isLocalStation(candidate);

        /*
         * 正式三字码记录替换LOCAL历史记录。
         */
        if (existingLocal && !candidateLocal) {
            return true;
        }

        if (!existingLocal && candidateLocal) {
            return false;
        }

        /*
         * 同为正式记录或同为LOCAL记录时，
         * 优先保留名称更简洁的记录。
         */
        String existingName =
                existing.getName() == null
                        ? ""
                        : existing.getName();

        String candidateName =
                candidate.getName() == null
                        ? ""
                        : candidate.getName();

        return candidateName.length()
                < existingName.length();
    }

    /**
     * 搜索匹配优先级。
     */
    private int matchRank(
            Station station,
            String keyword
    ) {
        String canonicalName =
                canonicalStationName(
                        firstNotBlank(
                                station.getNormalizedName(),
                                station.getName()
                        )
                );

        String pinyin =
                normalizeSearchText(
                        station.getPinyin()
                );

        String pinyinInitial =
                normalizeSearchText(
                        station.getPinyinInitial()
                );

        String city =
                canonicalStationName(
                        station.getCity()
                );

        if (canonicalName.equals(keyword)) {
            return 0;
        }

        if (canonicalName.startsWith(keyword)) {
            return 1;
        }

        if (pinyin.equals(keyword)
                || pinyinInitial.equals(keyword)) {
            return 2;
        }

        if (pinyin.startsWith(keyword)
                || pinyinInitial.startsWith(keyword)) {
            return 3;
        }

        if (canonicalName.contains(keyword)) {
            return 4;
        }

        if (city.equals(keyword)) {
            return 5;
        }

        if (city.contains(keyword)) {
            return 6;
        }

        return 7;
    }

    /**
     * 数据来源优先级：
     * 0表示正式12306三字码；
     * 1表示LOCAL历史记录。
     */
    private int sourceRank(Station station) {
        return isLocalStation(station) ? 1 : 0;
    }

    private boolean isLocalStation(Station station) {
        String stationCode =
                station.getStationCode();

        return stationCode == null
                || stationCode.isBlank()
                || stationCode
                .toUpperCase(Locale.ROOT)
                .startsWith(LOCAL_CODE_PREFIX);
    }

    /**
     * 将历史站名转换为用于比较的标准名称。
     *
     * 三家店站 -> 三家店
     * 上海南站(既有) -> 上海南
     * 上海虹桥站（高铁） -> 上海虹桥
     * 上海站(既有/高铁) -> 上海
     */
    private String canonicalStationName(
            String rawName
    ) {
        if (rawName == null) {
            return "";
        }

        String value = rawName
                .trim()
                .replaceAll("\\s+", "")
                .replace('（', '(')
                .replace('）', ')');

        /*
         * 先删除结尾括号说明。
         */
        value = value.replaceFirst(
                "\\([^)]*\\)$",
                ""
        );

        /*
         * 删除括号后，再删除结尾的“站”。
         */
        if (value.endsWith("站")
                && value.length() > 1) {
            value = value.substring(
                    0,
                    value.length() - 1
            );
        }

        return value
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeSearchText(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
    }

    private String firstNotBlank(
            String first,
            String second
    ) {
        if (first != null
                && !first.isBlank()) {
            return first;
        }

        return second == null ? "" : second;
    }

    /**
     * 防止输入的%和_成为LIKE通配符。
     */
    private String escapeLike(
            String value
    ) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /**
     * 查询最近30天购买次数最多的车站。
     */
    public List<StationSummaryResponse> getHotStations(
            int limit
    ) {
        List<Station> stations =
                stationMapper
                        .selectRecentlyPurchasedHotStations(
                                HOT_STATION_RECENT_DAYS,
                                limit
                        );

        return stations.stream()
                .map(this::toSummary)
                .toList();
    }

    /**
     * 查询启用状态的车站详情。
     */
    public StationDetailResponse getStationDetail(
            Long stationId
    ) {
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

    private StationSummaryResponse toSummary(
            Station station
    ) {
        /*
         * 返回数据库正式记录的名称。
         * 例如正式记录会显示“上海虹桥”，
         * 不显示“上海虹桥站(高铁)”。
         */
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
            Station station
    ) {
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