package com.example.railgo.data.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "车次类型字典项")
public record TrainTypeResponse(

        @Schema(
                description = "车次类型编码",
                example = "G"
        )
        String code,

        @Schema(
                description = "车次类型名称",
                example = "高速动车组"
        )
        String name,

        @Schema(
                description = "显示顺序",
                example = "10"
        )
        Integer sortNo
) {
}