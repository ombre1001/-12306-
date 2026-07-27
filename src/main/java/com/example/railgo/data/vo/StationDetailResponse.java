package com.example.railgo.data.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "车站详细信息")
public record StationDetailResponse(

        Long id,

        String stationCode,

        String name,

        String normalizedName,

        String pinyin,

        String pinyinInitial,

        String province,

        String city,

        String district,

        String address,

        String railwayBureau,

        Boolean passengerService,

        Boolean luggageService,

        Boolean parcelService,

        BigDecimal longitude,

        BigDecimal latitude,

        Integer hotScore,

        String status,

        String sourceUrl
) {
}