package com.example.railgo.data.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "席别字典项")
public record SeatTypeResponse(

        @Schema(
                description = "席别ID",
                example = "3"
        )
        Long id,

        @Schema(
                description = "席别编码",
                example = "SECOND_CLASS"
        )
        String code,

        @Schema(
                description = "席别名称",
                example = "二等座"
        )
        String name,

        @Schema(
                description = "显示顺序",
                example = "20"
        )
        Integer sortNo
) {
}