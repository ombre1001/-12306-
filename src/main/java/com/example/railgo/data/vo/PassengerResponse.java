package com.example.railgo.data.vo;

import com.example.railgo.data.enums.IdType;
import com.example.railgo.data.enums.PassengerType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "常用乘车人信息")
public record PassengerResponse(

        @Schema(description = "乘车人ID", example = "1")
        Long id,

        @Schema(description = "姓名", example = "张三")
        String name,

        @Schema(description = "证件类型", example = "ID_CARD")
        IdType idType,

        @Schema(
                description = "脱敏后的证件号码",
                example = "110105********002X"
        )
        String maskedIdNo,

        @Schema(description = "旅客类型", example = "ADULT")
        PassengerType passengerType,

        @Schema(description = "手机号", example = "13800000000")
        String phone,

        LocalDateTime createdAt,

        LocalDateTime updatedAt

) {
}