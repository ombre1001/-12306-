package com.example.railgo.controller;

import com.example.railgo.data.dto.CreateOrderRequest;
import com.example.railgo.data.dto.CreateTransferOrderRequest;
import com.example.railgo.data.vo.*;
import com.example.railgo.security.RailUserPrincipal;
import com.example.railgo.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "订单接口", description = "创建直达订单并锁定具体座位区间")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {
    private final OrderService orderService;

    @Operation(summary = "创建直达订单")
    @PostMapping
    public ResponseEntity<Result<OrderCreateResponse>> createOrder(
            @AuthenticationPrincipal RailUserPrincipal principal,
            @Valid @RequestBody CreateOrderRequest request) {
        return Result.success(orderService.createOrder(principal.userId(), request), "订单创建成功");
    }

    @Operation(summary = "创建一次换乘订单")
    @PostMapping("/transfer")
    public ResponseEntity<Result<OrderCreateResponse>> createTransferOrder(
            @AuthenticationPrincipal RailUserPrincipal principal,
            @Valid @RequestBody CreateTransferOrderRequest request) {
        return Result.success(
                orderService.createTransferOrder(principal.userId(), request),
                "换乘订单创建成功"
        );
    }

    @Operation(summary = "分页查询本人订单")
    @GetMapping
    public ResponseEntity<Result<OrderPageResponse<OrderSummaryResponse>>> listOrders(
            @AuthenticationPrincipal RailUserPrincipal principal,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "page必须大于等于1") long page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "size必须大于等于1")
            @Max(value = 100, message = "size不能大于100") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate orderDateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate orderDateTo,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDateTo,
            @RequestParam(required = false) String keyword) {
        return Result.success(orderService.listOrders(
                principal.userId(), page, size, status,
                orderDateFrom, orderDateTo, travelDateFrom, travelDateTo, keyword
        ));
    }

    @Operation(summary = "查询本人订单详情")
    @GetMapping("/{orderId}")
    public ResponseEntity<Result<OrderDetailResponse>> getOrderDetail(
            @AuthenticationPrincipal RailUserPrincipal principal,
            @PathVariable @Positive(message = "订单ID必须大于0") Long orderId) {
        return Result.success(orderService.getOrderDetail(principal.userId(), orderId));
    }

    @Operation(summary = "取消待支付订单")
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Result<OrderCancelResponse>> cancelOrder(
            @AuthenticationPrincipal RailUserPrincipal principal,
            @PathVariable @Positive(message = "订单ID必须大于0") Long orderId) {
        return Result.success(
                orderService.cancelOrder(principal.userId(), orderId),
                "订单已取消"
        );
    }

    @Operation(summary = "查询订单支付状态")
    @GetMapping("/{orderId}/payment-status")
    public ResponseEntity<Result<OrderPaymentStatusResponse>> getPaymentStatus(
            @AuthenticationPrincipal RailUserPrincipal principal,
            @PathVariable @Positive(message = "订单ID必须大于0") Long orderId) {
        return Result.success(orderService.getPaymentStatus(principal.userId(), orderId));
    }
}
