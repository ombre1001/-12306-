package com.example.railgo.data.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record AdminSeatGenerateRequest(

        @Valid
        @NotEmpty(message = "座位模板不能为空")
        List<CoachTemplate> templates,

        Boolean overwrite
) {
    public record CoachTemplate(

            @NotBlank(message = "车厢号不能为空")
            String coachNo,

            @NotNull(message = "起始排不能为空")
            @Min(1)
            Integer startRow,

            @NotNull(message = "结束排不能为空")
            @Min(1)
            Integer endRow,

            @NotEmpty(message = "座位字母不能为空")
            List<
                    @Pattern(
                            regexp = "[A-Z]",
                            message = "座位字母必须是单个大写字母"
                    )
                            String
                    > seatLetters
    ) {
    }
}