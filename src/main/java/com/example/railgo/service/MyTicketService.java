package com.example.railgo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.railgo.data.vo.MyTicketDetailResponse;
import com.example.railgo.data.vo.MyTicketResponse;
import com.example.railgo.data.vo.OrderPageResponse;
import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import com.example.railgo.mapper.MyTicketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MyTicketService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> QUERYABLE_STATUSES = Set.of("ISSUED", "REFUNDED", "CHANGED");
    private final MyTicketMapper myTicketMapper;

    @Transactional(readOnly = true)
    public OrderPageResponse<MyTicketResponse> list(Long userId, long page, long size, String status) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "page必须大于等于1，size必须在1到100之间");
        }
        String normalizedStatus = normalizeStatus(status);
        IPage<MyTicketResponse> result = myTicketMapper.selectMyTicketPage(
                new Page<>(page, size), userId, normalizedStatus);
        return OrderPageResponse.<MyTicketResponse>builder()
                .page(result.getCurrent())
                .size(result.getSize())
                .total(result.getTotal())
                .pages(result.getPages())
                .records(result.getRecords())
                .build();
    }

    @Transactional(readOnly = true)
    public MyTicketDetailResponse detail(Long userId, Long ticketId) {
        MyTicketDetailResponse response = myTicketMapper.selectOwnedTicketDetail(ticketId, userId);
        if (response == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "车票不存在或不属于当前用户");
        }
        return response;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!QUERYABLE_STATUSES.contains(normalized)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "车票状态仅支持 ISSUED、REFUNDED、CHANGED");
        }
        return normalized;
    }
}
