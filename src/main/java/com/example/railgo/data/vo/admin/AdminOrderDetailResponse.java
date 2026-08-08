package com.example.railgo.data.vo.admin;

import com.example.railgo.data.po.ChangeFundRecord;
import com.example.railgo.data.po.PaymentRecord;
import com.example.railgo.data.po.RefundRecord;
import com.example.railgo.data.po.TicketChange;
import com.example.railgo.data.po.TicketReturn;
import com.example.railgo.data.vo.OrderItemDetailResponse;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AdminOrderDetailResponse {
    private Long orderId;
    private String orderNo;
    private Long userId;
    private String userPhone;
    private String userNickname;
    private String status;
    private BigDecimal totalAmount;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemDetailResponse> items;
    private List<AdminInventoryOccupationResponse> inventoryOccupations;
    private List<PaymentRecord> payments;
    private List<TicketReturn> returns;
    private List<RefundRecord> refunds;
    private List<TicketChange> changes;
    private List<ChangeFundRecord> changeFunds;
}
