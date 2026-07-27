package com.example.railgo.controller;

import com.example.railgo.data.vo.Result;
import com.example.railgo.data.vo.SeatTypeResponse;
import com.example.railgo.data.vo.TrainTypeResponse;
import com.example.railgo.service.DictionaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(
        name = "公共字典接口",
        description = "查询席别、车次类型等公共字典数据"
)
@RestController
@RequestMapping("/api/v1/dictionaries")
@RequiredArgsConstructor
public class DictionaryController {

    private final DictionaryService dictionaryService;

    @Operation(
            summary = "查询席别字典",
            description = "从seat_type表中读取全部席别，按照sortNo升序排列"
    )
    @GetMapping("/seat-types")
    public ResponseEntity<
            Result<List<SeatTypeResponse>>>
    getSeatTypes() {

        return Result.success(
                dictionaryService.getSeatTypes()
        );
    }

    @Operation(
            summary = "查询车次类型字典",
            description = "查询系统支持的G、D、C、Z、T、K等车次类型"
    )
    @GetMapping("/train-types")
    public ResponseEntity<
            Result<List<TrainTypeResponse>>>
    getTrainTypes() {

        return Result.success(
                dictionaryService.getTrainTypes()
        );
    }
}