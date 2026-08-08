package com.example.railgo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.railgo.data.vo.Result;
import com.example.railgo.data.vo.admin.*;
import com.example.railgo.service.AdminOrderReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "管理端订单与统计接口")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUSINESS_ADMIN','SYSTEM_ADMIN')")
public class AdminOrderReportController {

    private final AdminOrderReportService service;

    @Operation(summary = "多条件分页查询订单")
    @GetMapping("/orders")
    public ResponseEntity<Result<IPage<AdminOrderSummaryResponse>>> orders(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) @Positive Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String trainNo,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(service.pageOrders(page, size, orderNo, userId, status,
                trainNo, keyword, startDate, endDate));
    }

    @Operation(summary = "查询订单、票、库存和资金流水详情")
    @GetMapping("/orders/{id}")
    public ResponseEntity<Result<AdminOrderDetailResponse>> orderDetail(@PathVariable @Positive Long id) {
        return Result.success(service.getOrderDetail(id));
    }

    @Operation(summary = "分页查询支付流水")
    @GetMapping("/payments")
    public ResponseEntity<Result<IPage<AdminPaymentResponse>>> payments(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String paymentNo,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(service.pagePayments(page, size, paymentNo, orderNo, status,
                channel, startDate, endDate));
    }

    @Operation(summary = "分页查询退款流水")
    @GetMapping("/refunds")
    public ResponseEntity<Result<IPage<AdminRefundResponse>>> refunds(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String refundNo,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(service.pageRefunds(page, size, refundNo, orderNo, status, startDate, endDate));
    }

    @Operation(summary = "分页查询改签记录")
    @GetMapping("/changes")
    public ResponseEntity<Result<IPage<AdminChangeResponse>>> changes(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String changeNo,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String trainNo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(service.pageChanges(page, size, changeNo, orderNo, status,
                trainNo, startDate, endDate));
    }

    @GetMapping("/reports/sales-summary")
    public ResponseEntity<Result<AdminSalesSummaryResponse>> salesSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(service.salesSummary(startDate, endDate));
    }

    @GetMapping("/reports/sales-trend")
    public ResponseEntity<Result<List<AdminSalesTrendResponse>>> salesTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "DAY") String granularity) {
        return Result.success(service.salesTrend(startDate, endDate, granularity));
    }

    @GetMapping("/reports/popular-routes")
    public ResponseEntity<Result<List<AdminPopularRouteResponse>>> popularRoutes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String trainNo,
            @RequestParam(required = false) @Positive Long fromStationId,
            @RequestParam(required = false) @Positive Long toStationId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer limit) {
        return Result.success(service.popularRoutes(startDate, endDate, trainNo,
                fromStationId, toStationId, limit));
    }

    @GetMapping("/reports/load-factor")
    public ResponseEntity<Result<List<AdminLoadFactorResponse>>> loadFactor(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String trainNo,
            @RequestParam(required = false) String seatTypeCode) {
        return Result.success(service.loadFactor(startDate, endDate, trainNo, seatTypeCode));
    }

    @GetMapping("/reports/users")
    public ResponseEntity<Result<AdminUserStatisticsResponse>> users(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(service.userStatistics(startDate, endDate));
    }

    @GetMapping(value = "/reports/export", produces = "text/csv")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "SALES_TREND") String reportType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "DAY") String granularity,
            @RequestParam(required = false) String trainNo,
            @RequestParam(required = false) String seatTypeCode) {
        byte[] data = service.exportCsv(reportType, startDate, endDate, granularity, trainNo, seatTypeCode);
        String filename = "railgo-" + reportType.toLowerCase() + "-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(data);
    }
}
