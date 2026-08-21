package com.example.railgo.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class TicketQueryRequest {

    /**
     * 兼容原来的车站ID查询。
     * fromStationId 和 fromStation 至少提供一个。
     */
    private Long fromStationId;

    private Long toStationId;

    /**
     * 用户直接输入的地名、车站名、拼音或车站编码。
     *
     * 示例：
     * 青岛、上海、济南西、jinan、jnx、AOH
     */
    private String fromStation;

    private String toStation;

    @Schema(hidden = true)
    private List<Long> fromStationIds;

    @Schema(hidden = true)
    private List<Long> toStationIds;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate travelDate;

    private List<String> trainTypes;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime departureTimeStart;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime departureTimeEnd;

    /**
     * DEPARTURE_ASC、DEPARTURE_DESC、
     * DURATION_ASC、PRICE_ASC
     */
    private String sort = "DEPARTURE_ASC";
}