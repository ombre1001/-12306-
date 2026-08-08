package com.example.railgo.controller;

import com.example.railgo.data.dto.TicketReturnRequest;
import com.example.railgo.data.vo.RefundDetailResponse;
import com.example.railgo.data.vo.Result;
import com.example.railgo.data.vo.TicketReturnPreviewResponse;
import com.example.railgo.data.vo.TicketReturnResponse;
import com.example.railgo.security.RailUserPrincipal;
import com.example.railgo.service.TicketReturnService;
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

@Tag(name = "退票退款接口", description = "退票手续费试算、确认退票和退款查询")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@Validated
public class TicketReturnController {

    private final TicketReturnService ticketReturnService;

    @Operation(summary = "试算退票手续费和退款金额")
    @GetMapping("/tickets/{ticketId}/return-preview")
    public ResponseEntity<Result<TicketReturnPreviewResponse>> preview(
            @AuthenticationPrincipal RailUserPrincipal principal,
            @PathVariable @Positive(message = "车票ID必须大于0") Long ticketId) {
        return Result.success(ticketReturnService.preview(principal.userId(), ticketId));
    }

    @Operation(summary = "确认退票并自动退款")
    @PostMapping("/tickets/{ticketId}/return")
    public ResponseEntity<Result<TicketReturnResponse>> returnTicket(
            @AuthenticationPrincipal RailUserPrincipal principal,
            @PathVariable @Positive(message = "车票ID必须大于0") Long ticketId,
            @Valid @RequestBody TicketReturnRequest request) {
        return Result.success(ticketReturnService.returnTicket(principal.userId(), ticketId, request),
                "退票成功，退款已原路退回");
    }

    @Operation(summary = "查询本人退票详情")
    @GetMapping("/returns/{returnId}")
    public ResponseEntity<Result<TicketReturnResponse>> getReturnDetail(
            @AuthenticationPrincipal RailUserPrincipal principal,
            @PathVariable @Positive(message = "退票ID必须大于0") Long returnId) {
        return Result.success(ticketReturnService.getReturnDetail(principal.userId(), returnId));
    }

    @Operation(summary = "查询本人退款详情")
    @GetMapping("/refunds/{refundId}")
    public ResponseEntity<Result<RefundDetailResponse>> getRefundDetail(
            @AuthenticationPrincipal RailUserPrincipal principal,
            @PathVariable @Positive(message = "退款ID必须大于0") Long refundId) {
        return Result.success(ticketReturnService.getRefundDetail(principal.userId(), refundId));
    }
}
