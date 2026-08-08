package com.example.railgo.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ticket_return")
public class TicketReturn {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String returnNo;
    private Long userId;
    private Long orderId;
    private Long orderItemId;
    private BigDecimal ticketAmount;
    private BigDecimal feeRate;
    private BigDecimal feeAmount;
    private BigDecimal refundAmount;
    private String status;
    private String clientRequestId;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
