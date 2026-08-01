package com.example.railgo.data.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record AdminFaresRequest(

        @Valid
        @NotEmpty(message = "票价列表不能为空")
        List<FareItem> fares
) {
    public record FareItem(

            @NotNull
            @Positive
            Integer fromSeq,

            @NotNull
            @Positive
            Integer toSeq,

            @NotNull
            @Positive
            Long seatTypeId,

            @NotNull
            @DecimalMin(value = "0.00", message = "票价不能小于0")
            @Digits(integer = 8, fraction = 2)
            BigDecimal price
    ) {
    }
}