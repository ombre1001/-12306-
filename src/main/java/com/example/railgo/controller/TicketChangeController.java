package com.example.railgo.controller;

import com.example.railgo.data.dto.ChangePreviewRequest;
import com.example.railgo.data.vo.ChangeDetailResponse;
import com.example.railgo.data.vo.ChangeOptionResponse;
import com.example.railgo.data.vo.Result;
import com.example.railgo.security.RailUserPrincipal;
import com.example.railgo.service.TicketChangeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "改签接口", description = "查询候选车次、锁定新座位、补差价及确认改签")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@Validated
public class TicketChangeController {

    private final TicketChangeService ticketChangeService;

    @Operation(summary = "查询可改签车次和席别")
    @GetMapping("/tickets/{ticketId}/change-options")
    public ResponseEntity<Result<List<ChangeOptionResponse>>> listOptions(
            @AuthenticationPrincipal RailUserPrincipal principal,
            @PathVariable @Positive(message = "车票ID必须大于0") Long ticketId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDate) {
        return Result.success(ticketChangeService.listOptions(principal.userId(), ticketId, travelDate));
    }

    @Operation(summary = "锁定新座位并试算改签差价")
    @PostMapping("/tickets/{ticketId}/change-preview")
    public ResponseEntity<Result<ChangeDetailResponse>> preview(
            @AuthenticationPrincipal RailUserPrincipal principal,
            @PathVariable @Positive(message = "车票ID必须大于0") Long ticketId,
            @Valid @RequestBody ChangePreviewRequest request) {
        return Result.success(ticketChangeService.preview(principal.userId(), ticketId, request),
                "新座位已锁定，请在15分钟内完成改签");
    }

    @Operation(summary = "确认同价或低价改签")
    @PostMapping("/changes/{changeId}/confirm")
    public ResponseEntity<Result<ChangeDetailResponse>> confirm(
            @AuthenticationPrincipal RailUserPrincipal principal,
            @PathVariable @Positive(message = "改签ID必须大于0") Long changeId) {
        return Result.success(ticketChangeService.confirm(principal.userId(), changeId), "改签成功");
    }

    @Operation(summary = "模拟补款并完成高价改签")
    @PostMapping("/changes/{changeId}/pay")
    public ResponseEntity<Result<ChangeDetailResponse>> pay(
            @AuthenticationPrincipal RailUserPrincipal principal,
            @PathVariable @Positive(message = "改签ID必须大于0") Long changeId) {
        return Result.success(ticketChangeService.pay(principal.userId(), changeId), "补款成功，改签已完成");
    }

    @Operation(summary = "取消未完成改签")
    @PostMapping("/changes/{changeId}/cancel")
    public ResponseEntity<Result<ChangeDetailResponse>> cancel(
            @AuthenticationPrincipal RailUserPrincipal principal,
            @PathVariable @Positive(message = "改签ID必须大于0") Long changeId) {
        return Result.success(ticketChangeService.cancel(principal.userId(), changeId), "改签申请已取消");
    }

    @Operation(summary = "查询本人改签详情")
    @GetMapping("/changes/{changeId}")
    public ResponseEntity<Result<ChangeDetailResponse>> getDetail(
            @AuthenticationPrincipal RailUserPrincipal principal,
            @PathVariable @Positive(message = "改签ID必须大于0") Long changeId) {
        return Result.success(ticketChangeService.getDetail(principal.userId(), changeId));
    }
}
