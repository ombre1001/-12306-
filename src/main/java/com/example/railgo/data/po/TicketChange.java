package com.example.railgo.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("change_record")
public class TicketChange {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String changeNo;
    private Long userId;
    private Long oldOrderItemId;
    private Long newOrderItemId;
    private BigDecimal originalAmount;
    private BigDecimal newAmount;
    private BigDecimal differenceAmount;
    private String differenceType;
    private String status;
    private String clientRequestId;
    private LocalDateTime expireAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
