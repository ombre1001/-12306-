package com.example.railgo.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.railgo.data.vo.admin.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AdminOrderReportMapper {

    IPage<AdminOrderSummaryResponse> selectOrderPage(
            Page<AdminOrderSummaryResponse> page,
            @Param("orderNo") String orderNo,
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("trainNo") String trainNo,
            @Param("keyword") String keyword,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    IPage<AdminPaymentResponse> selectPaymentPage(
            Page<AdminPaymentResponse> page,
            @Param("paymentNo") String paymentNo,
            @Param("orderNo") String orderNo,
            @Param("status") String status,
            @Param("channel") String channel,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    IPage<AdminRefundResponse> selectRefundPage(
            Page<AdminRefundResponse> page,
            @Param("refundNo") String refundNo,
            @Param("orderNo") String orderNo,
            @Param("status") String status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    IPage<AdminChangeResponse> selectChangePage(
            Page<AdminChangeResponse> page,
            @Param("changeNo") String changeNo,
            @Param("orderNo") String orderNo,
            @Param("status") String status,
            @Param("trainNo") String trainNo,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<AdminInventoryOccupationResponse> selectInventoryOccupations(@Param("orderId") Long orderId);

    AdminSalesSummaryResponse selectSalesSummary(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<AdminSalesTrendResponse> selectSalesTrend(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("granularity") String granularity);

    List<AdminPopularRouteResponse> selectPopularRoutes(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("trainNo") String trainNo,
            @Param("fromStationId") Long fromStationId,
            @Param("toStationId") Long toStationId,
            @Param("limit") Integer limit);

    List<AdminLoadFactorResponse> selectLoadFactor(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("trainNo") String trainNo,
            @Param("seatTypeCode") String seatTypeCode);

    AdminUserStatisticsResponse selectUserStatistics(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
