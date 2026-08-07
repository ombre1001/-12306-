package com.example.railgo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.railgo.config.TrainSyncProperties;
import com.example.railgo.data.po.TrainSyncLog;
import com.example.railgo.data.vo.TrainSyncSummary;
import com.example.railgo.mapper.TrainSyncLogMapper;
import com.example.railgo.service.source.SourceStop;
import com.example.railgo.service.source.SourceTrain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainSyncService {

    private static final String SOURCE = "12306_PUBLIC_QUERY";

    private final TrainSyncProperties properties;
    private final TrainSourceClient sourceClient;
    private final TrainSyncMergeService mergeService;
    private final TrainSyncLogMapper logMapper;
    private final DataSource dataSource;

    public TrainSyncSummary syncRange(LocalDate start, LocalDate end) {
        validate(start, end);
        Connection lockConnection = acquireLock();

        int successDays = 0;
        int failedDays = 0;
        int trainCount = 0;
        int stopCount = 0;
        try {
            for (LocalDate date = start;
                 !date.isAfter(end);
                 date = date.plusDays(1)) {
                DayResult dayResult = syncDate(date);
                if (dayResult.success()) {
                    successDays++;
                } else {
                    failedDays++;
                }
                trainCount += dayResult.trainCount();
                stopCount += dayResult.stopCount();
            }
        } finally {
            releaseLock(lockConnection);
        }
        return new TrainSyncSummary(
                start, end, successDays, failedDays,
                trainCount, stopCount
        );
    }

    public IPage<TrainSyncLog> pageLogs(long page, long size) {
        return logMapper.selectPage(
                new Page<>(page, size),
                Wrappers.<TrainSyncLog>lambdaQuery()
                        .orderByDesc(TrainSyncLog::getStartedAt)
        );
    }

    private DayResult syncDate(LocalDate date) {
        TrainSyncLog syncLog = new TrainSyncLog();
        syncLog.setBatchId(UUID.randomUUID().toString());
        syncLog.setSource(SOURCE);
        syncLog.setSourceDate(date);
        syncLog.setStartedAt(LocalDateTime.now());
        syncLog.setResult("RUNNING");
        syncLog.setTrainCount(0);
        syncLog.setStopCount(0);
        logMapper.insert(syncLog);

        Map<String, SourceTrain> discovered = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();

        for (String route : properties.getRoutes()) {
            try {
                String[] codes = parseRoute(route);
                for (SourceTrain train : sourceClient.queryTrains(
                        date, codes[0], codes[1])) {
                    discovered.putIfAbsent(train.sourceTrainCode(), train);
                }
            } catch (Exception exception) {
                errors.add(route + "：" + rootMessage(exception));
            }
        }

        int mergedTrains = 0;
        int mergedStops = 0;
        for (SourceTrain train : discovered.values()) {
            try {
                List<SourceStop> stops = sourceClient.queryStops(
                        date, train.sourceTrainCode()
                );
                TrainSyncMergeService.MergeResult result =
                        mergeService.merge(date, train, stops);
                mergedTrains++;
                mergedStops += result.stopCount();
            } catch (Exception exception) {
                errors.add(train.trainNo() + "：" + rootMessage(exception));
            }
        }

        syncLog.setFinishedAt(LocalDateTime.now());
        syncLog.setTrainCount(mergedTrains);
        syncLog.setStopCount(mergedStops);
        syncLog.setErrorText(errors.isEmpty()
                ? null
                : String.join(System.lineSeparator(), errors));
        syncLog.setResult(errors.isEmpty()
                ? "SUCCESS"
                : mergedTrains > 0 ? "PARTIAL" : "FAILED");
        logMapper.updateById(syncLog);

        return new DayResult(
                errors.isEmpty(), mergedTrains, mergedStops
        );
    }

    private String[] parseRoute(String route) {
        String[] codes = route == null
                ? new String[0]
                : route.trim().toUpperCase().split("-");
        if (codes.length != 2
                || codes[0].isBlank()
                || codes[1].isBlank()) {
            throw new IllegalArgumentException(
                    "线路格式必须为FROM-TO：" + route
            );
        }
        return codes;
    }

    private void validate(LocalDate start, LocalDate end) {
        if (properties.getRoutes().isEmpty()) {
            throw new IllegalStateException("未配置app.train-sync.routes");
        }
        if (start == null || end == null || start.isAfter(end)
                || end.isAfter(start.plusDays(30))) {
            throw new IllegalArgumentException("同步日期范围不合法");
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null ? current.getClass().getSimpleName() : message;
    }

    private Connection acquireLock() {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(
                         "SELECT GET_LOCK('railgo_train_sync', 0)")) {
                if (resultSet.next() && resultSet.getInt(1) == 1) {
                    return connection;
                }
            }
            connection.close();
            throw new IllegalStateException("已有车次同步任务正在执行");
        } catch (Exception exception) {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception closeException) {
                    exception.addSuppressed(closeException);
                }
            }
            if (exception instanceof IllegalStateException stateException) {
                throw stateException;
            }
            throw new IllegalStateException("获取车次同步数据库锁失败", exception);
        }
    }

    private void releaseLock(Connection connection) {
        if (connection == null) {
            return;
        }
        try (connection;
             Statement statement = connection.createStatement()) {
            statement.executeQuery(
                    "SELECT RELEASE_LOCK('railgo_train_sync')"
            ).close();
        } catch (Exception exception) {
            log.warn("释放车次同步数据库锁失败", exception);
        }
    }

    private record DayResult(
            boolean success,
            int trainCount,
            int stopCount
    ) {
    }
}
