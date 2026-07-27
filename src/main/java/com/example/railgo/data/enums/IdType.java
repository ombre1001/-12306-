package com.example.railgo.data.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "证件类型")
public enum IdType {
    ID_CARD,
    PASSPORT,
    HK_MACAO_TAIWAN,
    FOREIGN_PERMANENT_RESIDENCE
}