package com.example.railgo.scheduler;

import com.example.railgo.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduler {

    private final OrderService orderService;

    @Scheduled(
            fixedDelayString =
                    "${app.order.timeout-scan-delay-ms:60000}",
            initialDelayString =
                    "${app.order.timeout-scan-initial-delay-ms:10000}"
    )
    public void expireOrders() {
        try {
            int affected = orderService.closeExpiredOrders();

            if (affected > 0) {
                log.info("超时订单处理完成，共取消 {} 个订单", affected);
            }
        } catch (Exception exception) {
            log.error("超时订单处理失败", exception);
        }
    }
}
