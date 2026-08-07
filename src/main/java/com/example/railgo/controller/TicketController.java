package com.example.railgo.controller;

import com.example.railgo.data.dto.RunFareQueryRequest;
import com.example.railgo.data.dto.TicketQueryRequest;
import com.example.railgo.data.dto.TransferTicketQueryRequest;
import com.example.railgo.data.vo.DirectTicketResponse;
import com.example.railgo.data.vo.FareAvailabilityResponse;
import com.example.railgo.data.vo.Result;
import com.example.railgo.data.vo.RunStopResponse;
import com.example.railgo.data.vo.TransferTicketResponse;
import com.example.railgo.service.TicketQueryService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketQueryService ticketQueryService;

    @GetMapping("/direct")
    @Operation(summary = "查询直达车票")
    public Result<List<DirectTicketResponse>> queryDirectTickets(
            @Valid
            @ModelAttribute
            @ParameterObject
            TicketQueryRequest request
    ) {
        return Result.success(
                ticketQueryService.queryDirectTickets(request)
        ).getBody();
    }

    @GetMapping("/transfers")
    @Operation(summary = "查询一次换乘方案")
    public Result<List<TransferTicketResponse>> queryTransferTickets(
            @Valid
            @ModelAttribute
            @ParameterObject
            TransferTicketQueryRequest request
    ) {
        return Result.success(
                ticketQueryService.queryTransferTickets(request)
        ).getBody();
    }

    @GetMapping("/runs/{runId}/stops")
    @Operation(summary = "查询运行实例的全部经停站时刻")
    public Result<List<RunStopResponse>> queryRunStops(
            @PathVariable Long runId
    ) {
        return Result.success(
                ticketQueryService.queryRunStops(runId)
        ).getBody();
    }

    @GetMapping("/runs/{runId}/fares")
    @Operation(summary = "查询指定运行和区间的各席别票价余票")
    public Result<List<FareAvailabilityResponse>> queryRunFares(
            @PathVariable Long runId,
            @Valid
            @ModelAttribute
            @ParameterObject
            RunFareQueryRequest request
    ) {
        return Result.success(
                ticketQueryService.queryRunFares(runId, request)
        ).getBody();
    }
}