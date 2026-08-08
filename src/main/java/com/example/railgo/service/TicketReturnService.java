package com.example.railgo.service;

import com.example.railgo.data.dto.TicketReturnRequest;
import com.example.railgo.data.enums.OrderItemStatus;
import com.example.railgo.data.enums.OrderStatus;
import com.example.railgo.data.enums.RefundStatus;
import com.example.railgo.data.enums.TicketReturnStatus;
import com.example.railgo.data.po.OrderItem;
import com.example.railgo.data.po.PaymentRecord;
import com.example.railgo.data.po.RefundRecord;
import com.example.railgo.data.po.TicketReturn;
import com.example.railgo.data.vo.RefundDetailResponse;
import com.example.railgo.data.vo.TicketReturnPreviewResponse;
import com.example.railgo.data.vo.TicketReturnResponse;
import com.example.railgo.data.vo.row.ReturnableTicketRow;
import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import com.example.railgo.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketReturnService {

    private final TicketReturnMapper ticketReturnMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final RefundRecordMapper refundRecordMapper;
    private final OrderItemMapper orderItemMapper;
    private final InventoryMapper inventoryMapper;

    @Transactional(rollbackFor = Exception.class)
    public TicketReturnPreviewResponse preview(Long userId, Long ticketId) {
        ReturnableTicketRow ticket = requireReturnableTicket(userId, ticketId);
        Fee fee = calculateFee(ticket.getPrice(), ticket.getDepartureDateTime());
        return TicketReturnPreviewResponse.builder()
                .ticketId(ticket.getTicketId())
                .orderId(ticket.getOrderId())
                .orderNo(ticket.getOrderNo())
                .passengerName(ticket.getPassengerName())
                .trainNo(ticket.getTrainNo())
                .fromStationName(ticket.getFromStationName())
                .toStationName(ticket.getToStationName())
                .departureDateTime(ticket.getDepartureDateTime())
                .ticketAmount(ticket.getPrice())
                .feeRate(fee.rate())
                .feeAmount(fee.amount())
                .refundAmount(fee.refundAmount())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public TicketReturnResponse returnTicket(Long userId, Long ticketId,
                                             TicketReturnRequest request) {
        TicketReturn repeated = ticketReturnMapper.selectByUserAndClientRequestId(
                userId, request.getClientRequestId());
        if (repeated != null) {
            if (!ticketId.equals(repeated.getOrderItemId())) {
                throw new BusinessException(ErrorCode.DUPLICATE_RETURN_REQUEST);
            }
            return requireReturnDetail(userId, repeated.getId());
        }

        ReturnableTicketRow ticket = requireReturnableTicket(userId, ticketId);
        PaymentRecord payment = paymentRecordMapper.selectSuccessfulByOrderIdForUpdate(
                ticket.getOrderId());
        if (payment == null) {
            throw new BusinessException(ErrorCode.PAYMENT_RECORD_NOT_FOUND);
        }

        Fee fee = calculateFee(ticket.getPrice(), ticket.getDepartureDateTime());
        TicketReturn ticketReturn = new TicketReturn();
        ticketReturn.setReturnNo(generateNo("RT"));
        ticketReturn.setUserId(userId);
        ticketReturn.setOrderId(ticket.getOrderId());
        ticketReturn.setOrderItemId(ticketId);
        ticketReturn.setTicketAmount(ticket.getPrice());
        ticketReturn.setFeeRate(fee.rate());
        ticketReturn.setFeeAmount(fee.amount());
        ticketReturn.setRefundAmount(fee.refundAmount());
        ticketReturn.setStatus(TicketReturnStatus.PROCESSING.name());
        ticketReturn.setClientRequestId(request.getClientRequestId());
        ticketReturn.setCreatedAt(LocalDateTime.now());
        ticketReturn.setUpdatedAt(ticketReturn.getCreatedAt());
        try {
            if (ticketReturnMapper.insert(ticketReturn) != 1) {
                throw new BusinessException(ErrorCode.TICKET_RETURN_FAILED);
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_RETURN_REQUEST);
        }

        int expectedSegments = ticket.getToSeq() - ticket.getFromSeq();
        if (inventoryMapper.releaseSoldOrderItemSegments(ticketId) != expectedSegments) {
            throw new BusinessException(ErrorCode.TICKET_RETURN_FAILED, "售出库存区间与车票不一致");
        }
        OrderItem orderItem = orderItemMapper.selectById(ticketId);
        if (orderItem == null || !OrderItemStatus.ISSUED.name().equals(orderItem.getStatus())) {
            throw new BusinessException(ErrorCode.TICKET_RETURN_NOT_ALLOWED);
        }
        orderItem.setStatus(OrderItemStatus.REFUNDED.name());
        if (orderItemMapper.updateById(orderItem) != 1) {
            throw new BusinessException(ErrorCode.TICKET_RETURN_FAILED);
        }

        RefundRecord refund = new RefundRecord();
        refund.setReturnId(ticketReturn.getId());
        refund.setPaymentId(payment.getId());
        refund.setRefundNo(generateNo("RF"));
        refund.setAmount(fee.refundAmount());
        refund.setStatus(RefundStatus.SUCCESS.name());
        refund.setRefundedAt(LocalDateTime.now());
        refund.setCreatedAt(refund.getRefundedAt());
        refund.setUpdatedAt(refund.getRefundedAt());
        if (refundRecordMapper.insert(refund) != 1) {
            throw new BusinessException(ErrorCode.REFUND_CREATE_FAILED);
        }

        ticketReturn.setStatus(TicketReturnStatus.COMPLETED.name());
        ticketReturn.setCompletedAt(LocalDateTime.now());
        ticketReturn.setUpdatedAt(ticketReturn.getCompletedAt());
        if (ticketReturnMapper.updateById(ticketReturn) != 1) {
            throw new BusinessException(ErrorCode.TICKET_RETURN_FAILED);
        }

        String orderStatus = ticketReturnMapper.countIssuedTickets(ticket.getOrderId()) == 0
                ? OrderStatus.REFUNDED.name()
                : OrderStatus.PARTIALLY_REFUNDED.name();
        if (ticketReturnMapper.updateOrderStatus(ticket.getOrderId(), orderStatus) != 1) {
            throw new BusinessException(ErrorCode.TICKET_RETURN_FAILED, "订单状态更新失败");
        }
        return requireReturnDetail(userId, ticketReturn.getId());
    }

    @Transactional(readOnly = true)
    public TicketReturnResponse getReturnDetail(Long userId, Long returnId) {
        return requireReturnDetail(userId, returnId);
    }

    @Transactional(readOnly = true)
    public RefundDetailResponse getRefundDetail(Long userId, Long refundId) {
        RefundDetailResponse response = refundRecordMapper.selectOwnedRefundDetail(refundId, userId);
        if (response == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return response;
    }

    private ReturnableTicketRow requireReturnableTicket(Long userId, Long ticketId) {
        ReturnableTicketRow ticket = ticketReturnMapper.selectReturnableTicketForUpdate(ticketId, userId);
        if (ticket == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!(OrderStatus.PAID.name().equals(ticket.getOrderStatus())
                || OrderStatus.PARTIALLY_REFUNDED.name().equals(ticket.getOrderStatus()))
                || !OrderItemStatus.ISSUED.name().equals(ticket.getTicketStatus())) {
            throw new BusinessException(ErrorCode.TICKET_RETURN_NOT_ALLOWED);
        }
        if (ticket.getDepartureDateTime() == null
                || !ticket.getDepartureDateTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.TICKET_RETURN_NOT_ALLOWED, "车票已经发车，不能退票");
        }
        if (ticketReturnMapper.countActiveChange(ticketId) > 0) {
            throw new BusinessException(ErrorCode.TICKET_RETURN_NOT_ALLOWED, "车票存在未完成改签，不能退票");
        }
        return ticket;
    }

    /** 演示手续费：发车前8天免费；48小时及以上5%；24小时及以上10%；不足24小时20%。 */
    private Fee calculateFee(BigDecimal price, LocalDateTime departure) {
        long minutes = Duration.between(LocalDateTime.now(), departure).toMinutes();
        if (minutes <= 0) {
            throw new BusinessException(ErrorCode.TICKET_RETURN_NOT_ALLOWED);
        }
        BigDecimal rate;
        if (minutes >= 8L * 24 * 60) {
            rate = new BigDecimal("0.00");
        } else if (minutes >= 48L * 60) {
            rate = new BigDecimal("0.05");
        } else if (minutes >= 24L * 60) {
            rate = new BigDecimal("0.10");
        } else {
            rate = new BigDecimal("0.20");
        }
        BigDecimal fee = price.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        return new Fee(rate, fee, price.subtract(fee).setScale(2, RoundingMode.HALF_UP));
    }

    private TicketReturnResponse requireReturnDetail(Long userId, Long returnId) {
        TicketReturnResponse response = ticketReturnMapper.selectReturnDetail(returnId, userId);
        if (response == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return response;
    }

    private String generateNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private record Fee(BigDecimal rate, BigDecimal amount, BigDecimal refundAmount) {}
}
