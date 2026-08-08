package com.example.railgo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.railgo.data.po.SysOperationLog;
import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import com.example.railgo.mapper.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OperationLogService {
    private final SysOperationLogMapper logMapper;

    public IPage<SysOperationLog> page(long page, long size, Long operatorId,
                                        String module, String action, String result,
                                        String requestId, LocalDate startDate, LocalDate endDate) {
        var query = Wrappers.<SysOperationLog>lambdaQuery()
                .orderByDesc(SysOperationLog::getOperatedAt);
        if (operatorId != null) query.eq(SysOperationLog::getOperatorId, operatorId);
        if (StringUtils.hasText(module)) query.eq(SysOperationLog::getModule, module.trim().toUpperCase());
        if (StringUtils.hasText(action)) query.like(SysOperationLog::getAction, action.trim());
        if (StringUtils.hasText(result)) query.eq(SysOperationLog::getResult, result.trim().toUpperCase());
        if (StringUtils.hasText(requestId)) query.eq(SysOperationLog::getRequestId, requestId.trim());
        if (startDate != null) query.ge(SysOperationLog::getOperatedAt, startDate.atStartOfDay());
        if (endDate != null) query.lt(SysOperationLog::getOperatedAt, endDate.plusDays(1).atStartOfDay());
        return logMapper.selectPage(new Page<>(page, size), query);
    }

    public SysOperationLog detail(Long id) {
        SysOperationLog log = logMapper.selectById(id);
        if (log == null) throw new BusinessException(ErrorCode.AUDIT_LOG_NOT_FOUND);
        return log;
    }

    public void save(SysOperationLog log) {
        if (log.getOperatedAt() == null) log.setOperatedAt(LocalDateTime.now());
        logMapper.insert(log);
    }
}
