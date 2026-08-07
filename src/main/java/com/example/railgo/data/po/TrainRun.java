package com.example.railgo.data.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("train_run")
public class TrainRun {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long trainId;

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