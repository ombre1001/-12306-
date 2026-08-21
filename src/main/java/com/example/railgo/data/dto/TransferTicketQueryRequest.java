package com.example.railgo.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class TransferTicketQueryRequest {

    private Long fromStationId;

    private Long toStationId;

    private String fromStation;

    private String toStation;

    /** Service 解析出的所在地全部车站ID，仅供 Mapper 使用。 */
    @Schema(hidden = true)
    private java.util.List<Long> fromStationIds;

    @Schema(hidden = true)
    private java.util.List<Long> toStationIds;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate travelDate;

    @NotNull(message = "最短换乘时间不能为空")
    @Min(value = 1, message = "最短换乘时间必须大于0分钟")
    @Max(value = 1440, message = "最短换乘时间不能超过1440分钟")
    private Integer minTransferMinutes = 30;

    @NotNull(message = "最长换乘时间不能为空")
    @Min(value = 1, message = "最长换乘时间必须大于0分钟")
    @Max(value = 90, message = "最长换乘时间不能超过90分钟")
    private Integer maxTransferMinutes = 90;

    /**
     * TOTAL_DURATION_ASC：总历时升序
     * TOTAL_PRICE_ASC：最低总价升序
     * WAIT_TIME_ASC：换乘等待时间升序
     * DEPARTURE_ASC：第一程发车时间升序
     */
    private String sort = "TOTAL_DURATION_ASC";
}