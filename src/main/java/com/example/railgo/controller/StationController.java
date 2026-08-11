package com.example.railgo.controller;

import com.example.railgo.data.vo.Result;
import com.example.railgo.data.vo.StationDetailResponse;
import com.example.railgo.data.vo.StationSummaryResponse;
import com.example.railgo.service.StationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "公共车站接口",
        description = "车站联想、热门车站和车站详情"
)
@Validated
@RestController
@RequestMapping("/stations")
@RequiredArgsConstructor
public class StationController {

    private final StationService stationService;

    @Operation(
            summary = "车站联想",
            description = "支持车站中文名称、拼音、拼音首字母和城市查询"
    )
    @GetMapping("/suggest")
    public ResponseEntity<
            Result<List<StationSummaryResponse>>>
    suggest(

            @Parameter(
                    description = "中文、拼音或拼音首字母",
                    example = "jnx",
                    required = true
            )
            @RequestParam
            String keyword,

            @Parameter(
                    description = "返回数量，范围1～20"
            )
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "limit不能小于1")
            @Max(value = 20, message = "limit不能大于20")
            int limit) {

        return Result.success(
                stationService.suggest(
                        keyword,
                        limit
                )
        );
    }

    @Operation(
            summary = "查询热门车站",
            description = "返回最近30天支付成功车票中购票次数最多的出发站和到达站"
    )
    @GetMapping("/hot")
    public ResponseEntity<
            Result<List<StationSummaryResponse>>>
    getHotStations(

            @RequestParam(defaultValue = "12")
            @Min(value = 1, message = "limit不能小于1")
            @Max(value = 50, message = "limit不能大于50")
            int limit) {

        return Result.success(
                stationService.getHotStations(
                        limit
                )
        );
    }

    @Operation(
            summary = "查询车站详情"
    )
    @GetMapping("/{stationId}")
    public ResponseEntity<
            Result<StationDetailResponse>>
    getStationDetail(

            @PathVariable
            @Min(value = 1, message = "车站ID不合法")
            Long stationId) {

        return Result.success(
                stationService.getStationDetail(
                        stationId
                )
        );
    }
}