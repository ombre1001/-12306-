package com.example.railgo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.railgo.data.dto.AdminRunBatchRequest;
import com.example.railgo.data.po.Train;
import com.example.railgo.data.po.TrainRun;
import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import com.example.railgo.mapper.TrainMapper;
import com.example.railgo.mapper.TrainRunMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AdminTrainRunService {

    private final TrainMapper trainMapper;
    private final TrainRunMapper trainRunMapper;
    private final InventoryService inventoryService;

    @Transactional
    public int batchCreate(
            AdminRunBatchRequest request
    ) {
        Train train = trainMapper.selectById(
                request.trainId()
        );

        if (train == null) {
            throw new BusinessException(
                    ErrorCode.TRAIN_NOT_FOUND
            );
        }

        if (!"ACTIVE".equals(train.getStatus())) {
            throw new BusinessException(
                    ErrorCode.TRAIN_STATUS_INVALID,
                    "只有启用状态的车次才能生成运行计划"
            );
        }

        if (request.startDate().isAfter(
                request.endDate()
        )) {
            throw new BusinessException(
                    ErrorCode.RUN_DATE_RANGE_INVALID
            );
        }

        long days = ChronoUnit.DAYS.between(
                request.startDate(),
                request.endDate()
        );

        if (days > 366) {
            throw new BusinessException(
                    ErrorCode.RUN_DATE_RANGE_INVALID,
                    "一次最多生成367天的运行计划"
            );
        }

        int created = 0;

        for (LocalDate date = request.startDate();
             !date.isAfter(request.endDate());
             date = date.plusDays(1)) {

            TrainRun existing =
                    trainRunMapper.selectOne(
                            Wrappers.<TrainRun>lambdaQuery()
                                    .eq(
                                            TrainRun::getTrainId,
                                            request.trainId()
                                    )
                                    .eq(
                                            TrainRun::getRunDate,
                                            date
                                    )
                    );

            if (existing != null) {
                continue;
            }

            LocalDateTime now = LocalDateTime.now();

            TrainRun run = new TrainRun();
            run.setTrainId(request.trainId());
            run.setRunDate(date);
            run.setSaleStatus("NOT_ON_SALE");
            run.setInventoryInitialized(false);
            run.setCreatedAt(now);
            run.setUpdatedAt(now);

            trainRunMapper.insert(run);
            created++;

            if (Boolean.TRUE.equals(
                    request.initializeInventory()
            )) {
                inventoryService.initializeInventory(
                        run.getId()
                );
            }
        }

        return created;
    }

    public IPage<TrainRun> page(
            long page,
            long size,
            Long trainId,
            LocalDate startDate,
            LocalDate endDate,
            String saleStatus
    ) {
        return trainRunMapper.selectPage(
                new Page<>(page, size),
                Wrappers.<TrainRun>lambdaQuery()
                        .eq(
                                trainId != null,
                                TrainRun::getTrainId,
                                trainId
                        )
                        .ge(
                                startDate != null,
                                TrainRun::getRunDate,
                                startDate
                        )
                        .le(
                                endDate != null,
                                TrainRun::getRunDate,
                                endDate
                        )
                        .eq(
                                saleStatus != null
                                        && !saleStatus.isBlank(),
                                TrainRun::getSaleStatus,
                                saleStatus
                        )
                        .orderByDesc(TrainRun::getRunDate)
                        .orderByAsc(TrainRun::getTrainId)
        );
    }

    @Transactional
    public void updateSaleStatus(
            Long runId,
            String targetStatus
    ) {
        TrainRun run = requireRun(runId);

        String current = run.getSaleStatus();

        if (!canTransition(current, targetStatus)) {
            throw new BusinessException(
                    ErrorCode.RUN_SALE_STATUS_INVALID,
                    "不能从"
                            + current
                            + "切换为"
                            + targetStatus
            );
        }

        if ("ON_SALE".equals(targetStatus)
                && !Boolean.TRUE.equals(
                run.getInventoryInitialized()
        )) {
            throw new BusinessException(
                    ErrorCode.INVENTORY_NOT_INITIALIZED
            );
        }

        run.setSaleStatus(targetStatus);
        run.setUpdatedAt(LocalDateTime.now());

        trainRunMapper.updateById(run);
    }

    @Transactional
    public void delete(Long runId) {
        TrainRun run = requireRun(runId);

        if (trainRunMapper.countEffectiveOrderItems(runId)
                > 0) {
            throw new BusinessException(
                    ErrorCode.RUN_ALREADY_HAS_ORDER
            );
        }

        if ("ON_SALE".equals(run.getSaleStatus())) {
            throw new BusinessException(
                    ErrorCode.RUN_SALE_STATUS_INVALID,
                    "开售中的运行实例不能删除"
            );
        }

        trainRunMapper.deleteById(runId);
    }

    private TrainRun requireRun(Long runId) {
        TrainRun run = trainRunMapper.selectById(runId);

        if (run == null) {
            throw new BusinessException(
                    ErrorCode.TRAIN_RUN_NOT_FOUND
            );
        }

        return run;
    }

    private boolean canTransition(
            String current,
            String target
    ) {
        if (current.equals(target)) {
            return true;
        }

        return switch (current) {
            case "DRAFT", "NOT_ON_SALE" ->
                    target.equals("ON_SALE")
                            || target.equals("CANCELLED");
            case "ON_SALE" ->
                    target.equals("OFF_SALE")
                            || target.equals("CANCELLED");
            case "OFF_SALE" ->
                    target.equals("ON_SALE")
                            || target.equals("CANCELLED");
            case "CANCELLED" -> false;
            default -> false;
        };
    }
}