package com.example.railgo.data.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class TicketQueryRequest {

    @NotNull(message = "出发站不能为空")
    private Long fromStationId;

    @NotNull(message = "到达站不能为空")
    private Long toStationId;

    @NotNull(message = "乘车日期不能为空")
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