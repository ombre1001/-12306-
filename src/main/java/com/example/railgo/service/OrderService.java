package com.example.railgo.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.railgo.data.dto.CreateOrderRequest;
import com.example.railgo.data.dto.OrderPassengerRequest;
import com.example.railgo.data.enums.InventoryStatus;
import com.example.railgo.data.enums.OrderItemStatus;
import com.example.railgo.data.enums.OrderStatus;
import com.example.railgo.data.po.OrderItem;
import com.example.railgo.data.po.Passenger;
import com.example.railgo.data.po.TicketOrder;
import com.example.railgo.data.vo.LockedTicketResponse;
import com.example.railgo.data.vo.OrderCreateResponse;
import com.example.railgo.data.vo.row.BookingRouteRow;
import com.example.railgo.data.vo.row.CandidateSeatRow;
import com.example.railgo.data.vo.row.SegmentLockRow;
import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import com.example.railgo.mapper.InventoryMapper;
import com.example.railgo.mapper.OrderItemMapper;
import com.example.railgo.mapper.PassengerMapper;
import com.example.railgo.mapper.TicketOrderMapper;
import com.example.railgo.mapper.TicketQueryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {
    private static final int LOCK_MINUTES = 15;
    private static final int CANDIDATE_LIMIT = 50;

    private final TicketQueryMapper ticketQueryMapper;
    private final InventoryMapper inventoryMapper;
    private final TicketOrderMapper ticketOrderMapper;
    private final OrderItemMapper orderItemMapper;
    private final PassengerMapper passengerMapper;

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public OrderCreateResponse createOrder(Long userId, CreateOrderRequest request) {
        validateRequest(userId, request);
        if (ticketOrderMapper.selectByClientRequestId(userId, request.getClientRequestId()) != null) {
            throw new BusinessException(ErrorCode.DUPLICATE_CLIENT_REQUEST);
        }

        BookingRouteRow route = ticketQueryMapper.selectBookingRoute(
                request.getRunId(), request.getFromStationId(), request.getToStationId());
        if (route == null) {
            throw new BusinessException(ErrorCode.INVALID_ROUTE);
        }
        if (!"ON_SALE".equals(route.getSaleStatus())) {
            throw new BusinessException(ErrorCode.TRAIN_NOT_ON_SALE);
        }
        if (!Integer.valueOf(1).equals(route.getInventoryInitialized())) {
            throw new BusinessException(ErrorCode.INVENTORY_NOT_INITIALIZED);
        }

        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(LOCK_MINUTES);
        TicketOrder order = insertOrder(userId, request.getClientRequestId(), expireAt);
        List<LockedTicketResponse> tickets = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderPassengerRequest item : request.getItems()) {
            LockedTicketResponse ticket = lockOneSeat(order.getId(), route, item, expireAt);
            tickets.add(ticket);
            totalAmount = totalAmount.add(ticket.getPrice());
        }

        order.setTotalAmount(totalAmount);
        if (ticketOrderMapper.updateById(order) != 1) {
            throw new BusinessException(ErrorCode.ORDER_CREATE_FAILED);
        }
        return OrderCreateResponse.builder()
                .orderId(order.getId()).orderNo(order.getOrderNo()).status(order.getStatus())
                .totalAmount(totalAmount).expireAt(expireAt)
                .remainingSeconds(Math.max(0, Duration.between(LocalDateTime.now(), expireAt).getSeconds()))
                .tickets(tickets).build();
    }

    private LockedTicketResponse lockOneSeat(Long orderId, BookingRouteRow route,
                                             OrderPassengerRequest request, LocalDateTime expireAt) {
        String seatTypeCode = request.getSeatTypeCode().toUpperCase(Locale.ROOT);
        Long seatTypeId = ticketQueryMapper.selectSeatTypeIdByCode(seatTypeCode);
        if (seatTypeId == null) {
            throw new BusinessException(ErrorCode.SEAT_TYPE_NOT_FOUND);
        }
        BigDecimal price = ticketQueryMapper.selectFarePrice(
                route.getTrainId(), route.getFromSeq(), route.getToSeq(), seatTypeId);
        if (price == null) {
            throw new BusinessException(ErrorCode.FARE_NOT_FOUND);
        }

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(orderId);
        orderItem.setPassengerId(request.getPassengerId());
        orderItem.setRunId(route.getRunId());
        orderItem.setFromStationId(route.getFromStationId());
        orderItem.setToStationId(route.getToStationId());
        orderItem.setFromSeq(route.getFromSeq());
        orderItem.setToSeq(route.getToSeq());
        orderItem.setSeatTypeId(seatTypeId);
        orderItem.setPrice(price);
        orderItem.setStatus(OrderItemStatus.LOCKED.name());
        if (orderItemMapper.insert(orderItem) != 1) {
            throw new BusinessException(ErrorCode.ORDER_ITEM_CREATE_FAILED);
        }

        List<CandidateSeatRow> candidates = inventoryMapper.selectCandidateSeats(
                route.getRunId(), seatTypeId, route.getFromSeq(), route.getToSeq(),
                normalizePreference(request.getSeatPreference()), CANDIDATE_LIMIT);
        if (candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_TICKETS);
        }

        int requiredSegments = route.getToSeq() - route.getFromSeq();
        for (CandidateSeatRow candidate : candidates) {
            List<SegmentLockRow> segments = inventoryMapper.selectSeatSegmentsForUpdate(
                    route.getRunId(), candidate.getSeatId(), route.getFromSeq(), route.getToSeq());
            if (segments.size() != requiredSegments || segments.stream().anyMatch(segment ->
                    !InventoryStatus.AVAILABLE.name().equals(segment.getStatus()))) {
                continue;
            }
            int affected = inventoryMapper.lockSeatSegments(route.getRunId(), candidate.getSeatId(),
                    route.getFromSeq(), route.getToSeq(), orderItem.getId(), expireAt);
            if (affected != requiredSegments) {
                throw new BusinessException(ErrorCode.SEAT_LOCK_FAILED);
            }
            orderItem.setSeatId(candidate.getSeatId());
            if (orderItemMapper.updateById(orderItem) != 1) {
                throw new BusinessException(ErrorCode.ORDER_ITEM_UPDATE_FAILED);
            }
            return LockedTicketResponse.builder()
                    .orderItemId(orderItem.getId()).passengerId(request.getPassengerId())
                    .seatTypeCode(seatTypeCode)
                    .seatTypeName(ticketQueryMapper.selectSeatTypeNameById(seatTypeId))
                    .coachNo(candidate.getCoachNo()).seatNo(candidate.getSeatNo()).price(price).build();
        }
        throw new BusinessException(ErrorCode.SEAT_LOCK_FAILED);
    }

    private void validateRequest(Long userId, CreateOrderRequest request) {
        if (request.getFromStationId().equals(request.getToStationId())) {
            throw new BusinessException(ErrorCode.INVALID_ROUTE);
        }
        Set<Long> ids = new HashSet<>();
        for (OrderPassengerRequest item : request.getItems()) {
            if (!ids.add(item.getPassengerId())) {
                throw new BusinessException(ErrorCode.DUPLICATE_PASSENGER);
            }
        }
        long ownedCount = passengerMapper.selectCount(Wrappers.<Passenger>lambdaQuery()
                .eq(Passenger::getUserId, userId).in(Passenger::getId, ids));
        if (ownedCount != ids.size()) {
            throw new BusinessException(ErrorCode.PASSENGER_NOT_OWNED);
        }
    }

    private TicketOrder insertOrder(Long userId, String clientRequestId, LocalDateTime expireAt) {
        TicketOrder order = new TicketOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setClientRequestId(clientRequestId);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setStatus(OrderStatus.PENDING_PAYMENT.name());
        order.setExpireAt(expireAt);
        try {
            if (ticketOrderMapper.insert(order) != 1) {
                throw new BusinessException(ErrorCode.ORDER_CREATE_FAILED);
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_CLIENT_REQUEST);
        }
        return order;
    }

    private String normalizePreference(String preference) {
        if (preference == null || preference.isBlank()) return "NONE";
        String value = preference.toUpperCase(Locale.ROOT);
        return Set.of("WINDOW", "AISLE", "NONE").contains(value) ? value : "NONE";
    }

    private String generateOrderNo() {
        return "RG" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
    }
}