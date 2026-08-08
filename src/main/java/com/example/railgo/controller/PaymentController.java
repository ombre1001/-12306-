package com.example.railgo.controller;

import com.example.railgo.data.dto.PayOrderRequest;
import com.example.railgo.data.vo.PaymentResponse;
import com.example.railgo.data.vo.Result;
import com.example.railgo.security.RailUserPrincipal;
import com.example.railgo.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "支付接口", description = "订单模拟支付和出票")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@Validated
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "支付待支付订单")
    @PostMapping("/orders/{orderId}/pay")
    public ResponseEntity<Result<PaymentResponse>> pay(
            @AuthenticationPrincipal RailUserPrincipal principal,
            @PathVariable @Positive(message = "订单ID必须大于0") Long orderId,
            @Valid @RequestBody PayOrderRequest request) {
        return Result.success(paymentService.pay(principal.userId(), orderId, request),
                "支付流水创建成功，请确认模拟支付");
    }

    @Operation(summary = "确认模拟支付成功")
    @PostMapping("/payments/{paymentNo}/confirm")
    public ResponseEntity<Result<PaymentResponse>> confirm(
            @AuthenticationPrincipal RailUserPrincipal principal,
            @PathVariable String paymentNo) {
        return Result.success(paymentService.confirm(principal.userId(), paymentNo),
                "模拟支付确认成功，车票已出票");
    }

    @Operation(summary = "查询本人支付流水")
    @GetMapping("/payments/{paymentNo}")
    public ResponseEntity<Result<PaymentResponse>> getPayment(
            @AuthenticationPrincipal RailUserPrincipal principal,
            @PathVariable String paymentNo) {
        return Result.success(paymentService.getPayment(principal.userId(), paymentNo));
    }
}
