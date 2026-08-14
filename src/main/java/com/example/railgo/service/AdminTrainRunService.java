package com.example.railgo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.railgo.data.dto.AdminInventoryBatchInitRequest;
import com.example.railgo.data.dto.AdminInventoryBatchInitResult;
import com.example.railgo.data.dto.AdminRunBatchRequest;
import com.example.railgo.data.po.Train;
import com.example.railgo.data.po.TrainRun;
import com.example.railgo.data.vo.admin.AdminTrainRunResponse;
import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import com.example.railgo.mapper.TrainMapper;
import com.example.railgo.mapper.TrainRunMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
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

    public IPage<AdminTrainRunResponse> page(
            long page,
            long size,
            Long trainId,
            LocalDate startDate,
            LocalDate endDate,
            String saleStatus
    ) {
        String normalizedSaleStatus =
                saleStatus == null || saleStatus.isBlank()
                        ? null
                        : saleStatus.trim().toUpperCase();

        return trainRunMapper.selectAdminRunPage(
                new Page<>(page, size),
                trainId,
                startDate,
                endDate,
                normalizedSaleStatus
        );
    }

    public AdminInventoryBatchInitResult batchInitializeInventory(
            AdminInventoryBatchInitRequest request
    ) {
        validateBatchInventoryDateRange(
                request.startDate(),
                request.endDate()
        );

        List<TrainRun> runs = trainRunMapper.selectList(
                Wrappers.<TrainRun>lambdaQuery()
                        .eq(
                                request.trainId() != null,
                                TrainRun::getTrainId,
                                request.trainId()
                        )
                        .ge(
                                TrainRun::getRunDate,
                                request.startDate()
                        )
                        .le(
                                TrainRun::getRunDate,
                                request.endDate()
                        )
                        .orderByAsc(TrainRun::getRunDate)
                        .orderByAsc(TrainRun::getTrainId)
                        .orderByAsc(TrainRun::getId)
        );

        int initializedCount = 0;
        int skippedCount = 0;
        List<AdminInventoryBatchInitResult.FailureItem> failures =
                new ArrayList<>();

        for (TrainRun run : runs) {
            if (Boolean.TRUE.equals(run.getInventoryInitialized())) {
                skippedCount++;
                continue;
            }

            if (!"DRAFT".equals(run.getSaleStatus())
                    && !"NOT_ON_SALE".equals(run.getSaleStatus())) {
                skippedCount++;
                continue;
            }

            try {
                /*
                 * InventoryService 是独立 Spring Bean；每次调用都会进入其
                 * @Transactional 方法，因此单个运行失败不会回滚其他运行。
                 */
                inventoryService.initializeInventory(run.getId());
                initializedCount++;
            } catch (BusinessException exception) {
                failures.add(toBatchFailure(run, exception.getMessage()));
            } catch (RuntimeException exception) {
                log.error(
                        "批量初始化库存失败: runId={}, trainId={}, runDate={}",
                        run.getId(),
                        run.getTrainId(),
                        run.getRunDate(),
                        exception
                );
                failures.add(toBatchFailure(run, "库存初始化失败，请查看后端日志"));
            }
        }

        return new AdminInventoryBatchInitResult(
                runs.size(),
                initializedCount,
                skippedCount,
                failures.size(),
                List.copyOf(failures)
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

    private void validateBatchInventoryDateRange(
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(
                    ErrorCode.RUN_DATE_RANGE_INVALID,
                    "开始日期不能晚于结束日期"
            );
        }

        if (ChronoUnit.DAYS.between(startDate, endDate) > 366) {
            throw new BusinessException(
                    ErrorCode.RUN_DATE_RANGE_INVALID,
                    "一次最多初始化367天的运行库存"
            );
        }
    }

    private AdminInventoryBatchInitResult.FailureItem toBatchFailure(
            TrainRun run,
            String reason
    ) {
        Train train = trainMapper.selectById(run.getTrainId());

        return new AdminInventoryBatchInitResult.FailureItem(
                run.getId(),
                run.getTrainId(),
                train == null ? null : train.getTrainNo(),
                run.getRunDate(),
                reason
        );
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