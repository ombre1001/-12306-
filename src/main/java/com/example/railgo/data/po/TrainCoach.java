package com.example.railgo.data.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("train_coach")
public class TrainCoach {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long trainId;

    private String coachNo;

    private Long seatTypeId;

    private Integer capacity;
}