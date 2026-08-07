package com.example.railgo.data.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrderCancelResponse {

    private Long orderId;
    private String orderNo;
    private String status;
    private LocalDateTime cancelledAt;
    private Integer releasedSegmentCount;
}
