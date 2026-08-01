package com.example.railgo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.railgo.data.dto.*;
import com.example.railgo.data.po.TrainRun;
import com.example.railgo.data.vo.Result;
import com.example.railgo.service.AdminTrainRunService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Tag(name = "管理端运行计划接口")
@Validated
@RestController
@RequestMapping("/api/v1/admin/train-runs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUSINESS_ADMIN','SYSTEM_ADMIN')")
public class AdminTrainRunController {

    private final AdminTrainRunService adminTrainRunService;

    @PostMapping("/batch")
    public ResponseEntity<Result<Map<String, Integer>>> batchCreate(
            @Valid
            @RequestBody
            AdminRunBatchRequest request
    ) {
        int createdCount =
                adminTrainRunService.batchCreate(request);

        return Result.success(
                Map.of("createdCount", createdCount),
                "运行计划生成成功"
        );
    }

    @GetMapping
    public ResponseEntity<Result<IPage<TrainRun>>> page(
            @RequestParam(defaultValue = "1")
            @Min(1) long page,

            @RequestParam(defaultValue = "20")
            @Min(1) @Max(100) long size,

            @RequestParam(required = false)
            Long trainId,

            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate endDate,

            @RequestParam(required = false)
            String saleStatus
    ) {
        return Result.success(
                adminTrainRunService.page(
                        page,
                        size,
                        trainId,
                        startDate,
                        endDate,
                        saleStatus
                )
        );
    }

    @PatchMapping("/{runId}/sale-status")
    public ResponseEntity<Result<Void>> updateSaleStatus(
            @PathVariable @Positive Long runId,
            @Valid
            @RequestBody
            AdminRunStatusRequest request
    ) {
        adminTrainRunService.updateSaleStatus(
                runId,
                request.saleStatus()
        );

        return Result.ok();
    }

    @DeleteMapping("/{runId}")
    public ResponseEntity<Result<Void>> delete(
            @PathVariable @Positive Long runId
    ) {
        adminTrainRunService.delete(runId);
        return Result.ok();
    }
}