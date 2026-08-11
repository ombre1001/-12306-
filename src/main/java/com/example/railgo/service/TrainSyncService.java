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
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainSyncService {

    private static final String SOURCE = "12306_PUBLIC_QUERY";
    private static final String SYNC_LOCK_NAME = "railgo_train_sync";

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
        Map<String, List<SourceStop>> stopCache = new HashMap<>();
        try {
            for (LocalDate date = start;
                 !date.isAfter(end);
                 date = date.plusDays(1)) {
                DayResult dayResult = syncDate(date, stopCache);
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

    private DayResult syncDate(
            LocalDate date,
            Map<String, List<SourceStop>> stopCache
    ) {
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
        Set<String> allowedTypes = normalizedTrainTypes();

        for (String route : properties.getRoutes()) {
            if (discovered.size() >= properties.getMaxTrainsPerDay()) {
                break;
            }

            try {
                String[] codes = parseRoute(route);
                addRouteTrains(
                        discovered,
                        sourceClient.queryTrains(date, codes[0], codes[1]),
                        allowedTypes
                );
            } catch (Exception exception) {
                errors.add(route + "：" + rootMessage(exception));
            }
        }

        if (discovered.isEmpty()) {
            errors.add("未从热门区间中匹配到可同步的演示车次");
        } else {
            log.info(
                    "日期{}选中{}趟演示车次：{}",
                    date,
                    discovered.size(),
                    discovered.values().stream()
                            .map(SourceTrain::trainNo)
                            .toList()
            );
        }

        int mergedTrains = 0;
        int mergedStops = 0;
        for (SourceTrain train : discovered.values()) {
            try {
                List<SourceStop> stops = stopCache.get(
                        train.sourceTrainCode()
                );
                if (stops == null) {
                    stops = List.copyOf(sourceClient.queryStops(
                            date, train.sourceTrainCode()
                    ));
                    stopCache.put(train.sourceTrainCode(), stops);
                }
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

    /**
     * 每个热门区间只选择少量车次，并使用全局上限限制一天的同步规模。
     * LinkedHashMap 同时完成去重：同一趟车可能被多个热门区间查询到，
     * 但经停站只会抓取和合并一次。
     */
    private void addRouteTrains(
            Map<String, SourceTrain> discovered,
            List<SourceTrain> routeTrains,
            Set<String> allowedTypes
    ) {
        List<SourceTrain> candidates = new ArrayList<>();
        for (SourceTrain train : routeTrains) {
            String type = normalize(train.trainType());
            if (allowedTypes.contains(type)
                    && !discovered.containsKey(train.sourceTrainCode())) {
                candidates.add(train);
            }
        }

        int remaining = properties.getMaxTrainsPerDay()
                - discovered.size();
        int selectedCount = Math.min(
                Math.min(properties.getTrainsPerRoute(), remaining),
                candidates.size()
        );

        /*
         * 12306余票查询结果通常按出发时间排列。这里从整天结果中
         * 均匀抽样，而不是只拿最早几趟，使同区间车次的时刻具有间隔，
         * 更容易演示改签和30～240分钟的一次换乘。
         */
        for (int index = 0; index < selectedCount; index++) {
            int candidateIndex = (int) Math.floor(
                    (index + 0.5D) * candidates.size() / selectedCount
            );
            SourceTrain selected = candidates.get(
                    Math.min(candidateIndex, candidates.size() - 1)
            );
            discovered.put(selected.sourceTrainCode(), selected);
        }
    }

    private Set<String> normalizedTrainTypes() {
        Set<String> result = new HashSet<>();
        if (properties.getTrainTypes() == null) {
            return result;
        }
        for (String type : properties.getTrainTypes()) {
            String normalized = normalize(type);
            if (!normalized.isBlank()) {
                result.add(normalized);
            }
        }
        return result;
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toUpperCase(Locale.ROOT);
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
        if (properties.getRoutes() == null
                || properties.getRoutes().isEmpty()) {
            throw new IllegalStateException("未配置app.train-sync.routes");
        }
        if (normalizedTrainTypes().isEmpty()) {
            throw new IllegalStateException("未配置app.train-sync.train-types");
        }
        if (properties.getTrainsPerRoute() < 1
                || properties.getMaxTrainsPerDay() < 1
                || properties.getTrainsPerRoute()
                > properties.getMaxTrainsPerDay()) {
            throw new IllegalStateException(
                    "车次同步数量配置不合法：trains-per-route必须大于0，"
                            + "且不能超过max-trains-per-day"
            );
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

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT GET_LOCK(?, 0)"
            )) {
                statement.setString(1, SYNC_LOCK_NAME);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        discardConnection(connection);
                        connection = null;
                        throw new IllegalStateException("获取车次同步数据库锁没有返回结果");
                    }

                    Object value = resultSet.getObject(1);

                    if (value instanceof Number number
                            && number.intValue() == 1) {
                        log.info(
                                "已获得车次同步数据库锁，connectionId={}",
                                queryConnectionId(connection)
                        );
                        return connection;
                    }

                    if (value instanceof Number number
                            && number.intValue() == 0) {
                        closeQuietly(connection);
                        connection = null;
                        throw new IllegalStateException("已有车次同步任务正在执行");
                    }

                    discardConnection(connection);
                    connection = null;
                    throw new IllegalStateException("获取车次同步数据库锁返回NULL");
                }
            }
        } catch (IllegalStateException exception) {
            if (connection != null) {
                discardConnection(connection);
            }
            throw exception;
        } catch (Exception exception) {
            if (connection != null) {
                discardConnection(connection);
            }
            throw new IllegalStateException(
                    "获取车次同步数据库锁失败",
                    exception
            );
        }
    }

    private void releaseLock(Connection connection) {
        if (connection == null) {
            return;
        }

        boolean released = false;

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT RELEASE_LOCK(?)"
        )) {
            statement.setString(1, SYNC_LOCK_NAME);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Object value = resultSet.getObject(1);
                    released = value instanceof Number number
                            && number.intValue() == 1;
                }
            }

            if (released) {
                log.info("车次同步数据库锁已正常释放");
            } else {
                log.error(
                        "RELEASE_LOCK未成功，准备销毁持锁物理连接"
                );
            }
        } catch (Exception exception) {
            log.error(
                    "释放车次同步数据库锁异常，准备销毁持锁物理连接",
                    exception
            );
        } finally {
            if (released) {
                closeQuietly(connection);
            } else {
                /*
                 * 不能只调用connection.close()。
                 * 对Hikari代理连接而言，close可能只是归还连接池，
                 * 物理连接继续存在，命名锁也会继续存在。
                 */
                discardConnection(connection);
            }
        }
    }

    private long queryConnectionId(Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT CONNECTION_ID()"
        );
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
        } catch (Exception exception) {
            log.warn("查询持锁连接ID失败", exception);
        }

        return -1L;
    }

    private void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (Exception exception) {
            log.warn("关闭数据库连接失败", exception);
        }
    }

    private void discardConnection(Connection connection) {
        if (connection == null) {
            return;
        }

        try {
            if (dataSource instanceof HikariDataSource hikariDataSource) {
                /*
                 * 将连接标记为驱逐。
                 * 连接归还连接池时会关闭底层MySQL物理连接，
                 * MySQL随后自动释放该连接持有的命名锁。
                 */
                hikariDataSource.evictConnection(connection);
            }
        } catch (Exception exception) {
            log.error("驱逐持锁数据库连接失败", exception);
        } finally {
            closeQuietly(connection);
        }
    }

    private record DayResult(
            boolean success,
            int trainCount,
            int stopCount
    ) {
    }
}