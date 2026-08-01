package com.example.railgo.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long passengerId;

    private Long runId;

    private Long fromStationId;

    private Long toStationId;

    private Integer fromSeq;

    private Integer toSeq;

    private Long seatTypeId;

    private Long seatId;

    private BigDecimal price;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}