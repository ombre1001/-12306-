package com.example.railgo.scheduler;

import com.example.railgo.service.TicketChangeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChangeTimeoutScheduler {

    private final TicketChangeService ticketChangeService;

    @Scheduled(fixedDelayString = "${app.change.timeout-scan-delay-ms:60000}",
            initialDelayString = "${app.change.timeout-scan-initial-delay-ms:15000}")
    public void expireChanges() {
        try {
            int affected = ticketChangeService.closeExpiredChanges();
            if (affected > 0) {
                log.info("超时改签处理完成，共释放 {} 个新座位", affected);
            }
        } catch (Exception exception) {
            log.error("超时改签处理失败", exception);
        }
    }
}
