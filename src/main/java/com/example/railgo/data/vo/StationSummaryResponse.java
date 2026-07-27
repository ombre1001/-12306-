package com.example.railgo.data.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "车站概要信息")
public record StationSummaryResponse(

        @Schema(description = "车站ID", example = "101")
        Long id,

        @Schema(description = "车站编码", example = "JNKVK")
        String stationCode,

        @Schema(description = "车站名称", example = "济南西")
        String name,

        @Schema(description = "完整拼音", example = "jinanxi")
        String pinyin,

        @Schema(description = "拼音首字母", example = "jnx")
        String pinyinInitial,

        @Schema(description = "所在省份", example = "山东省")
        String province,

        @Schema(description = "所在城市", example = "济南市")
        String city,

        @Schema(description = "详细地址")
        String address
) {
}