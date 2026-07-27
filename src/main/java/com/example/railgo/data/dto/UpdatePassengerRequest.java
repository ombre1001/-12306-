package com.example.railgo.data.dto;

import com.example.railgo.data.enums.IdType;
import com.example.railgo.data.enums.PassengerType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "修改常用乘车人请求")
public record UpdatePassengerRequest(

        @Size(
                min = 1,
                max = 50,
                message = "乘车人姓名长度必须为1至50个字符"
        )
        @Schema(example = "张三")
        String name,

        @Schema(example = "ID_CARD")
        IdType idType,

        @Size(
                min = 1,
                max = 50,
                message = "证件号码长度必须为1至50个字符"
        )
        @Schema(
                description = "不修改证件号码时不传",
                example = "11010519491231002X"
        )
        String idNo,

        @Schema(example = "ADULT")
        PassengerType passengerType,

        @Pattern(
                regexp = "^$|^1[3-9]\\d{9}$",
                message = "手机号格式不正确"
        )
        @Schema(
                description = "传空字符串表示清空手机号",
                example = "13800000000"
        )
        String phone

) {

    public boolean hasChanges() {
        return name != null
                || idType != null
                || idNo != null
                || passengerType != null
                || phone != null;
    }
}