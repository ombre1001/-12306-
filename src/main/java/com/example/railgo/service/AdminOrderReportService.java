package com.example.railgo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.railgo.data.po.*;
import com.example.railgo.data.vo.OrderItemDetailResponse;
import com.example.railgo.data.vo.admin.*;
import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import com.example.railgo.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class AdminOrderReportService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_REPORT_DAYS = 366;

    private final AdminOrderReportMapper reportMapper;
    private final TicketOrderMapper ticketOrderMapper;
    private final SysUserMapper sysUserMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final TicketReturnMapper ticketReturnMapper;
    private final RefundRecordMapper refundRecordMapper;
    private final TicketChangeMapper ticketChangeMapper;
    private final ChangeFundRecordMapper changeFundRecordMapper;

    public IPage<AdminOrderSummaryResponse> pageOrders(
            long page, long size, String orderNo, Long userId, String status,
            String trainNo, String keyword, LocalDate startDate, LocalDate endDate) {
        checkPage(page, size);
        checkOptionalRange(startDate, endDate);
        return reportMapper.selectOrderPage(new Page<>(page, size), trim(orderNo), userId,
                upper(status), trim(trainNo), trim(keyword), startDate, endDate);
    }

    @Transactional(readOnly = true)
    public AdminOrderDetailResponse getOrderDetail(Long orderId) {
        TicketOrder order = ticketOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        SysUser user = sysUserMapper.selectById(order.getUserId());
        List<OrderItemDetailResponse> items = ticketOrderMapper.selectOrderItemDetails(orderId);

        List<PaymentRecord> payments = paymentRecordMapper.selectList(
                Wrappers.<PaymentRecord>lambdaQuery().eq(PaymentRecord::getOrderId, orderId)
                        .orderByDesc(PaymentRecord::getCreatedAt));
        List<TicketReturn> returns = ticketReturnMapper.selectList(
                Wrappers.<TicketReturn>lambdaQuery().eq(TicketReturn::getOrderId, orderId)
                        .orderByDesc(TicketReturn::getCreatedAt));
        List<RefundRecord> refunds = returns.isEmpty() ? List.of() : refundRecordMapper.selectList(
                Wrappers.<RefundRecord>lambdaQuery()
                        .in(RefundRecord::getReturnId, returns.stream().map(TicketReturn::getId).toList())
                        .orderByDesc(RefundRecord::getCreatedAt));

        List<Long> itemIds = items.stream().map(OrderItemDetailResponse::getOrderItemId).toList();
        List<TicketChange> changes = itemIds.isEmpty() ? List.of() : ticketChangeMapper.selectList(
                Wrappers.<TicketChange>lambdaQuery()
                        .in(TicketChange::getOldOrderItemId, itemIds)
                        .or().in(TicketChange::getNewOrderItemId, itemIds)
                        .orderByDesc(TicketChange::getCreatedAt));
        List<ChangeFundRecord> funds = changes.isEmpty() ? List.of() : changeFundRecordMapper.selectList(
                Wrappers.<ChangeFundRecord>lambdaQuery()
                        .in(ChangeFundRecord::getChangeId, changes.stream().map(TicketChange::getId).toList())
                        .orderByDesc(ChangeFundRecord::getCreatedAt));

        return AdminOrderDetailResponse.builder()
                .orderId(order.getId()).orderNo(order.getOrderNo()).userId(order.getUserId())
                .userPhone(user == null ? null : user.getPhone())
                .userNickname(user == null ? null : user.getNickname())
                .status(order.getStatus()).totalAmount(order.getTotalAmount())
                .expireAt(order.getExpireAt()).createdAt(order.getCreatedAt()).updatedAt(order.getUpdatedAt())
                .items(items).inventoryOccupations(reportMapper.selectInventoryOccupations(orderId))
                .payments(payments).returns(returns).refunds(refunds).changes(changes).changeFunds(funds)
                .build();
    }

    public IPage<AdminPaymentResponse> pagePayments(
            long page, long size, String paymentNo, String orderNo, String status,
            String channel, LocalDate startDate, LocalDate endDate) {
        checkPage(page, size);
        checkOptionalRange(startDate, endDate);
        return reportMapper.selectPaymentPage(new Page<>(page, size), trim(paymentNo), trim(orderNo),
                upper(status), upper(channel), startDate, endDate);
    }

    public IPage<AdminRefundResponse> pageRefunds(
            long page, long size, String refundNo, String orderNo, String status,
            LocalDate startDate, LocalDate endDate) {
        checkPage(page, size);
        checkOptionalRange(startDate, endDate);
        return reportMapper.selectRefundPage(new Page<>(page, size), trim(refundNo), trim(orderNo),
                upper(status), startDate, endDate);
    }

    public IPage<AdminChangeResponse> pageChanges(
            long page, long size, String changeNo, String orderNo, String status,
            String trainNo, LocalDate startDate, LocalDate endDate) {
        checkPage(page, size);
        checkOptionalRange(startDate, endDate);
        return reportMapper.selectChangePage(new Page<>(page, size), trim(changeNo), trim(orderNo),
                upper(status), trim(trainNo), startDate, endDate);
    }

    public AdminSalesSummaryResponse salesSummary(LocalDate startDate, LocalDate endDate) {
        DateRange range = reportRange(startDate, endDate);
        AdminSalesSummaryResponse result = reportMapper.selectSalesSummary(range.startDate(), range.endDate());
        result.setNetRevenue(money(result.getGrossSales())
                .add(money(result.getChangePaymentAmount()))
                .subtract(money(result.getRefundAmount()))
                .subtract(money(result.getChangeRefundAmount())));
        return result;
    }

    public List<AdminSalesTrendResponse> salesTrend(
            LocalDate startDate, LocalDate endDate, String granularity) {
        DateRange range = reportRange(startDate, endDate);
        return reportMapper.selectSalesTrend(range.startDate(), range.endDate(), normalizeGranularity(granularity));
    }

    public List<AdminPopularRouteResponse> popularRoutes(
            LocalDate startDate, LocalDate endDate, String trainNo,
            Long fromStationId, Long toStationId, Integer limit) {
        DateRange range = reportRange(startDate, endDate);
        int safeLimit = limit == null ? 10 : limit;
        if (safeLimit < 1 || safeLimit > 100) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "limit必须在1到100之间");
        }
        return reportMapper.selectPopularRoutes(range.startDate(), range.endDate(), trim(trainNo),
                fromStationId, toStationId, safeLimit);
    }

    public List<AdminLoadFactorResponse> loadFactor(
            LocalDate startDate, LocalDate endDate, String trainNo, String seatTypeCode) {
        DateRange range = reportRange(startDate, endDate);
        return reportMapper.selectLoadFactor(range.startDate(), range.endDate(),
                trim(trainNo), upper(seatTypeCode));
    }

    public AdminUserStatisticsResponse userStatistics(LocalDate startDate, LocalDate endDate) {
        DateRange range = reportRange(startDate, endDate);
        return reportMapper.selectUserStatistics(range.startDate(), range.endDate());
    }

    public byte[] exportCsv(String reportType, LocalDate startDate, LocalDate endDate,
                            String granularity, String trainNo, String seatTypeCode) {
        String type = reportType == null ? "SALES_TREND" : reportType.trim().toUpperCase(Locale.ROOT);
        StringBuilder csv = new StringBuilder("\uFEFF");
        switch (type) {
            case "SALES_TREND" -> {
                csv.append("周期,支付订单数,出票数,销售额,退款额,改签补款,改签退款,净收入\r\n");
                appendRows(csv, salesTrend(startDate, endDate, granularity), row -> Arrays.asList(
                        row.getPeriod(), row.getPaidOrderCount(), row.getTicketCount(), row.getGrossSales(),
                        row.getRefundAmount(), row.getChangePaymentAmount(), row.getChangeRefundAmount(),
                        row.getNetRevenue()));
            }
            case "POPULAR_ROUTES" -> {
                csv.append("出发站ID,出发站,到达站ID,到达站,票数,乘车人数,销售额\r\n");
                appendRows(csv, popularRoutes(startDate, endDate, trainNo, null, null, 100), row -> Arrays.asList(
                        row.getFromStationId(), row.getFromStationName(), row.getToStationId(),
                        row.getToStationName(), row.getTicketCount(), row.getPassengerCount(), row.getSalesAmount()));
            }
            case "LOAD_FACTOR" -> {
                csv.append("运行ID,日期,车次,席别编码,席别,总区间数,售出区间数,锁定区间数,上座率(%)\r\n");
                appendRows(csv, loadFactor(startDate, endDate, trainNo, seatTypeCode), row -> Arrays.asList(
                        row.getRunId(), row.getRunDate(), row.getTrainNo(), row.getSeatTypeCode(),
                        row.getSeatTypeName(), row.getTotalSegmentCount(), row.getSoldSegmentCount(),
                        row.getLockedSegmentCount(), row.getLoadFactor()));
            }
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "reportType仅支持SALES_TREND、POPULAR_ROUTES、LOAD_FACTOR");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private <T> void appendRows(StringBuilder csv, List<T> rows, Function<T, List<Object>> mapper) {
        for (T row : rows) {
            List<Object> values = mapper.apply(row);
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) csv.append(',');
                csv.append(csvCell(values.get(i)));
            }
            csv.append("\r\n");
        }
    }

    private String csvCell(Object value) {
        if (value == null) return "";
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return '"' + text.replace("\"", "\"\"") + '"';
        }
        return text;
    }

    private void checkPage(long page, long size) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "page必须大于等于1，size必须在1到100之间");
        }
    }

    private void checkOptionalRange(LocalDate startDate, LocalDate endDate) {
        if ((startDate == null) != (endDate == null)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "startDate和endDate必须同时传入");
        }
        if (startDate != null) validateRange(startDate, endDate);
    }

    private DateRange reportRange(LocalDate startDate, LocalDate endDate) {
        LocalDate safeEnd = endDate == null ? LocalDate.now() : endDate;
        LocalDate safeStart = startDate == null ? safeEnd.minusDays(29) : startDate;
        validateRange(safeStart, safeEnd);
        return new DateRange(safeStart, safeEnd);
    }

    private void validateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "startDate不能晚于endDate");
        }
        if (startDate.plusDays(MAX_REPORT_DAYS - 1L).isBefore(endDate)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "单次查询范围不能超过366天");
        }
    }

    private String normalizeGranularity(String value) {
        String result = value == null ? "DAY" : value.trim().toUpperCase(Locale.ROOT);
        if (!List.of("DAY", "WEEK", "MONTH").contains(result)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "granularity仅支持DAY、WEEK、MONTH");
        }
        return result;
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String upper(String value) {
        String result = trim(value);
        return result == null ? null : result.toUpperCase(Locale.ROOT);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {}
}
