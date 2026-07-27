package com.example.railgo.data.enums;

import lombok.Getter;

@Getter
public enum TrainType {

    G(
            "G",
            "高速动车组",
            10
    ),

    D(
            "D",
            "动车组",
            20
    ),

    C(
            "C",
            "城际动车组",
            30
    ),

    Z(
            "Z",
            "直达特快",
            40
    ),

    T(
            "T",
            "特快",
            50
    ),

    K(
            "K",
            "快速",
            60
    ),

    OTHER(
            "OTHER",
            "其他列车",
            70
    );

    private final String code;

    private final String name;

    private final Integer sortNo;

    TrainType(
            String code,
            String name,
            Integer sortNo) {

        this.code = code;
        this.name = name;
        this.sortNo = sortNo;
    }
}