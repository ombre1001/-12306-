package com.example.railgo.data.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("train_seat")
public class TrainSeat {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long coachId;

    private String seatNo;

    private Integer rowNo;

    private String seatLetter;

    private Boolean enabled;
}