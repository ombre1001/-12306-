package com.example.railgo.service;

import com.example.railgo.data.vo.InventorySummaryResponse;
import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import com.example.railgo.mapper.InventoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InventoryMapper inventoryMapper;

    @Transactional(rollbackFor = Exception.class)
    public void initializeInventory(Long runId) {
        String status = inventoryMapper.selectRunSaleStatusForUpdate(runId);
        if (status == null) {
            throw new BusinessException(ErrorCode.TRAIN_RUN_NOT_FOUND);
        }
        if (!"NOT_ON_SALE".equals(status) && !"DRAFT".equals(status)) {
            throw new BusinessException(ErrorCode.INVENTORY_INIT_STATUS_INVALID);
        }

        int stopCount = inventoryMapper.countTrainStops(runId);
        int seatCount = inventoryMapper.countTrainSeats(runId);
        if (stopCount < 2) {
            throw new BusinessException(ErrorCode.TRAIN_STOP_NOT_ENOUGH);
        }
        if (seatCount <= 0) {
            throw new BusinessException(ErrorCode.TRAIN_SEAT_NOT_FOUND);
        }

        inventoryMapper.insertMissingSegmentInventory(runId);
        long expectedCount = (long) seatCount * (stopCount - 1);
        if (inventoryMapper.countInventoryRows(runId) != expectedCount) {
            throw new BusinessException(ErrorCode.INVENTORY_INITIALIZATION_FAILED);
        }
        if (inventoryMapper.markInventoryInitialized(runId) != 1) {
            throw new BusinessException(ErrorCode.INVENTORY_INITIALIZATION_FAILED);
        }
    }

    public List<InventorySummaryResponse> getInventorySummary(Long runId) {
        return inventoryMapper.selectInventorySummary(runId);
    }
}