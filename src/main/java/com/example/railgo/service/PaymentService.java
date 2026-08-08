package com.example.railgo.service;

import com.example.railgo.data.dto.PayOrderRequest;
import com.example.railgo.data.enums.OrderStatus;
import com.example.railgo.data.enums.PaymentStatus;
import com.example.railgo.data.po.PaymentRecord;
import com.example.railgo.data.po.TicketOrder;
import com.example.railgo.data.vo.PaymentResponse;
import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import com.example.railgo.exception.OrderExpiredException;
import com.example.railgo.mapper.InventoryMapper;
import com.example.railgo.mapper.PaymentRecordMapper;
import com.example.railgo.mapper.TicketOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TicketOrderMapper ticketOrderMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final InventoryMapper inventoryMapper;

    /** 创建 PROCESSING 支付流水，不在此步骤出票。 */
    @Transactional(rollbackFor = Exception.class, noRollbackFor = OrderExpiredException.class)
    public PaymentResponse pay(Long userId, Long orderId, PayOrderRequest request) {
        TicketOrder order = ticketOrderMapper.selectByIdAndUserIdForUpdate(orderId, userId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        PaymentRecord repeated = paymentRecordMapper.selectByOrderAndClientRequestId(
                orderId, request.getClientRequestId());
        if (repeated != null) {
            return buildResponse(order, repeated);
        }

        if (OrderStatus.PAID.name().equals(order.getStatus())
                || OrderStatus.PARTIALLY_REFUNDED.name().equals(order.getStatus())
                || OrderStatus.REFUNDED.name().equals(order.getStatus())) {
            PaymentRecord successful = paymentRecordMapper.selectSuccessfulByOrderIdForUpdate(orderId);
            if (successful == null) {
                throw new BusinessException(ErrorCode.PAYMENT_RECORD_NOT_FOUND);
            }
            return buildResponse(order, successful);
        }
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "当前订单不能支付");
        }
        if (order.getExpireAt() != null && !order.getExpireAt().isAfter(LocalDateTime.now())) {
            expireOrder(orderId);
            throw new OrderExpiredException();
        }

        PaymentRecord processing = paymentRecordMapper.selectProcessingByOrderIdForUpdate(orderId);
        if (processing != null) {
            return buildResponse(order, processing);
        }

        PaymentRecord payment = new PaymentRecord();
        payment.setOrderId(orderId);
        payment.setPaymentNo(generateNo("PAY"));
        payment.setChannel(request.getChannel().trim().toUpperCase(Locale.ROOT));
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.PROCESSING.name());
        payment.setClientRequestId(request.getClientRequestId());
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(payment.getCreatedAt());
        try {
            if (paymentRecordMapper.insert(payment) != 1) {
                throw new BusinessException(ErrorCode.PAYMENT_CREATE_FAILED);
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT_REQUEST);
        }
        return buildResponse(order, payment);
    }

    /** 将 PROCESSING 流水确认成功，并在同一事务内完成出票与库存售出。 */
    @Transactional(rollbackFor = Exception.class, noRollbackFor = OrderExpiredException.class)
    public PaymentResponse confirm(Long userId, String paymentNo) {
        String normalizedNo = normalizePaymentNo(paymentNo);
        PaymentRecord lookup = paymentRecordMapper.selectOwnedByPaymentNo(normalizedNo, userId);
        if (lookup == null) {
            throw new BusinessException(ErrorCode.PAYMENT_RECORD_NOT_FOUND);
        }
        // 统一采用“订单行 -> 支付流水行”的加锁顺序，避免与创建支付并发时形成死锁。
        TicketOrder order = ticketOrderMapper.selectByIdAndUserIdForUpdate(lookup.getOrderId(), userId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        PaymentRecord payment = paymentRecordMapper.selectOwnedByPaymentNoForUpdate(normalizedNo, userId);
        if (payment == null) {
            throw new BusinessException(ErrorCode.PAYMENT_RECORD_NOT_FOUND);
        }

        if (PaymentStatus.SUCCESS.name().equals(payment.getStatus())) {
            return buildResponse(order, payment);
        }
        if (!PaymentStatus.PROCESSING.name().equals(payment.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "当前支付流水不能确认");
        }
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "当前订单不能确认支付");
        }
        if (order.getExpireAt() != null && !order.getExpireAt().isAfter(LocalDateTime.now())) {
            expireOrder(order.getId());
            payment.setStatus(PaymentStatus.CLOSED.name());
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRecordMapper.updateById(payment);
            throw new OrderExpiredException();
        }

        int lockedItems = ticketOrderMapper.countLockedItems(order.getId());
        int lockedSegments = inventoryMapper.countLockedOrderSegments(order.getId());
        if (lockedItems <= 0 || lockedSegments <= 0) {
            throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED, "订单锁座数据不完整");
        }
        if (inventoryMapper.sellLockedOrderSegments(order.getId()) != lockedSegments
                || ticketOrderMapper.issueLockedItems(order.getId()) != lockedItems
                || ticketOrderMapper.updateStatusIfExpected(
                order.getId(), OrderStatus.PENDING_PAYMENT.name(), OrderStatus.PAID.name()) != 1) {
            throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED);
        }

        LocalDateTime paidAt = LocalDateTime.now();
        payment.setStatus(PaymentStatus.SUCCESS.name());
        payment.setPaidAt(paidAt);
        payment.setUpdatedAt(paidAt);
        if (paymentRecordMapper.updateById(payment) != 1) {
            throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED);
        }
        order.setStatus(OrderStatus.PAID.name());
        order.setUpdatedAt(paidAt);
        return buildResponse(order, payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long userId, String paymentNo) {
        PaymentRecord payment = paymentRecordMapper.selectOwnedByPaymentNo(
                normalizePaymentNo(paymentNo), userId);
        if (payment == null) {
            throw new BusinessException(ErrorCode.PAYMENT_RECORD_NOT_FOUND);
        }
        TicketOrder order = ticketOrderMapper.selectByIdAndUserId(payment.getOrderId(), userId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return buildResponse(order, payment);
    }

    private String normalizePaymentNo(String paymentNo) {
        if (paymentNo == null || paymentNo.isBlank() || paymentNo.length() > 40) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "支付流水号不合法");
        }
        return paymentNo.trim();
    }

    private void expireOrder(Long orderId) {
        if (ticketOrderMapper.updateStatusIfExpected(orderId,
                OrderStatus.PENDING_PAYMENT.name(), OrderStatus.EXPIRED.name()) == 1) {
            inventoryMapper.releaseOrderSegments(orderId);
            ticketOrderMapper.cancelLockedItems(orderId);
        }
    }

    private PaymentResponse buildResponse(TicketOrder order, PaymentRecord payment) {
        return PaymentResponse.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .orderStatus(order.getStatus())
                .paymentId(payment.getId())
                .paymentNo(payment.getPaymentNo())
                .paymentStatus(payment.getStatus())
                .channel(payment.getChannel())
                .amount(payment.getAmount())
                .paidAt(payment.getPaidAt())
                .build();
    }

    private String generateNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 6).toUpperCase(Locale.ROOT);
    }
}
