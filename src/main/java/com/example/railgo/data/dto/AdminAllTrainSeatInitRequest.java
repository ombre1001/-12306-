package com.example.railgo.data.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AdminAllTrainSeatInitRequest(

        /**
         * false：已有车厢的车次跳过
         * true：删除原车厢、座位后重新生成
         */
        Boolean overwrite,

        @NotEmpty(message = "车厢模板不能为空")
        List<@Valid CoachTemplate> templates
) {

    public record CoachTemplate(

            @NotBlank(message = "车厢号不能为空")
            @Size(max = 10, message = "车厢号长度不能超过10")
            String coachNo,

            @NotNull(message = "起始排不能为空")
            @Min(value = 1, message = "起始排必须大于等于1")
            Integer startRow,

            @NotNull(message = "结束排不能为空")
            @Min(value = 1, message = "结束排必须大于等于1")
            Integer endRow,

            @NotEmpty(message = "座位字母不能为空")
            List<
                    @NotBlank(message = "座位字母不能为空")
                    @Pattern(
                            regexp = "[A-Za-z]",
                            message = "座位字母必须是单个英文字母"
                    )
                            String
                    > seatLetters
    ) {
    }
}