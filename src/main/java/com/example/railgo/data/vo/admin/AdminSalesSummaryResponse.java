package com.example.railgo.data.vo.admin;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminSalesSummaryResponse {
    private Long orderCount;
    private Long paidOrderCount;
    private Long ticketCount;
    private BigDecimal grossSales;
    private BigDecimal refundAmount;
    private BigDecimal returnFeeAmount;
    private BigDecimal changePaymentAmount;
    private BigDecimal changeRefundAmount;
    private BigDecimal netRevenue;
}
