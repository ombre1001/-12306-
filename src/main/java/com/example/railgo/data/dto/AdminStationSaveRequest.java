package com.example.railgo.data.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record AdminStationSaveRequest(

        @NotBlank(message = "车站编码不能为空")
        @Size(max = 20, message = "车站编码长度不能超过20")
        String stationCode,

        @NotBlank(message = "车站名称不能为空")
        @Size(max = 100)
        String name,

        @NotBlank(message = "标准化名称不能为空")
        @Size(max = 100)
        String normalizedName,

        @Size(max = 200)
        String pinyin,

        @Size(max = 50)
        String pinyinInitial,

        @Size(max = 50)
        String province,

        @Size(max = 50)
        String city,

        @Size(max = 50)
        String district,

        @Size(max = 255)
        String address,

        @Size(max = 100)
        String railwayBureau,

        Boolean passengerService,

        Boolean luggageService,

        Boolean parcelService,

        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        BigDecimal longitude,

        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        BigDecimal latitude,

        @Min(value = 0)
        Integer hotScore
) {
}