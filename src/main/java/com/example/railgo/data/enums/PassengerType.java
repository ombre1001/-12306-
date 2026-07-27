package com.example.railgo.data.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "旅客类型")
public enum PassengerType {

    ADULT,
    CHILD,
    STUDENT
}