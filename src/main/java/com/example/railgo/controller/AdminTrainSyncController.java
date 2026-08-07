package com.example.railgo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.railgo.data.dto.TrainSyncRequest;
import com.example.railgo.data.po.TrainSyncLog;
import com.example.railgo.data.vo.Result;
import com.example.railgo.data.vo.TrainSyncSummary;
import com.example.railgo.service.TrainSyncService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/admin/train-sync")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class AdminTrainSyncController {

    private final TrainSyncService trainSyncService;

    @PostMapping
    public ResponseEntity<Result<TrainSyncSummary>> sync(
            @Valid @RequestBody TrainSyncRequest request
    ) {
        return Result.success(trainSyncService.syncRange(
                request.startDate(), request.endDate()
        ));
    }

    @GetMapping("/logs")
    public ResponseEntity<Result<IPage<TrainSyncLog>>> logs(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size
    ) {
        return Result.success(trainSyncService.pageLogs(page, size));
    }
}