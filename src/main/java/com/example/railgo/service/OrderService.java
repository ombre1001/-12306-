package com.example.railgo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.railgo.data.dto.CreateOrderRequest;
import com.example.railgo.data.dto.CreateTransferOrderRequest;
import com.example.railgo.data.dto.OrderPassengerRequest;
import com.example.railgo.data.dto.TransferOrderPassengerRequest;
import com.example.railgo.data.enums.InventoryStatus;
import com.example.railgo.data.enums.OrderItemStatus;
import com.example.railgo.data.enums.OrderStatus;
import com.example.railgo.data.po.OrderItem;
import com.example.railgo.data.po.Passenger;
import com.example.railgo.data.po.TicketOrder;
import com.example.railgo.data.vo.*;
import com.example.railgo.data.vo.row.BookingRouteRow;
import com.example.railgo.data.vo.row.CandidateSeatRow;
import com.example.railgo.data.vo.row.SegmentLockRow;
import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import com.example.railgo.mapper.InventoryMapper;
import com.example.railgo.mapper.OrderItemMapper;
import com.example.railgo.mapper.TicketOrderMapper;
import com.example.railgo.mapper.TicketQueryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final int LOCK_MINUTES = 15;
    private static final int CANDIDATE_LIMIT = 50;
    private static final int MIN_TRANSFER_MINUTES = 30;
    private static final int MAX_TRANSFER_MINUTES = 360;
    private static final int MAX_PAGE_SIZE = 100;

    private final TicketQueryMapper ticketQueryMapper;
    private final InventoryMapper inventoryMapper;
    private final TicketOrderMapper ticketOrderMapper;
    private final OrderItemMapper orderItemMapper;

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public OrderCreateResponse createOrder(Long userId, CreateOrderRequest request) {
        Map<Long, Passenger> passengers = validatePassengers(
                userId,
                request.getItems().stream().map(OrderPassengerRequest::getPassengerId).toList()
        );
        ensureClientRequestNotUsed(userId, request.getClientRequestId());

        BookingRouteRow route = loadBookableRoute(
                request.getRunId(), request.getFromStationId(), request.getToStationId()
        );
        validateNoTravelConflict(userId, passengers.keySet(), route);

        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(LOCK_MINUTES);
        TicketOrder order = insertOrder(userId, request.getClientRequestId(), expireAt);
        List<LockedTicketResponse> tickets = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderPassengerRequest item : request.getItems()) {
            LockedTicketResponse ticket = lockOneSeat(
                    order.getId(), route, item.getPassengerId(), item.getSeatTypeCode(),
                    item.getSeatPreference(), expireAt, passengers.get(item.getPassengerId()).getName()
            );
            tickets.add(ticket);
            totalAmount = totalAmount.add(ticket.getPrice());
        }

        updateOrderTotal(order, totalAmount);
        return buildCreateResponse(order, totalAmount, expireAt, tickets);
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public OrderCreateResponse createTransferOrder(Long userId, CreateTransferOrderRequest request) {
        if (!request.getFirstLeg().getToStationId()
                .equals(request.getSecondLeg().getFromStationId())) {
            throw new BusinessException(ErrorCode.INVALID_ROUTE, "两程的换乘站必须相同");
        }

        List<Long> passengerIds = request.getItems().stream()
                .map(TransferOrderPassengerRequest::getPassengerId)
                .toList();
        Map<Long, Passenger> passengers = validatePassengers(userId, passengerIds);
        ensureClientRequestNotUsed(userId, request.getClientRequestId());

        BookingRouteRow firstRoute = loadBookableRoute(
                request.getFirstLeg().getRunId(),
                request.getFirstLeg().getFromStationId(),
                request.getFirstLeg().getToStationId()
        );
        BookingRouteRow secondRoute = loadBookableRoute(
                request.getSecondLeg().getRunId(),
                request.getSecondLeg().getFromStationId(),
                request.getSecondLeg().getToStationId()
        );
        validateTransferTime(firstRoute, secondRoute);
        validateNoTravelConflict(userId, passengers.keySet(), firstRoute);
        validateNoTravelConflict(userId, passengers.keySet(), secondRoute);

        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(LOCK_MINUTES);
        TicketOrder order = insertOrder(userId, request.getClientRequestId(), expireAt);
        List<LockedTicketResponse> tickets = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (TransferOrderPassengerRequest item : request.getItems()) {
            String passengerName = passengers.get(item.getPassengerId()).getName();
            LockedTicketResponse firstTicket = lockOneSeat(
                    order.getId(), firstRoute, item.getPassengerId(), item.getFirstSeatTypeCode(),
                    item.getFirstSeatPreference(), expireAt, passengerName
            );
            LockedTicketResponse secondTicket = lockOneSeat(
                    order.getId(), secondRoute, item.getPassengerId(), item.getSecondSeatTypeCode(),
                    item.getSecondSeatPreference(), expireAt, passengerName
            );
            tickets.add(firstTicket);
            tickets.add(secondTicket);
            totalAmount = totalAmount.add(firstTicket.getPrice()).add(secondTicket.getPrice());
        }

        updateOrderTotal(order, totalAmount);
        return buildCreateResponse(order, totalAmount, expireAt, tickets);
    }

    public OrderPageResponse<OrderSummaryResponse> listOrders(
            Long userId,
            long page,
            long size,
            String status,
            LocalDate orderDateFrom,
            LocalDate orderDateTo,
            LocalDate travelDateFrom,
            LocalDate travelDateTo,
            String keyword) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "page必须大于等于1，size必须在1到100之间");
        }
        String normalizedStatus = normalizeOrderStatus(status);
        validateDateRange(orderDateFrom, orderDateTo, "下单日期");
        validateDateRange(travelDateFrom, travelDateTo, "乘车日期");
        String normalizedKeyword = keyword == null ? null : keyword.trim();

        Page<OrderSummaryResponse> pageRequest = new Page<>(page, size);
        IPage<OrderSummaryResponse> result = ticketOrderMapper.selectOrderPage(
                pageRequest, userId, normalizedStatus,
                orderDateFrom, orderDateTo, travelDateFrom, travelDateTo, normalizedKeyword
        );
        LocalDateTime now = LocalDateTime.now();
        result.getRecords().forEach(item -> item.setRemainingSeconds(
                OrderStatus.PENDING_PAYMENT.name().equals(item.getStatus())
                        ? remainingSeconds(now, item.getExpireAt()) : 0L
        ));
        return OrderPageResponse.<OrderSummaryResponse>builder()
                .page(result.getCurrent())
                .size(result.getSize())
                .total(result.getTotal())
                .pages(result.getPages())
                .records(result.getRecords())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderDetailResponse getOrderDetail(Long userId, Long orderId) {
        TicketOrder order = loadOwnedOrderForUpdate(userId, orderId);
        expireOwnedOrderIfNecessary(order);
        List<OrderItemDetailResponse> items = ticketOrderMapper.selectOrderItemDetails(orderId);
        return OrderDetailResponse.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .expireAt(order.getExpireAt())
                .remainingSeconds(OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())
                        ? remainingSeconds(LocalDateTime.now(), order.getExpireAt()) : 0L)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(items)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderCancelResponse cancelOrder(Long userId, Long orderId) {
        TicketOrder order = loadOwnedOrderForUpdate(userId, orderId);
        if (OrderStatus.CANCELLED.name().equals(order.getStatus())
                || OrderStatus.EXPIRED.name().equals(order.getStatus())) {
            return buildCancelResponse(order, 0);
        }
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }

        String targetStatus = order.getExpireAt() != null
                && !order.getExpireAt().isAfter(LocalDateTime.now())
                ? OrderStatus.EXPIRED.name()
                : OrderStatus.CANCELLED.name();
        int released = releasePendingOrder(order.getId(), targetStatus);
        order.setStatus(targetStatus);
        order.setUpdatedAt(LocalDateTime.now());
        return buildCancelResponse(order, released);
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderPaymentStatusResponse getPaymentStatus(Long userId, Long orderId) {
        TicketOrder order = loadOwnedOrderForUpdate(userId, orderId);
        expireOwnedOrderIfNecessary(order);
        OrderPaymentStatusResponse response = ticketOrderMapper.selectPaymentStatus(orderId, userId);
        if (response == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public int closeExpiredOrders() {
        return expireOrders(100);
    }

    @Transactional(rollbackFor = Exception.class)
    public int expireOrders(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        List<Long> orderIds = ticketOrderMapper.selectExpiredOrderIds(safeLimit);
        int expired = 0;
        for (Long orderId : orderIds) {
            int changed = ticketOrderMapper.updateStatusIfExpected(
                    orderId, OrderStatus.PENDING_PAYMENT.name(), OrderStatus.EXPIRED.name()
            );
            if (changed == 1) {
                inventoryMapper.releaseOrderSegments(orderId);
                ticketOrderMapper.cancelLockedItems(orderId);
                expired++;
            }
        }
        return expired;
    }

    private LockedTicketResponse lockOneSeat(
            Long orderId,
            BookingRouteRow route,
            Long passengerId,
            String requestedSeatTypeCode,
            String seatPreference,
            LocalDateTime expireAt,
            String passengerName) {
        String seatTypeCode = requestedSeatTypeCode.toUpperCase(Locale.ROOT);
        Long seatTypeId = ticketQueryMapper.selectSeatTypeIdByCode(seatTypeCode);
        if (seatTypeId == null) {
            throw new BusinessException(ErrorCode.SEAT_TYPE_NOT_FOUND);
        }
        BigDecimal price = ticketQueryMapper.selectFarePrice(
                route.getTrainId(), route.getFromSeq(), route.getToSeq(), seatTypeId
        );
        if (price == null) {
            throw new BusinessException(ErrorCode.FARE_NOT_FOUND);
        }

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(orderId);
        orderItem.setPassengerId(passengerId);
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
                normalizePreference(seatPreference), CANDIDATE_LIMIT
        );
        if (candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_TICKETS);
        }

        int requiredSegments = route.getToSeq() - route.getFromSeq();
        for (CandidateSeatRow candidate : candidates) {
            List<SegmentLockRow> segments = inventoryMapper.selectSeatSegmentsForUpdate(
                    route.getRunId(), candidate.getSeatId(), route.getFromSeq(), route.getToSeq()
            );
            if (segments.size() != requiredSegments || segments.stream().anyMatch(segment ->
                    !InventoryStatus.AVAILABLE.name().equals(segment.getStatus()))) {
                continue;
            }
            int affected = inventoryMapper.lockSeatSegments(
                    route.getRunId(), candidate.getSeatId(), route.getFromSeq(), route.getToSeq(),
                    orderItem.getId(), expireAt
            );
            if (affected != requiredSegments) {
                throw new BusinessException(ErrorCode.SEAT_LOCK_FAILED);
            }
            orderItem.setSeatId(candidate.getSeatId());
            if (orderItemMapper.updateById(orderItem) != 1) {
                throw new BusinessException(ErrorCode.ORDER_ITEM_UPDATE_FAILED);
            }
            return LockedTicketResponse.builder()
                    .orderItemId(orderItem.getId())
                    .passengerId(passengerId)
                    .passengerName(passengerName)
                    .runId(route.getRunId())
                    .trainNo(route.getTrainNo())
                    .fromStationId(route.getFromStationId())
                    .toStationId(route.getToStationId())
                    .seatTypeCode(seatTypeCode)
                    .seatTypeName(ticketQueryMapper.selectSeatTypeNameById(seatTypeId))
                    .coachNo(candidate.getCoachNo())
                    .seatNo(candidate.getSeatNo())
                    .price(price)
                    .build();
        }
        throw new BusinessException(ErrorCode.SEAT_LOCK_FAILED);
    }

    private Map<Long, Passenger> validatePassengers(Long userId, List<Long> passengerIds) {
        Set<Long> uniqueIds = new HashSet<>();
        for (Long passengerId : passengerIds) {
            if (!uniqueIds.add(passengerId)) {
                throw new BusinessException(ErrorCode.DUPLICATE_PASSENGER);
            }
        }
        List<Long> orderedIds = uniqueIds.stream().sorted().toList();
        List<Passenger> passengers = ticketOrderMapper.selectOwnedPassengersForUpdate(
                userId, orderedIds
        );
        if (passengers.size() != uniqueIds.size()) {
            throw new BusinessException(ErrorCode.PASSENGER_NOT_OWNED);
        }
        Map<Long, Passenger> result = new LinkedHashMap<>();
        passengers.forEach(passenger -> result.put(passenger.getId(), passenger));
        return result;
    }

    private BookingRouteRow loadBookableRoute(Long runId, Long fromStationId, Long toStationId) {
        if (fromStationId.equals(toStationId)) {
            throw new BusinessException(ErrorCode.INVALID_ROUTE);
        }
        BookingRouteRow route = ticketQueryMapper.selectBookingRouteForUpdate(
                runId, fromStationId, toStationId
        );
        if (route == null || route.getDepartureDateTime() == null || route.getArrivalDateTime() == null) {
            throw new BusinessException(ErrorCode.INVALID_ROUTE);
        }
        if (!"ON_SALE".equals(route.getSaleStatus())) {
            throw new BusinessException(ErrorCode.TRAIN_NOT_ON_SALE);
        }
        if (!Integer.valueOf(1).equals(route.getInventoryInitialized())) {
            throw new BusinessException(ErrorCode.INVENTORY_NOT_INITIALIZED);
        }
        if (!route.getDepartureDateTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.TRAIN_NOT_ON_SALE, "列车已经发车，不能下单");
        }
        return route;
    }

    private void validateNoTravelConflict(
            Long userId,
            Set<Long> passengerIds,
            BookingRouteRow route) {
        for (Long passengerId : passengerIds) {
            int count = ticketOrderMapper.countPassengerTravelConflict(
                    userId, passengerId, route.getDepartureDateTime(), route.getArrivalDateTime()
            );
            if (count > 0) {
                throw new BusinessException(
                        ErrorCode.PASSENGER_TRAVEL_CONFLICT,
                        "乘车人ID " + passengerId + " 已存在时间重叠的有效订单"
                );
            }
        }
    }

    private void validateTransferTime(BookingRouteRow firstRoute, BookingRouteRow secondRoute) {
        long waitMinutes = Duration.between(
                firstRoute.getArrivalDateTime(), secondRoute.getDepartureDateTime()
        ).toMinutes();
        if (waitMinutes < MIN_TRANSFER_MINUTES || waitMinutes > MAX_TRANSFER_MINUTES) {
            throw new BusinessException(
                    ErrorCode.INVALID_TRANSFER_TIME,
                    "换乘等待时间必须在" + MIN_TRANSFER_MINUTES + "到" + MAX_TRANSFER_MINUTES + "分钟之间"
            );
        }
    }

    private void ensureClientRequestNotUsed(Long userId, String clientRequestId) {
        if (ticketOrderMapper.selectByClientRequestId(userId, clientRequestId) != null) {
            throw new BusinessException(ErrorCode.DUPLICATE_CLIENT_REQUEST);
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

    private void updateOrderTotal(TicketOrder order, BigDecimal totalAmount) {
        order.setTotalAmount(totalAmount);
        if (ticketOrderMapper.updateById(order) != 1) {
            throw new BusinessException(ErrorCode.ORDER_CREATE_FAILED);
        }
    }

    private TicketOrder loadOwnedOrderForUpdate(Long userId, Long orderId) {
        TicketOrder order = ticketOrderMapper.selectByIdAndUserIdForUpdate(orderId, userId);
        if (order == null) {
            // 不区分“订单不存在”和“不是本人的订单”，避免泄露他人订单是否存在。
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return order;
    }

    private void expireOwnedOrderIfNecessary(TicketOrder order) {
        if (OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())
                && order.getExpireAt() != null
                && !order.getExpireAt().isAfter(LocalDateTime.now())) {
            releasePendingOrder(order.getId(), OrderStatus.EXPIRED.name());
            order.setStatus(OrderStatus.EXPIRED.name());
            order.setUpdatedAt(LocalDateTime.now());
        }
    }

    private int releasePendingOrder(Long orderId, String targetStatus) {
        int changed = ticketOrderMapper.updateStatusIfExpected(
                orderId, OrderStatus.PENDING_PAYMENT.name(), targetStatus
        );
        if (changed != 1) {
            throw new BusinessException(ErrorCode.ORDER_CANCEL_FAILED);
        }
        int released = inventoryMapper.releaseOrderSegments(orderId);
        ticketOrderMapper.cancelLockedItems(orderId);
        return released;
    }

    private OrderCreateResponse buildCreateResponse(
            TicketOrder order,
            BigDecimal totalAmount,
            LocalDateTime expireAt,
            List<LockedTicketResponse> tickets) {
        return OrderCreateResponse.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .status(order.getStatus())
                .totalAmount(totalAmount)
                .expireAt(expireAt)
                .remainingSeconds(remainingSeconds(LocalDateTime.now(), expireAt))
                .tickets(tickets)
                .build();
    }

    private OrderCancelResponse buildCancelResponse(TicketOrder order, int released) {
        return OrderCancelResponse.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .status(order.getStatus())
                .cancelledAt(order.getUpdatedAt() == null ? LocalDateTime.now() : order.getUpdatedAt())
                .releasedSegmentCount(released)
                .build();
    }

    private String normalizePreference(String preference) {
        if (preference == null || preference.isBlank()) {
            return "NONE";
        }
        String value = preference.toUpperCase(Locale.ROOT);
        return Set.of("WINDOW", "AISLE", "NONE").contains(value) ? value : "NONE";
    }

    private String normalizeOrderStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String value = status.trim().toUpperCase(Locale.ROOT);
        try {
            OrderStatus.valueOf(value);
            return value;
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单状态不合法");
        }
    }

    private void validateDateRange(LocalDate from, LocalDate to, String fieldName) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + "起始日期不能晚于结束日期");
        }
    }

    private long remainingSeconds(LocalDateTime now, LocalDateTime expireAt) {
        return expireAt == null ? 0L : Math.max(0L, Duration.between(now, expireAt).getSeconds());
    }

    private String generateOrderNo() {
        return "RG" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 6).toUpperCase(Locale.ROOT);
    }
}
