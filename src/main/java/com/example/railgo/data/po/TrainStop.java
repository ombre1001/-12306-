package com.example.railgo.data.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalTime;

@Data
@TableName("train_stop")
public class TrainStop {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long trainId;

    private Long stationId;

    private Integer stopSeq;

    private LocalTime arrivalTime;

    private Integer arrivalDayOffset;

    private LocalTime departureTime;

    private Integer departureDayOffset;

    private Integer distanceKm;
}