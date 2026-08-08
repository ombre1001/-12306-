package com.example.railgo.mapper;

import com.example.railgo.data.vo.InventorySummaryResponse;
import com.example.railgo.data.vo.row.CandidateSeatRow;
import com.example.railgo.data.vo.row.SegmentLockRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface InventoryMapper {

    String selectRunSaleStatusForUpdate(@Param("runId") Long runId);

    int countTrainStops(@Param("runId") Long runId);

    int countTrainSeats(@Param("runId") Long runId);

    int insertMissingSegmentInventory(@Param("runId") Long runId);

    int countInventoryRows(@Param("runId") Long runId);

    int markInventoryInitialized(@Param("runId") Long runId);

    List<InventorySummaryResponse> selectInventorySummary(
            @Param("runId") Long runId
    );

    List<CandidateSeatRow> selectCandidateSeats(
            @Param("runId") Long runId,
            @Param("seatTypeId") Long seatTypeId,
            @Param("fromSeq") Integer fromSeq,
            @Param("toSeq") Integer toSeq,
            @Param("seatPreference") String seatPreference,
            @Param("limit") Integer limit
    );

    List<SegmentLockRow> selectSeatSegmentsForUpdate(
            @Param("runId") Long runId,
            @Param("seatId") Long seatId,
            @Param("fromSeq") Integer fromSeq,
            @Param("toSeq") Integer toSeq
    );

    int lockSeatSegments(
            @Param("runId") Long runId,
            @Param("seatId") Long seatId,
            @Param("fromSeq") Integer fromSeq,
            @Param("toSeq") Integer toSeq,
            @Param("orderItemId") Long orderItemId,
            @Param("expireAt") LocalDateTime expireAt
    );

    int releaseOrderItemSegments(
            @Param("orderItemId") Long orderItemId
    );

    int releaseOrderSegments(
            @Param("orderId") Long orderId
    );

    int sellLockedOrderItemSegments(@Param("orderItemId") Long orderItemId);

    int releaseSoldOrderItemSegments(@Param("orderItemId") Long orderItemId);

    int countLockedOrderSegments(@Param("orderId") Long orderId);

    int sellLockedOrderSegments(@Param("orderId") Long orderId);
}
