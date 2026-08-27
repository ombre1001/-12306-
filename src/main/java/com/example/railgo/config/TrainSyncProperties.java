package com.example.railgo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "app.train-sync")
public class TrainSyncProperties {

    private boolean enabled = false;
    private int futureDays = 14;
    private int nearDays = 1;
    private int maxRetry = 3;
    private Duration requestInterval = Duration.ofSeconds(2);
    private Duration freshness = Duration.ofHours(36);
    private String leftTicketUrl =
            "https://kyfw.12306.cn/otn/leftTicket/query";
    private String trainInfoUrl =
            "https://kyfw.12306.cn/otn/queryTrainInfo/query";
    private String stationNameUrl =
            "https://kyfw.12306.cn/otn/resources/js/framework/station_name.js";
    private Duration stationDirectoryTtl = Duration.ofHours(12);

    /** 每个热门区间最多选取的车次数。 */
    private int trainsPerRoute = 3;

    /** 每个运行日期最多同步的车次数，防止热门区间返回几百趟车。 */
    private int maxTrainsPerDay = 36;

    /** 演示环境只同步高铁、动车和城际列车。 */
    private List<String> trainTypes = new ArrayList<>(
            List.of("G", "D", "C")
    );

    /** 格式：起点三字码-终点三字码，例如 JNK-BJP。 */
    private List<String> routes = new ArrayList<>();
}
