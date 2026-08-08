package com.example.railgo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.railgo.data.dto.*;
import com.example.railgo.data.po.Station;
import com.example.railgo.data.vo.Result;
import com.example.railgo.service.AdminStationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端车站接口")
@Validated
@RestController
@RequestMapping("/admin/stations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('BUSINESS:STATION:WRITE')")
public class AdminStationController {

    private final AdminStationService adminStationService;

    @GetMapping
    public ResponseEntity<Result<IPage<Station>>> page(
            @RequestParam(defaultValue = "1")
            @Min(1) long page,

            @RequestParam(defaultValue = "20")
            @Min(1) @Max(100) long size,

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            String status
    ) {
        return Result.success(
                adminStationService.page(
                        page,
                        size,
                        keyword,
                        status
                )
        );
    }

    @GetMapping("/{stationId}")
    public ResponseEntity<Result<Station>> detail(
            @PathVariable @Positive Long stationId
    ) {
        return Result.success(
                adminStationService.getById(stationId)
        );
    }

    @PostMapping
    public ResponseEntity<Result<Station>> create(
            @Valid
            @RequestBody
            AdminStationSaveRequest request
    ) {
        return Result.success(
                adminStationService.create(request),
                "车站创建成功"
        );
    }

    @PutMapping("/{stationId}")
    public ResponseEntity<Result<Station>> update(
            @PathVariable @Positive Long stationId,
            @Valid
            @RequestBody
            AdminStationSaveRequest request
    ) {
        return Result.success(
                adminStationService.update(
                        stationId,
                        request
                ),
                "车站修改成功"
        );
    }

    @PatchMapping("/{stationId}/status")
    public ResponseEntity<Result<Void>> updateStatus(
            @PathVariable @Positive Long stationId,
            @Valid
            @RequestBody
            AdminStationStatusRequest request
    ) {
        adminStationService.updateStatus(
                stationId,
                request.status()
        );

        return Result.ok();
    }
}
