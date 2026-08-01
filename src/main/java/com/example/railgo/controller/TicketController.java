package com.example.railgo.controller;

import com.example.railgo.data.dto.TicketQueryRequest;
import com.example.railgo.data.vo.DirectTicketResponse;
import com.example.railgo.data.vo.Result;
import com.example.railgo.service.TicketQueryService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
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
        return Result.success(ticketQueryService.queryDirectTickets(request)).getBody();
    }
}