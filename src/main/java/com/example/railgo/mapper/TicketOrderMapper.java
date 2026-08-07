package com.example.railgo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.railgo.data.po.Passenger;
import com.example.railgo.data.po.TicketOrder;
import com.example.railgo.data.vo.OrderItemDetailResponse;
import com.example.railgo.data.vo.OrderPaymentStatusResponse;
import com.example.railgo.data.vo.OrderSummaryResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TicketOrderMapper extends BaseMapper<TicketOrder> {

    TicketOrder selectByClientRequestId(
            @Param("userId") Long userId,
            @Param("clientRequestId") String clientRequestId
    );

    TicketOrder selectByIdAndUserId(
            @Param("orderId") Long orderId,
            @Param("userId") Long userId
    );

    TicketOrder selectByIdAndUserIdForUpdate(
            @Param("orderId") Long orderId,
            @Param("userId") Long userId
    );

    List<Passenger> selectOwnedPassengersForUpdate(
            @Param("userId") Long userId,
            @Param("passengerIds") List<Long> passengerIds
    );

    IPage<OrderSummaryResponse> selectOrderPage(
            Page<OrderSummaryResponse> page,
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("orderDateFrom") LocalDate orderDateFrom,
            @Param("orderDateTo") LocalDate orderDateTo,
            @Param("travelDateFrom") LocalDate travelDateFrom,
            @Param("travelDateTo") LocalDate travelDateTo,
            @Param("keyword") String keyword
    );

    List<OrderItemDetailResponse> selectOrderItemDetails(
            @Param("orderId") Long orderId
    );

    OrderPaymentStatusResponse selectPaymentStatus(
            @Param("orderId") Long orderId,
            @Param("userId") Long userId
    );

    int countPassengerTravelConflict(
            @Param("userId") Long userId,
            @Param("passengerId") Long passengerId,
            @Param("departureDateTime") LocalDateTime departureDateTime,
            @Param("arrivalDateTime") LocalDateTime arrivalDateTime
    );

    List<Long> selectExpiredOrderIds(
            @Param("limit") int limit
    );

    int updateStatusIfExpected(
            @Param("orderId") Long orderId,
            @Param("expectedStatus") String expectedStatus,
            @Param("newStatus") String newStatus
    );

    int cancelLockedItems(
            @Param("orderId") Long orderId
    );
}
