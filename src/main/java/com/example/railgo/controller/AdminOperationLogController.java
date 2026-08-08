package com.example.railgo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.railgo.data.po.SysOperationLog;
import com.example.railgo.data.vo.Result;
import com.example.railgo.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "后台操作日志接口")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequestMapping("/admin/operation-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('ADMIN:AUDIT:READ')")
public class AdminOperationLogController {
    private final OperationLogService service;

    @Operation(summary = "分页查询后台操作日志")
    @GetMapping
    public ResponseEntity<Result<IPage<SysOperationLog>>> page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) @Positive Long operatorId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(service.page(page, size, operatorId, module, action, result,
                requestId, startDate, endDate));
    }

    @Operation(summary = "查询操作日志详情")
    @GetMapping("/{id}")
    public ResponseEntity<Result<SysOperationLog>> detail(@PathVariable @Positive Long id) {
        return Result.success(service.detail(id));
    }
}
