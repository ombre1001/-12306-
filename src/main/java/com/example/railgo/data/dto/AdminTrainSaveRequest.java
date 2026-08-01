package com.example.railgo.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminTrainSaveRequest(

        @NotBlank(message = "车次编号不能为空")
        @Size(max = 20)
        String trainNo,


        @NotNull(message = "始发站ID不能为空")
        Long fromStationId,

        @NotNull(message = "终到站ID不能为空")
        Long toStationId,

        @NotBlank(message = "车次类型不能为空")
        @Pattern(
                regexp = "G|D|C|Z|T|K|OTHER",
                message = "车次类型不合法"
        )
        String trainType,

        @NotBlank(message = "车次状态不能为空")
        @Pattern(
                regexp = "INACTIVE|ACTIVE",
                message = "车次状态只能是INACTIVE或ACTIVE"
        )
        String status
) {
    public String getTrainNo() {
        return trainNo;
    }

    public String getTrainType() {
        return trainType;
    }

    public String getStatus() {
        return status;
    }

    public Long getFromStationId() {
        return fromStationId;
    }

    public Long getToStationId() {
        return toStationId;
    }
}