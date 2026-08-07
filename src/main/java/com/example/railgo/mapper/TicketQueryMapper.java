package com.example.railgo.mapper;

import com.example.railgo.data.dto.TicketQueryRequest;
import com.example.railgo.data.dto.TransferTicketQueryRequest;
import com.example.railgo.data.vo.DirectTicketResponse;
import com.example.railgo.data.vo.RunStopResponse;
import com.example.railgo.data.vo.row.BookingRouteRow;
import com.example.railgo.data.vo.row.FareAvailabilityRow;
import com.example.railgo.data.vo.row.TransferCandidateRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface TicketQueryMapper {

    List<DirectTicketResponse> selectDirectTickets(
            @Param("query") TicketQueryRequest query
    );

    List<FareAvailabilityRow> selectFareAvailability(
            @Param("runIds") List<Long> runIds,
            @Param("fromStationId") Long fromStationId,
            @Param("toStationId") Long toStationId
    );

    List<TransferCandidateRow> selectTransferCandidates(
            @Param("query") TransferTicketQueryRequest query
    );

    List<RunStopResponse> selectRunStops(
            @Param("runId") Long runId
    );

    int countRunById(
            @Param("runId") Long runId
    );

    BookingRouteRow selectBookingRoute(
            @Param("runId") Long runId,
            @Param("fromStationId") Long fromStationId,
            @Param("toStationId") Long toStationId
    );

    Long selectSeatTypeIdByCode(
            @Param("seatTypeCode") String seatTypeCode
    );

    String selectSeatTypeNameById(
            @Param("seatTypeId") Long seatTypeId
    );

    BigDecimal selectFarePrice(
            @Param("trainId") Long trainId,
            @Param("fromSeq") Integer fromSeq,
            @Param("toSeq") Integer toSeq,
            @Param("seatTypeId") Long seatTypeId
    );

    BookingRouteRow selectBookingRouteForUpdate(
            @Param("runId") Long runId,
            @Param("fromStationId") Long fromStationId,
            @Param("toStationId") Long toStationId
    );
}