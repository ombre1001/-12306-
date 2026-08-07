package com.example.railgo.scheduler;

import com.example.railgo.config.TrainSyncProperties;
import com.example.railgo.service.TrainSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.train-sync",
        name = "enabled",
        havingValue = "true"
)
public class TrainSyncScheduler {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final TrainSyncService trainSyncService;
    private final TrainSyncProperties properties;

    @Scheduled(
            cron = "${app.train-sync.rolling-cron:0 30 5 * * *}",
            zone = "Asia/Shanghai"
    )
    public void syncRollingWindow() {
        LocalDate today = LocalDate.now(ZONE);
        execute(today, today.plusDays(properties.getFutureDays()));
    }

    @Scheduled(
            cron = "${app.train-sync.near-cron:0 30 12,18 * * *}",
            zone = "Asia/Shanghai"
    )
    public void recheckNearDates() {
        LocalDate today = LocalDate.now(ZONE);
        execute(today, today.plusDays(properties.getNearDays()));
    }

    private void execute(LocalDate start, LocalDate end) {
        try {
            trainSyncService.syncRange(start, end);
        } catch (Exception exception) {
            log.error("车次同步失败，范围{}至{}", start, end, exception);
        }
    }
}