package com.example.railgo.controller;

import com.example.railgo.data.dto.CreateOrderRequest;
import com.example.railgo.data.vo.OrderCreateResponse;
import com.example.railgo.data.vo.Result;
import com.example.railgo.security.RailUserPrincipal;
import com.example.railgo.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "订单接口", description = "创建直达订单并锁定具体座位区间")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @Operation(summary = "创建直达订单")
    @PostMapping
    public ResponseEntity<Result<OrderCreateResponse>> createOrder(
            @AuthenticationPrincipal RailUserPrincipal principal,
            @Valid @RequestBody CreateOrderRequest request) {
        return Result.success(orderService.createOrder(principal.userId(), request), "订单创建成功");
    }
}