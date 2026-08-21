package com.example.railgo.service;

import com.example.railgo.data.dto.RunFareQueryRequest;
import com.example.railgo.data.dto.TicketQueryRequest;
import com.example.railgo.data.dto.TransferTicketQueryRequest;
import com.example.railgo.data.vo.DirectTicketResponse;
import com.example.railgo.data.vo.FareAvailabilityResponse;
import com.example.railgo.data.vo.RunStopResponse;
import com.example.railgo.data.vo.TransferTicketResponse;
import com.example.railgo.data.vo.row.BookingRouteRow;
import com.example.railgo.data.vo.row.FareAvailabilityRow;
import com.example.railgo.data.vo.row.TransferCandidateRow;
import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import com.example.railgo.mapper.StationMapper;
import com.example.railgo.mapper.TicketQueryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketQueryService {

    private static final Set<String> SORTS = Set.of(
            "DEPARTURE_ASC",
            "DEPARTURE_DESC",
            "DURATION_ASC",
            "PRICE_ASC"
    );

    private static final Set<String> TRANSFER_SORTS = Set.of(
            "TOTAL_DURATION_ASC",
            "TOTAL_PRICE_ASC",
            "WAIT_TIME_ASC",
            "DEPARTURE_ASC"
    );

    private static final int TRANSFER_RESULT_LIMIT = 20;

    private static final int MAX_MATCHED_STATIONS = 100;

    private final TicketQueryMapper ticketQueryMapper;

    private final StationMapper stationMapper;

    public List<DirectTicketResponse> queryDirectTickets(
            TicketQueryRequest request
    ) {
        validateAndNormalize(request);

        List<DirectTicketResponse> runs =
                ticketQueryMapper.selectDirectTickets(request);

        if (runs.isEmpty()) {
            return Collections.emptyList();
        }

        Map<RouteKey, List<FareAvailabilityResponse>>
                faresByRoute = new HashMap<>();

        Map<StationPair, List<DirectTicketResponse>>
                runsByStationPair =
                runs.stream().collect(
                        Collectors.groupingBy(
                                run -> new StationPair(
                                        run.getFromStationId(),
                                        run.getToStationId()
                                )
                        )
                );

        runsByStationPair.forEach(
                (stationPair, routeRuns) ->
                        loadRouteFares(
                                faresByRoute,
                                routeRuns.stream()
                                        .map(DirectTicketResponse::getRunId)
                                        .distinct()
                                        .toList(),
                                stationPair.fromStationId(),
                                stationPair.toStationId()
                        )
        );

        runs.forEach(run ->
                run.setFares(
                        faresByRoute.getOrDefault(
                                new RouteKey(
                                        run.getRunId(),
                                        run.getFromStationId(),
                                        run.getToStationId()
                                ),
                                Collections.emptyList()
                        )
                )
        );

        runs = runs.stream()
                .filter(run -> !run.getFares().isEmpty())
                .collect(
                        Collectors.toCollection(ArrayList::new)
                );

        if ("PRICE_ASC".equals(request.getSort())) {
            runs.sort(
                    Comparator.comparing(
                            this::minimumAvailablePrice
                    )
            );
        }

        return runs;
    }

    public List<TransferTicketResponse> queryTransferTickets(
            TransferTicketQueryRequest request
    ) {
        validateAndNormalize(request);

        /*
         * SQL 只负责找出满足时间关系的两程候选。
         * 区间余票和最低价格在 Java 层统一装配。
         */
        List<TransferCandidateRow> candidates =
                ticketQueryMapper.selectTransferCandidates(request);

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        /*
         * 按换乘站分组批量查询两程余票。
         * 避免对每一个换乘方案执行两次独立查询。
         */
        Map<RouteKey, List<FareAvailabilityResponse>> faresByRoute =
                loadTransferFares(candidates, request);

        List<TransferTicketResponse> responses =
                new ArrayList<>();

        for (TransferCandidateRow candidate : candidates) {
            RouteKey firstRouteKey = new RouteKey(
                    candidate.getFirstRunId(),
                    candidate.getFirstFromStationId(),
                    candidate.getTransferStationId()
            );

            RouteKey secondRouteKey = new RouteKey(
                    candidate.getSecondRunId(),
                    candidate.getTransferStationId(),
                    candidate.getSecondToStationId()
            );

            List<FareAvailabilityResponse> firstFares =
                    faresByRoute.getOrDefault(
                            firstRouteKey,
                            Collections.emptyList()
                    );

            List<FareAvailabilityResponse> secondFares =
                    faresByRoute.getOrDefault(
                            secondRouteKey,
                            Collections.emptyList()
                    );

            /*
             * 两程都至少存在一种有余票的席别，
             * 才能作为有效换乘方案返回。
             */
            if (!hasAvailableFare(firstFares)
                    || !hasAvailableFare(secondFares)) {
                continue;
            }

            TransferTicketResponse response =
                    new TransferTicketResponse();

            response.setTransferStationId(
                    candidate.getTransferStationId()
            );

            response.setTransferStationCode(
                    candidate.getTransferStationCode()
            );

            response.setTransferStation(
                    candidate.getTransferStation()
            );

            response.setFirstLeg(
                    toFirstLeg(candidate, firstFares)
            );

            response.setSecondLeg(
                    toSecondLeg(candidate, secondFares)
            );

            response.setWaitMinutes(
                    candidate.getWaitMinutes()
            );

            response.setTotalDurationMinutes(
                    candidate.getTotalDurationMinutes()
            );

            response.setMinimumTotalPrice(
                    minimumAvailablePrice(firstFares)
                            .add(
                                    minimumAvailablePrice(
                                            secondFares
                                    )
                            )
            );

            responses.add(response);
        }

        responses.sort(
                transferComparator(request.getSort())
        );

        return responses.stream()
                .limit(TRANSFER_RESULT_LIMIT)
                .toList();
    }

    public List<RunStopResponse> queryRunStops(
            Long runId
    ) {
        requireRun(runId);
        return ticketQueryMapper.selectRunStops(runId);
    }

    public List<FareAvailabilityResponse> queryRunFares(
            Long runId,
            RunFareQueryRequest request
    ) {
        requireRun(runId);

        if (request.getFromStationId()
                .equals(request.getToStationId())) {
            throw new BusinessException(
                    ErrorCode.INVALID_ROUTE
            );
        }

        BookingRouteRow route =
                ticketQueryMapper.selectBookingRoute(
                        runId,
                        request.getFromStationId(),
                        request.getToStationId()
                );

        /*
         * route 为 null 表示：
         * 1. 运行对应车次不经过其中一个车站；
         * 2. 两站顺序相反；
         * 3. 站序相同。
         */
        if (route == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_ROUTE
            );
        }

        if (!"ON_SALE".equals(route.getSaleStatus())) {
            throw new BusinessException(
                    ErrorCode.TRAIN_NOT_ON_SALE
            );
        }

        if (!Integer.valueOf(1)
                .equals(route.getInventoryInitialized())) {
            throw new BusinessException(
                    ErrorCode.INVENTORY_NOT_INITIALIZED
            );
        }

        List<FareAvailabilityResponse> fares =
                ticketQueryMapper.selectFareAvailability(
                                List.of(runId),
                                request.getFromStationId(),
                                request.getToStationId()
                        )
                        .stream()
                        .map(this::toFareResponse)
                        .toList();

        if (fares.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.FARE_NOT_FOUND
            );
        }

        return fares;
    }

    private void validateAndNormalize(
            TicketQueryRequest request
    ) {
        request.setFromStationIds(
                resolveStationIds(
                        request.getFromStationId(),
                        request.getFromStation(),
                        "出发地"
                )
        );

        request.setToStationIds(
                resolveStationIds(
                        request.getToStationId(),
                        request.getToStation(),
                        "到达地"
                )
        );

        if (request.getTravelDate() == null) {
            throw new BusinessException(
                    ErrorCode.PARAM_ERROR,
                    "乘车日期不能为空"
            );
        }

        if (request.getFromStationId() != null
                && request.getFromStationId()
                .equals(request.getToStationId())) {
            throw new BusinessException(
                    ErrorCode.INVALID_ROUTE
            );
        }

        if (isSameInputLocation(
                request.getFromStation(),
                request.getToStation()
        )) {
            throw new BusinessException(
                    ErrorCode.INVALID_ROUTE
            );
        }

        if (request.getTravelDate()
                .isBefore(LocalDate.now())) {
            throw new BusinessException(
                    ErrorCode.INVALID_TRAVEL_DATE
            );
        }

        if (request.getDepartureTimeStart() != null
                && request.getDepartureTimeEnd() != null
                && request.getDepartureTimeStart()
                .isAfter(request.getDepartureTimeEnd())) {
            throw new BusinessException(
                    ErrorCode.INVALID_QUERY_TIME
            );
        }

        String sort = request.getSort() == null
                ? "DEPARTURE_ASC"
                : request.getSort()
                .toUpperCase(Locale.ROOT);

        request.setSort(
                SORTS.contains(sort)
                        ? sort
                        : "DEPARTURE_ASC"
        );

        if (request.getTrainTypes() != null) {
            request.setTrainTypes(
                    request.getTrainTypes()
                            .stream()
                            .filter(value ->
                                    value != null
                                            && !value.isBlank()
                            )
                            .map(value ->
                                    value.toUpperCase(
                                            Locale.ROOT
                                    )
                            )
                            .distinct()
                            .toList()
            );
        }
    }

    private void validateAndNormalize(
            TransferTicketQueryRequest request
    ) {
        request.setFromStationIds(
                resolveStationIds(
                        request.getFromStationId(),
                        request.getFromStation(),
                        "出发地"
                )
        );

        request.setToStationIds(
                resolveStationIds(
                        request.getToStationId(),
                        request.getToStation(),
                        "到达地"
                )
        );

        if (request.getTravelDate() == null) {
            throw new BusinessException(
                    ErrorCode.PARAM_ERROR,
                    "乘车日期不能为空"
            );
        }

        if (request.getFromStationId() != null
                && request.getFromStationId()
                .equals(request.getToStationId())) {
            throw new BusinessException(
                    ErrorCode.INVALID_ROUTE
            );
        }

        if (isSameInputLocation(
                request.getFromStation(),
                request.getToStation()
        )) {
            throw new BusinessException(
                    ErrorCode.INVALID_ROUTE
            );
        }

        if (request.getTravelDate()
                .isBefore(LocalDate.now())) {
            throw new BusinessException(
                    ErrorCode.INVALID_TRAVEL_DATE
            );
        }

        if (request.getMinTransferMinutes() == null
                || request.getMaxTransferMinutes() == null) {
            throw new BusinessException(
                    ErrorCode.PARAM_ERROR,
                    "换乘时间不能为空"
            );
        }

        if (request.getMinTransferMinutes()
                >= request.getMaxTransferMinutes()) {
            throw new BusinessException(
                    ErrorCode.INVALID_TRANSFER_TIME
            );
        }

        if (request.getMaxTransferMinutes() > 90) {
            throw new BusinessException(
                    ErrorCode.INVALID_TRANSFER_TIME,
                    "最长换乘时间不能超过90分钟"
            );
        }

        String sort = request.getSort() == null
                ? "TOTAL_DURATION_ASC"
                : request.getSort()
                .toUpperCase(Locale.ROOT);

        request.setSort(
                TRANSFER_SORTS.contains(sort)
                        ? sort
                        : "TOTAL_DURATION_ASC"
        );
    }

    private List<Long> resolveStationIds(
            Long stationId,
            String keyword,
            String fieldName
    ) {
        if (stationId != null) {
            return List.of(stationId);
        }

        if (keyword == null || keyword.isBlank()) {
            throw new BusinessException(
                    ErrorCode.PARAM_ERROR,
                    fieldName + "不能为空"
            );
        }

        String trimmedKeyword = keyword.trim();
        String normalizedKeyword = trimmedKeyword
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);

        List<Long> stationIds =
                stationMapper.selectMatchedStationIds(
                        trimmedKeyword,
                        normalizedKeyword,
                        trimmedKeyword.toUpperCase(Locale.ROOT),
                        MAX_MATCHED_STATIONS
                );

        if (stationIds.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.NOT_FOUND,
                    "没有找到与“"
                            + trimmedKeyword
                            + "”匹配的客运车站"
            );
        }

        return stationIds;
    }

    private boolean isSameInputLocation(
            String fromStation,
            String toStation
    ) {
        if (fromStation == null
                || fromStation.isBlank()
                || toStation == null
                || toStation.isBlank()) {
            return false;
        }

        return normalizeLocation(fromStation)
                .equals(normalizeLocation(toStation));
    }

    private String normalizeLocation(String value) {
        return value
                .trim()
                .replaceAll("\\s+", "")
                .replaceAll(
                        "(特别行政区|自治州|地区|市|省|盟)$",
                        ""
                )
                .toLowerCase(Locale.ROOT);
    }

    private void requireRun(Long runId) {
        if (runId == null
                || ticketQueryMapper.countRunById(runId) == 0) {
            throw new BusinessException(
                    ErrorCode.TRAIN_RUN_NOT_FOUND
            );
        }
    }

    private Map<RouteKey, List<FareAvailabilityResponse>>
    loadTransferFares(
            List<TransferCandidateRow> candidates,
            TransferTicketQueryRequest request
    ) {
        Map<RouteKey, List<FareAvailabilityResponse>> result =
                new HashMap<>();

        Map<StationPair, List<Long>> firstRoutes =
                candidates.stream()
                        .collect(
                                Collectors.groupingBy(
                                        candidate ->
                                                new StationPair(
                                                        candidate.getFirstFromStationId(),
                                                        candidate.getTransferStationId()
                                                ),
                                        Collectors.mapping(
                                                TransferCandidateRow::getFirstRunId,
                                                Collectors.collectingAndThen(
                                                        Collectors.toSet(),
                                                        ArrayList::new
                                                )
                                        )
                                )
                        );

        firstRoutes.forEach(
                (stationPair, runIds) ->
                        loadRouteFares(
                                result,
                                runIds,
                                stationPair.fromStationId(),
                                stationPair.toStationId()
                        )
        );

        Map<StationPair, List<Long>> secondRoutes =
                candidates.stream()
                        .collect(
                                Collectors.groupingBy(
                                        candidate ->
                                                new StationPair(
                                                        candidate.getTransferStationId(),
                                                        candidate.getSecondToStationId()
                                                ),
                                        Collectors.mapping(
                                                TransferCandidateRow::getSecondRunId,
                                                Collectors.collectingAndThen(
                                                        Collectors.toSet(),
                                                        ArrayList::new
                                                )
                                        )
                                )
                        );

        secondRoutes.forEach(
                (stationPair, runIds) ->
                        loadRouteFares(
                                result,
                                runIds,
                                stationPair.fromStationId(),
                                stationPair.toStationId()
                        )
        );

        return result;
    }

    private void loadRouteFares(
            Map<RouteKey, List<FareAvailabilityResponse>> target,
            List<Long> runIds,
            Long fromStationId,
            Long toStationId
    ) {
        if (runIds.isEmpty()) {
            return;
        }

        Map<Long, List<FareAvailabilityResponse>> byRun =
                ticketQueryMapper.selectFareAvailability(
                                runIds,
                                fromStationId,
                                toStationId
                        )
                        .stream()
                        .collect(
                                Collectors.groupingBy(
                                        FareAvailabilityRow::getRunId,
                                        Collectors.mapping(
                                                this::toFareResponse,
                                                Collectors.toList()
                                        )
                                )
                        );

        runIds.forEach(runId ->
                target.put(
                        new RouteKey(
                                runId,
                                fromStationId,
                                toStationId
                        ),
                        byRun.getOrDefault(
                                runId,
                                Collections.emptyList()
                        )
                )
        );
    }

    private DirectTicketResponse toFirstLeg(
            TransferCandidateRow row,
            List<FareAvailabilityResponse> fares
    ) {
        DirectTicketResponse response =
                new DirectTicketResponse();

        response.setRunId(row.getFirstRunId());
        response.setTrainNo(row.getFirstTrainNo());
        response.setTrainType(row.getFirstTrainType());
        response.setOriginStation(
                row.getFirstOriginStation()
        );
        response.setTerminalStation(
                row.getFirstTerminalStation()
        );
        response.setFromStationId(
                row.getFirstFromStationId()
        );
        response.setFromStation(
                row.getFirstFromStation()
        );
        response.setToStationId(
                row.getTransferStationId()
        );
        response.setToStation(
                row.getFirstToStation()
        );
        response.setFromSeq(row.getFirstFromSeq());
        response.setToSeq(row.getFirstToSeq());
        response.setDepartureDateTime(
                row.getFirstDepartureDateTime()
        );
        response.setArrivalDateTime(
                row.getFirstArrivalDateTime()
        );
        response.setDurationMinutes(
                row.getFirstDurationMinutes()
        );
        response.setFares(fares);

        return response;
    }

    private DirectTicketResponse toSecondLeg(
            TransferCandidateRow row,
            List<FareAvailabilityResponse> fares
    ) {
        DirectTicketResponse response =
                new DirectTicketResponse();

        response.setRunId(row.getSecondRunId());
        response.setTrainNo(row.getSecondTrainNo());
        response.setTrainType(row.getSecondTrainType());
        response.setOriginStation(
                row.getSecondOriginStation()
        );
        response.setTerminalStation(
                row.getSecondTerminalStation()
        );
        response.setFromStationId(
                row.getTransferStationId()
        );
        response.setFromStation(
                row.getSecondFromStation()
        );
        response.setToStationId(
                row.getSecondToStationId()
        );
        response.setToStation(
                row.getSecondToStation()
        );
        response.setFromSeq(row.getSecondFromSeq());
        response.setToSeq(row.getSecondToSeq());
        response.setDepartureDateTime(
                row.getSecondDepartureDateTime()
        );
        response.setArrivalDateTime(
                row.getSecondArrivalDateTime()
        );
        response.setDurationMinutes(
                row.getSecondDurationMinutes()
        );
        response.setFares(fares);

        return response;
    }

    private boolean hasAvailableFare(
            List<FareAvailabilityResponse> fares
    ) {
        return fares.stream().anyMatch(
                fare ->
                        fare.getAvailableCount() != null
                                && fare.getAvailableCount() > 0
        );
    }

    private Comparator<TransferTicketResponse>
    transferComparator(String sort) {
        Comparator<TransferTicketResponse> comparator =
                switch (sort) {
                    case "TOTAL_PRICE_ASC" ->
                            Comparator.comparing(
                                    TransferTicketResponse
                                            ::getMinimumTotalPrice
                            );

                    case "WAIT_TIME_ASC" ->
                            Comparator.comparing(
                                    TransferTicketResponse
                                            ::getWaitMinutes
                            );

                    case "DEPARTURE_ASC" ->
                            Comparator.comparing(
                                    response ->
                                            response.getFirstLeg()
                                                    .getDepartureDateTime()
                            );

                    default ->
                            Comparator.comparing(
                                    TransferTicketResponse
                                            ::getTotalDurationMinutes
                            );
                };

        return comparator.thenComparing(
                response ->
                        response.getFirstLeg()
                                .getDepartureDateTime()
        );
    }

    private FareAvailabilityResponse toFareResponse(
            FareAvailabilityRow row
    ) {
        FareAvailabilityResponse response =
                new FareAvailabilityResponse();

        response.setSeatTypeCode(
                row.getSeatTypeCode()
        );
        response.setSeatTypeName(
                row.getSeatTypeName()
        );
        response.setPrice(row.getPrice());
        response.setAvailableCount(
                row.getAvailableCount()
        );

        return response;
    }

    private BigDecimal minimumAvailablePrice(
            DirectTicketResponse run
    ) {
        return minimumAvailablePrice(
                run.getFares()
        );
    }

    private BigDecimal minimumAvailablePrice(
            List<FareAvailabilityResponse> fares
    ) {
        return fares.stream()
                .filter(fare ->
                        fare.getAvailableCount() != null
                                && fare.getAvailableCount() > 0
                )
                .map(FareAvailabilityResponse::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(
                        new BigDecimal("99999999")
                );
    }

    private record RouteKey(
            Long runId,
            Long fromStationId,
            Long toStationId
    ) {
    }

    private record StationPair(
            Long fromStationId,
            Long toStationId
    ) {
    }
}