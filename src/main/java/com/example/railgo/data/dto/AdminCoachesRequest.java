package com.example.railgo.data.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record AdminCoachesRequest(

        @Valid
        @NotEmpty(message = "车厢编组不能为空")
        List<CoachItem> coaches
) {
    public record CoachItem(

            @NotBlank(message = "车厢号不能为空")
            @Size(max = 10)
            String coachNo,

            @NotNull(message = "席别ID不能为空")
            @Positive
            Long seatTypeId
    ) {
    }
}