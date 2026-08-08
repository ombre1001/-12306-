package com.example.railgo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.railgo.data.dto.*;
import com.example.railgo.data.po.*;
import com.example.railgo.data.vo.Result;
import com.example.railgo.service.AdminTrainService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "管理端车次、座席和票价接口")
@Validated
@RestController
@RequestMapping("/admin/trains")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('BUSINESS:TRAIN:WRITE')")
public class AdminTrainController {

    private final AdminTrainService adminTrainService;

    @GetMapping
    public ResponseEntity<Result<IPage<Train>>> page(
            @RequestParam(defaultValue = "1")
            @Min(1) long page,

            @RequestParam(defaultValue = "20")
            @Min(1) @Max(100) long size,

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            String trainType,

            @RequestParam(required = false)
            String status
    ) {
        return Result.success(
                adminTrainService.page(
                        page,
                        size,
                        keyword,
                        trainType,
                        status
                )
        );
    }

    @PostMapping
    public ResponseEntity<Result<Train>> create(
            @Valid
            @RequestBody
            AdminTrainSaveRequest request
    ) {
        return Result.success(
                adminTrainService.createTrain(request),
                "车次创建成功"
        );
    }

    @GetMapping("/{trainId}")
    public ResponseEntity<Result<Train>> detail(
            @PathVariable @Positive Long trainId
    ) {
        return Result.success(
                adminTrainService.getTrain(trainId)
        );
    }

    @PutMapping("/{trainId}")
    public ResponseEntity<Result<Train>> update(
            @PathVariable @Positive Long trainId,
            @Valid
            @RequestBody
            AdminTrainSaveRequest request
    ) {
        return Result.success(
                adminTrainService.updateTrain(
                        trainId,
                        request
                ),
                "车次修改成功"
        );
    }

    @GetMapping("/{trainId}/stops")
    public ResponseEntity<Result<List<TrainStop>>> getStops(
            @PathVariable @Positive Long trainId
    ) {
        return Result.success(
                adminTrainService.getStops(trainId)
        );
    }

    @PutMapping("/{trainId}/stops")
    public ResponseEntity<Result<Void>> saveStops(
            @PathVariable @Positive Long trainId,
            @Valid
            @RequestBody
            AdminTrainStopsRequest request
    ) {
        adminTrainService.saveStops(trainId, request);
        return Result.ok();
    }

    @GetMapping("/{trainId}/coaches")
    public ResponseEntity<Result<List<TrainCoach>>> getCoaches(
            @PathVariable @Positive Long trainId
    ) {
        return Result.success(
                adminTrainService.getCoaches(trainId)
        );
    }

    @PutMapping("/{trainId}/coaches")
    public ResponseEntity<Result<Void>> saveCoaches(
            @PathVariable @Positive Long trainId,
            @Valid
            @RequestBody
            AdminCoachesRequest request
    ) {
        adminTrainService.saveCoaches(trainId, request);
        return Result.ok();
    }

    @PostMapping("/{trainId}/seats/generate")
    public ResponseEntity<Result<Map<String, Integer>>> generateSeats(
            @PathVariable @Positive Long trainId,
            @Valid
            @RequestBody
            AdminSeatGenerateRequest request
    ) {
        int count = adminTrainService.generateSeats(
                trainId,
                request
        );

        return Result.success(
                Map.of("generatedCount", count),
                "座位生成成功"
        );
    }

    @GetMapping("/{trainId}/fares")
    public ResponseEntity<Result<List<TrainFare>>> getFares(
            @PathVariable @Positive Long trainId
    ) {
        return Result.success(
                adminTrainService.getFares(trainId)
        );
    }

    @PutMapping("/{trainId}/fares")
    public ResponseEntity<Result<Void>> saveFares(
            @PathVariable @Positive Long trainId,
            @Valid
            @RequestBody
            AdminFaresRequest request
    ) {
        adminTrainService.saveFares(trainId, request);
        return Result.ok();
    }

    @PostMapping("/seats/init-all")
    public ResponseEntity<Result<AdminAllTrainSeatInitResult>> initializeAllTrainSeats(
            @Valid
            @RequestBody
            AdminAllTrainSeatInitRequest request
    ) {
        return Result.success(
                adminTrainService.initializeAllTrainSeats(request),
                "所有车次车厢和座位初始化完成"
        );
    }
}
