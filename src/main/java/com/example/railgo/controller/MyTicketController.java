package com.example.railgo.controller;

import com.example.railgo.data.vo.MyTicketDetailResponse;
import com.example.railgo.data.vo.MyTicketResponse;
import com.example.railgo.data.vo.OrderPageResponse;
import com.example.railgo.data.vo.Result;
import com.example.railgo.security.RailUserPrincipal;
import com.example.railgo.service.MyTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "本人车票接口", description = "查询当前登录用户已出票及历史车票")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Validated
public class MyTicketController {

    private final MyTicketService myTicketService;

    @Operation(summary = "分页查询本人所有车票")
    @GetMapping
    public ResponseEntity<Result<OrderPageResponse<MyTicketResponse>>> list(
            @AuthenticationPrincipal RailUserPrincipal principal,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String status) {
        return Result.success(myTicketService.list(principal.userId(), page, size, status));
    }

    @Operation(summary = "查询本人车票详情")
    @GetMapping("/{ticketId}")
    public ResponseEntity<Result<MyTicketDetailResponse>> detail(
            @AuthenticationPrincipal RailUserPrincipal principal,
            @PathVariable @Positive(message = "车票ID必须大于0") Long ticketId) {
        return Result.success(myTicketService.detail(principal.userId(), ticketId));
    }
}
