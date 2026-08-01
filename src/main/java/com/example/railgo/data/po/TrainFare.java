package com.example.railgo.data.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("train_fare")
public class TrainFare {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long trainId;

    private Integer fromSeq;

    private Integer toSeq;

    private Long seatTypeId;

    private BigDecimal price;
}