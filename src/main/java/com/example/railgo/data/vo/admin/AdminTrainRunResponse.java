package com.example.railgo.data.vo.admin;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AdminTrainRunResponse {

    private Long id;

    private Long trainId;

    private String trainNo;

    private LocalDate runDate;

    private String saleStatus;

    private Boolean inventoryInitialized;

    private LocalDateTime inventoryInitializedAt;

    private Boolean sourceManaged;

    private String sourceStatus;

    private LocalDateTime sourceCheckedAt;

    private LocalDateTime sourceLastSeenAt;

    private LocalDateTime sourceValidUntil;

    private String sourceHash;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}