package com.example.railgo.service;

import com.example.railgo.data.dto.TicketQueryRequest;
import com.example.railgo.data.vo.DirectTicketResponse;
import com.example.railgo.data.vo.FareAvailabilityResponse;
import com.example.railgo.data.vo.row.FareAvailabilityRow;
import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import com.example.railgo.mapper.TicketQueryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketQueryService {
    private static final Set<String> SORTS = Set.of(
            "DEPARTURE_ASC", "DEPARTURE_DESC", "DURATION_ASC", "PRICE_ASC");

    private final TicketQueryMapper ticketQueryMapper;

    public List<DirectTicketResponse> queryDirectTickets(TicketQueryRequest request) {
        validateAndNormalize(request);
        List<DirectTicketResponse> runs = ticketQueryMapper.selectDirectTickets(request);
        if (runs.isEmpty()) {
            return Collections.emptyList();
        }

        List<FareAvailabilityRow> rows = ticketQueryMapper.selectFareAvailability(
                runs.stream().map(DirectTicketResponse::getRunId).toList(),
                request.getFromStationId(), request.getToStationId());

        Map<Long, List<FareAvailabilityResponse>> faresByRun = rows.stream()
                .collect(Collectors.groupingBy(FareAvailabilityRow::getRunId,
                        Collectors.mapping(this::toFareResponse, Collectors.toList())));
        runs.forEach(run -> run.setFares(
                faresByRun.getOrDefault(run.getRunId(), Collections.emptyList())));

        if ("PRICE_ASC".equals(request.getSort())) {
            runs.sort((left, right) -> minimumAvailablePrice(left)
                    .compareTo(minimumAvailablePrice(right)));
        }
        return runs;
    }

    private void validateAndNormalize(TicketQueryRequest request) {
        if (request.getFromStationId().equals(request.getToStationId())) {
            throw new BusinessException(ErrorCode.INVALID_ROUTE);
        }
        if (request.getTravelDate().isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.INVALID_TRAVEL_DATE);
        }
        if (request.getDepartureTimeStart() != null && request.getDepartureTimeEnd() != null
                && request.getDepartureTimeStart().isAfter(request.getDepartureTimeEnd())) {
            throw new BusinessException(ErrorCode.INVALID_QUERY_TIME);
        }

        String sort = request.getSort() == null ? "DEPARTURE_ASC"
                : request.getSort().toUpperCase(Locale.ROOT);
        request.setSort(SORTS.contains(sort) ? sort : "DEPARTURE_ASC");
        if (request.getTrainTypes() != null) {
            request.setTrainTypes(request.getTrainTypes().stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(value -> value.toUpperCase(Locale.ROOT))
                    .distinct().toList());
        }
    }

    private FareAvailabilityResponse toFareResponse(FareAvailabilityRow row) {
        FareAvailabilityResponse response = new FareAvailabilityResponse();
        response.setSeatTypeCode(row.getSeatTypeCode());
        response.setSeatTypeName(row.getSeatTypeName());
        response.setPrice(row.getPrice());
        response.setAvailableCount(row.getAvailableCount());
        return response;
    }

    private java.math.BigDecimal minimumAvailablePrice(DirectTicketResponse run) {
        return run.getFares().stream()
                .filter(fare -> fare.getAvailableCount() != null && fare.getAvailableCount() > 0)
                .map(FareAvailabilityResponse::getPrice)
                .min(java.math.BigDecimal::compareTo)
                .orElse(new java.math.BigDecimal("99999999"));
    }
}