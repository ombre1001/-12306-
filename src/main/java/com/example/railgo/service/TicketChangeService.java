package com.example.railgo.service;

import com.example.railgo.data.dto.ChangePreviewRequest;
import com.example.railgo.data.enums.ChangeDifferenceType;
import com.example.railgo.data.enums.ChangeStatus;
import com.example.railgo.data.enums.InventoryStatus;
import com.example.railgo.data.enums.OrderItemStatus;
import com.example.railgo.data.po.ChangeFundRecord;
import com.example.railgo.data.po.OrderItem;
import com.example.railgo.data.po.TicketChange;
import com.example.railgo.data.vo.ChangeDetailResponse;
import com.example.railgo.data.vo.ChangeOptionResponse;
import com.example.railgo.data.vo.row.BookingRouteRow;
import com.example.railgo.data.vo.row.CandidateSeatRow;
import com.example.railgo.data.vo.row.ChangeTicketRow;
import com.example.railgo.data.vo.row.SegmentLockRow;
import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import com.example.railgo.mapper.ChangeFundRecordMapper;
import com.example.railgo.mapper.InventoryMapper;
import com.example.railgo.mapper.OrderItemMapper;
import com.example.railgo.mapper.TicketChangeMapper;
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
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketChangeService {

    private static final int CHANGE_LOCK_MINUTES = 15;
    private static final int CANDIDATE_LIMIT = 50;

    private final TicketChangeMapper ticketChangeMapper;
    private final TicketQueryMapper ticketQueryMapper;
    private final InventoryMapper inventoryMapper;
    private final OrderItemMapper orderItemMapper;
    private final ChangeFundRecordMapper changeFundRecordMapper;

    @Transactional(rollbackFor = Exception.class)
    public List<ChangeOptionResponse> listOptions(Long userId, Long ticketId, LocalDate travelDate) {
        if (travelDate.isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.INVALID_TRAVEL_DATE);
        }
        ChangeTicketRow oldTicket = requireChangeableTicket(userId, ticketId);
        ensureNoExistingChange(ticketId);
        return ticketChangeMapper.selectChangeOptions(oldTicket.getTicketId(), userId, travelDate)
                .stream().filter(option -> option.getAvailableCount() != null
                        && option.getAvailableCount() > 0).toList();
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public ChangeDetailResponse preview(Long userId, Long ticketId, ChangePreviewRequest request) {
        ChangeTicketRow oldTicket = requireChangeableTicket(userId, ticketId);
        ensureNoExistingChange(ticketId);
        if (ticketChangeMapper.selectByClientRequestId(userId, request.getClientRequestId()) != null) {
            throw new BusinessException(ErrorCode.CHANGE_DUPLICATE_REQUEST);
        }
        if (!oldTicket.getFromStationId().equals(request.getFromStationId())
                || !oldTicket.getToStationId().equals(request.getToStationId())) {
            throw new BusinessException(ErrorCode.INVALID_ROUTE, "改签前后乘车区间必须一致");
        }

        BookingRouteRow route = loadBookableRoute(
                request.getNewRunId(), request.getFromStationId(), request.getToStationId());
        if (oldTicket.getRunId().equals(route.getRunId())) {
            throw new BusinessException(ErrorCode.TICKET_CHANGE_NOT_ALLOWED, "新车次不能与原车次相同");
        }
        if (ticketChangeMapper.countOtherTravelConflict(
                userId, oldTicket.getPassengerId(), oldTicket.getTicketId(),
                route.getDepartureDateTime(), route.getArrivalDateTime()) > 0) {
            throw new BusinessException(ErrorCode.PASSENGER_TRAVEL_CONFLICT);
        }

        String seatTypeCode = request.getSeatTypeCode().trim().toUpperCase(Locale.ROOT);
        Long seatTypeId = ticketQueryMapper.selectSeatTypeIdByCode(seatTypeCode);
        if (seatTypeId == null) {
            throw new BusinessException(ErrorCode.SEAT_TYPE_NOT_FOUND);
        }
        BigDecimal newPrice = ticketQueryMapper.selectFarePrice(
                route.getTrainId(), route.getFromSeq(), route.getToSeq(), seatTypeId);
        if (newPrice == null) {
            throw new BusinessException(ErrorCode.FARE_NOT_FOUND);
        }

        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(CHANGE_LOCK_MINUTES);
        OrderItem newTicket = lockNewTicket(
                oldTicket, route, seatTypeId, newPrice, request.getSeatPreference(), expireAt);
        BigDecimal difference = newPrice.subtract(oldTicket.getPrice());
        ChangeDifferenceType differenceType = difference.signum() > 0
                ? ChangeDifferenceType.PAY_DIFFERENCE
                : difference.signum() < 0
                ? ChangeDifferenceType.REFUND_DIFFERENCE
                : ChangeDifferenceType.NO_DIFFERENCE;

        TicketChange change = new TicketChange();
        change.setChangeNo(generateNo("CG"));
        change.setUserId(userId);
        change.setOldOrderItemId(oldTicket.getTicketId());
        change.setNewOrderItemId(newTicket.getId());
        change.setOriginalAmount(oldTicket.getPrice());
        change.setNewAmount(newPrice);
        change.setDifferenceAmount(difference);
        change.setDifferenceType(differenceType.name());
        change.setStatus(difference.signum() > 0
                ? ChangeStatus.WAITING_PAYMENT.name()
                : ChangeStatus.WAITING_CONFIRMATION.name());
        change.setClientRequestId(request.getClientRequestId());
        change.setExpireAt(expireAt);
        try {
            if (ticketChangeMapper.insert(change) != 1) {
                throw new BusinessException(ErrorCode.CHANGE_CREATE_FAILED);
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.CHANGE_DUPLICATE_REQUEST);
        }
        return requireDetail(userId, change.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public ChangeDetailResponse confirm(Long userId, Long changeId) {
        TicketChange change = requireActiveChange(userId, changeId);
        if (!ChangeStatus.WAITING_CONFIRMATION.name().equals(change.getStatus())) {
            throw new BusinessException(ErrorCode.CHANGE_STATUS_INVALID,
                    "只有同价或低价改签可以直接确认");
        }
        completeChange(change, false);
        return requireDetail(userId, changeId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ChangeDetailResponse pay(Long userId, Long changeId) {
        TicketChange change = requireActiveChange(userId, changeId);
        if (!ChangeStatus.WAITING_PAYMENT.name().equals(change.getStatus())) {
            throw new BusinessException(ErrorCode.CHANGE_PAYMENT_NOT_REQUIRED);
        }
        completeChange(change, true);
        return requireDetail(userId, changeId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ChangeDetailResponse cancel(Long userId, Long changeId) {
        TicketChange change = ticketChangeMapper.selectOwnedChangeForUpdate(changeId, userId);
        if (change == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (ChangeStatus.CANCELLED.name().equals(change.getStatus())
                || ChangeStatus.EXPIRED.name().equals(change.getStatus())) {
            return requireDetail(userId, changeId);
        }
        if (!isPending(change.getStatus())) {
            throw new BusinessException(ErrorCode.CHANGE_STATUS_INVALID);
        }
        cancelLockedNewTicket(change, ChangeStatus.CANCELLED);
        return requireDetail(userId, changeId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ChangeDetailResponse getDetail(Long userId, Long changeId) {
        TicketChange change = ticketChangeMapper.selectOwnedChangeForUpdate(changeId, userId);
        if (change == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (isExpired(change)) {
            cancelLockedNewTicket(change, ChangeStatus.EXPIRED);
        }
        return requireDetail(userId, changeId);
    }

    @Transactional(rollbackFor = Exception.class)
    public int closeExpiredChanges() {
        int count = 0;
        for (Long changeId : ticketChangeMapper.selectExpiredChangeIds(100)) {
            TicketChange change = ticketChangeMapper.selectById(changeId);
            if (change != null && isPending(change.getStatus())) {
                int affected = ticketChangeMapper.updateStatusIfExpected(
                        changeId, change.getStatus(), ChangeStatus.EXPIRED.name());
                if (affected == 1) {
                    inventoryMapper.releaseOrderItemSegments(change.getNewOrderItemId());
                    markNewTicketCancelled(change.getNewOrderItemId());
                    count++;
                }
            }
        }
        return count;
    }

    private ChangeTicketRow requireChangeableTicket(Long userId, Long ticketId) {
        ChangeTicketRow ticket = ticketChangeMapper.selectOwnedTicketForUpdate(ticketId, userId);
        if (ticket == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!OrderItemStatus.ISSUED.name().equals(ticket.getStatus())
                || ticket.getDepartureDateTime() == null
                || !ticket.getDepartureDateTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.TICKET_CHANGE_NOT_ALLOWED);
        }
        if (ticketChangeMapper.countSuccessfulChanges(ticketId) > 0) {
            throw new BusinessException(ErrorCode.TICKET_ALREADY_CHANGED);
        }
        return ticket;
    }

    private void ensureNoExistingChange(Long ticketId) {
        if (ticketChangeMapper.countActiveChanges(ticketId) > 0) {
            throw new BusinessException(ErrorCode.CHANGE_ALREADY_PROCESSING);
        }
    }

    private TicketChange requireActiveChange(Long userId, Long changeId) {
        TicketChange change = ticketChangeMapper.selectOwnedChangeForUpdate(changeId, userId);
        if (change == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (isExpired(change)) {
            throw new BusinessException(ErrorCode.CHANGE_EXPIRED);
        }
        if (!isPending(change.getStatus())) {
            throw new BusinessException(ErrorCode.CHANGE_STATUS_INVALID);
        }
        return change;
    }

    private BookingRouteRow loadBookableRoute(Long runId, Long fromStationId, Long toStationId) {
        BookingRouteRow route = ticketQueryMapper.selectBookingRouteForUpdate(
                runId, fromStationId, toStationId);
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
            throw new BusinessException(ErrorCode.TICKET_CHANGE_NOT_ALLOWED, "新车次已经发车");
        }
        return route;
    }

    private OrderItem lockNewTicket(ChangeTicketRow oldTicket, BookingRouteRow route,
                                    Long seatTypeId, BigDecimal price, String preference,
                                    LocalDateTime expireAt) {
        OrderItem item = new OrderItem();
        item.setOrderId(oldTicket.getOrderId());
        item.setPassengerId(oldTicket.getPassengerId());
        item.setRunId(route.getRunId());
        item.setFromStationId(route.getFromStationId());
        item.setToStationId(route.getToStationId());
        item.setFromSeq(route.getFromSeq());
        item.setToSeq(route.getToSeq());
        item.setSeatTypeId(seatTypeId);
        item.setPrice(price);
        item.setStatus(OrderItemStatus.CHANGE_LOCKED.name());
        if (orderItemMapper.insert(item) != 1) {
            throw new BusinessException(ErrorCode.ORDER_ITEM_CREATE_FAILED);
        }

        List<CandidateSeatRow> candidates = inventoryMapper.selectCandidateSeats(
                route.getRunId(), seatTypeId, route.getFromSeq(), route.getToSeq(),
                normalizePreference(preference), CANDIDATE_LIMIT);
        int required = route.getToSeq() - route.getFromSeq();
        for (CandidateSeatRow candidate : candidates) {
            List<SegmentLockRow> segments = inventoryMapper.selectSeatSegmentsForUpdate(
                    route.getRunId(), candidate.getSeatId(), route.getFromSeq(), route.getToSeq());
            if (segments.size() != required || segments.stream().anyMatch(segment ->
                    !InventoryStatus.AVAILABLE.name().equals(segment.getStatus()))) {
                continue;
            }
            if (inventoryMapper.lockSeatSegments(route.getRunId(), candidate.getSeatId(),
                    route.getFromSeq(), route.getToSeq(), item.getId(), expireAt) != required) {
                throw new BusinessException(ErrorCode.SEAT_LOCK_FAILED);
            }
            item.setSeatId(candidate.getSeatId());
            if (orderItemMapper.updateById(item) != 1) {
                throw new BusinessException(ErrorCode.ORDER_ITEM_UPDATE_FAILED);
            }
            return item;
        }
        throw new BusinessException(ErrorCode.INSUFFICIENT_TICKETS);
    }

    private void completeChange(TicketChange change, boolean paidDifference) {
        ChangeTicketRow oldTicket = ticketChangeMapper.selectOwnedTicketForUpdate(
                change.getOldOrderItemId(), change.getUserId());
        if (oldTicket == null || !OrderItemStatus.ISSUED.name().equals(oldTicket.getStatus())) {
            throw new BusinessException(ErrorCode.TICKET_CHANGE_NOT_ALLOWED);
        }
        OrderItem newTicket = orderItemMapper.selectById(change.getNewOrderItemId());
        if (newTicket == null || !OrderItemStatus.CHANGE_LOCKED.name().equals(newTicket.getStatus())) {
            throw new BusinessException(ErrorCode.CHANGE_CONFIRM_FAILED);
        }
        int oldSegments = oldTicket.getToSeq() - oldTicket.getFromSeq();
        int newSegments = newTicket.getToSeq() - newTicket.getFromSeq();
        if (inventoryMapper.releaseSoldOrderItemSegments(oldTicket.getTicketId()) != oldSegments
                || inventoryMapper.sellLockedOrderItemSegments(newTicket.getId()) != newSegments) {
            throw new BusinessException(ErrorCode.CHANGE_CONFIRM_FAILED);
        }
        OrderItem oldItem = orderItemMapper.selectById(oldTicket.getTicketId());
        oldItem.setStatus(OrderItemStatus.CHANGED.name());
        newTicket.setStatus(OrderItemStatus.ISSUED.name());
        if (orderItemMapper.updateById(oldItem) != 1 || orderItemMapper.updateById(newTicket) != 1) {
            throw new BusinessException(ErrorCode.CHANGE_CONFIRM_FAILED);
        }
        change.setStatus(ChangeStatus.COMPLETED.name());
        change.setConfirmedAt(LocalDateTime.now());
        if (ticketChangeMapper.updateById(change) != 1
                || ticketChangeMapper.updateOrderAmount(oldTicket.getOrderId(),
                change.getDifferenceAmount()) != 1) {
            throw new BusinessException(ErrorCode.CHANGE_CONFIRM_FAILED);
        }
        if (paidDifference) {
            insertFundRecord(change, "PAYMENT", change.getDifferenceAmount());
        } else if (change.getDifferenceAmount().signum() < 0) {
            insertFundRecord(change, "REFUND", change.getDifferenceAmount().abs());
        }
    }

    private void insertFundRecord(TicketChange change, String type, BigDecimal amount) {
        ChangeFundRecord record = new ChangeFundRecord();
        record.setChangeId(change.getId());
        record.setFundNo(generateNo("CF"));
        record.setFundType(type);
        record.setAmount(amount);
        record.setStatus("SUCCESS");
        record.setProcessedAt(LocalDateTime.now());
        if (changeFundRecordMapper.insert(record) != 1) {
            throw new BusinessException(ErrorCode.CHANGE_CONFIRM_FAILED);
        }
    }

    private void cancelLockedNewTicket(TicketChange change, ChangeStatus target) {
        int affected = ticketChangeMapper.updateStatusIfExpected(
                change.getId(), change.getStatus(), target.name());
        if (affected != 1) {
            throw new BusinessException(ErrorCode.CHANGE_STATUS_INVALID);
        }
        inventoryMapper.releaseOrderItemSegments(change.getNewOrderItemId());
        markNewTicketCancelled(change.getNewOrderItemId());
        change.setStatus(target.name());
    }

    private void markNewTicketCancelled(Long ticketId) {
        OrderItem item = orderItemMapper.selectById(ticketId);
        if (item != null && OrderItemStatus.CHANGE_LOCKED.name().equals(item.getStatus())) {
            item.setStatus(OrderItemStatus.CANCELLED.name());
            orderItemMapper.updateById(item);
        }
    }

    private ChangeDetailResponse requireDetail(Long userId, Long changeId) {
        ChangeDetailResponse response = ticketChangeMapper.selectChangeDetail(changeId, userId);
        if (response == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        response.setRemainingSeconds(isPending(response.getStatus())
                ? Math.max(0L, Duration.between(LocalDateTime.now(), response.getExpireAt()).getSeconds())
                : 0L);
        return response;
    }

    private boolean isExpired(TicketChange change) {
        return isPending(change.getStatus()) && change.getExpireAt() != null
                && !change.getExpireAt().isAfter(LocalDateTime.now());
    }

    private boolean isPending(String status) {
        return ChangeStatus.WAITING_CONFIRMATION.name().equals(status)
                || ChangeStatus.WAITING_PAYMENT.name().equals(status);
    }

    private String normalizePreference(String preference) {
        if (preference == null || preference.isBlank()) {
            return "NONE";
        }
        String value = preference.trim().toUpperCase(Locale.ROOT);
        return Set.of("WINDOW", "AISLE", "NONE").contains(value) ? value : "NONE";
    }

    private String generateNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 6).toUpperCase(Locale.ROOT);
    }
}
