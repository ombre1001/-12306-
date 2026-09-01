package com.example.railgo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.railgo.data.po.TicketChange;
import com.example.railgo.data.vo.ChangeDetailResponse;
import com.example.railgo.data.vo.ChangeOptionResponse;
import com.example.railgo.data.vo.row.ChangeTicketRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TicketChangeMapper extends BaseMapper<TicketChange> {

    ChangeTicketRow selectOwnedTicketForUpdate(@Param("ticketId") Long ticketId,
                                               @Param("userId") Long userId);

    TicketChange selectByClientRequestId(@Param("userId") Long userId,
                                         @Param("clientRequestId") String clientRequestId);

    TicketChange selectOwnedChange(@Param("changeId") Long changeId,
                                   @Param("userId") Long userId);

    TicketChange selectOwnedChangeForUpdate(@Param("changeId") Long changeId,
                                            @Param("userId") Long userId);

    List<TicketChange> selectPendingChangesByTicketForUpdate(
            @Param("oldTicketId") Long oldTicketId,
            @Param("userId") Long userId);

    int countSuccessfulChanges(@Param("oldTicketId") Long oldTicketId);

    int countOtherTravelConflict(@Param("userId") Long userId,
                                 @Param("passengerId") Long passengerId,
                                 @Param("excludedTicketId") Long excludedTicketId,
                                 @Param("departureDateTime") java.time.LocalDateTime departureDateTime,
                                 @Param("arrivalDateTime") java.time.LocalDateTime arrivalDateTime);

    List<ChangeOptionResponse> selectChangeOptions(@Param("ticketId") Long ticketId,
                                                   @Param("userId") Long userId,
                                                   @Param("travelDate") LocalDate travelDate);

    ChangeDetailResponse selectChangeDetail(@Param("changeId") Long changeId,
                                            @Param("userId") Long userId);

    List<Long> selectExpiredChangeIds(@Param("limit") int limit);

    int updateStatusIfExpected(@Param("changeId") Long changeId,
                               @Param("expectedStatus") String expectedStatus,
                               @Param("newStatus") String newStatus);

    int updateOrderAmount(@Param("orderId") Long orderId,
                          @Param("differenceAmount") java.math.BigDecimal differenceAmount);
}
