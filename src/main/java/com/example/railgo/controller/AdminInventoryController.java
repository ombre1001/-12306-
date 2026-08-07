package com.example.railgo.controller;

import com.example.railgo.data.vo.InventorySummaryResponse;
import com.example.railgo.data.vo.Result;
import com.example.railgo.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "管理端库存接口")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequestMapping("/admin/train-runs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUSINESS_ADMIN','SYSTEM_ADMIN')")
public class AdminInventoryController {
    private final InventoryService inventoryService;

    @Operation(summary = "初始化运行实例的区间库存")
    @PostMapping("/{runId}/inventory/init")
    public ResponseEntity<Result<Void>> initializeInventory(
            @PathVariable @Positive(message = "运行实例ID必须为正整数") Long runId) {
        inventoryService.initializeInventory(runId);
        return Result.ok();
    }

    @Operation(summary = "按席别查询库存汇总")
    @GetMapping("/{runId}/inventory")
    public ResponseEntity<Result<List<InventorySummaryResponse>>> getInventorySummary(
            @PathVariable @Positive(message = "运行实例ID必须为正整数") Long runId) {
        return Result.success(inventoryService.getInventorySummary(runId));
    }
}