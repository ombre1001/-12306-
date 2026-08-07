package com.example.railgo.service;

import com.example.railgo.config.TrainSyncProperties;
import com.example.railgo.service.source.SourceStop;
import com.example.railgo.service.source.SourceTrain;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TrainSourceClient {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                    + "AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/138.0.0.0 Safari/537.36";

    private final TrainSyncProperties properties;
    private final ObjectMapper objectMapper;

    private final CookieManager cookieManager =
            new CookieManager(null, CookiePolicy.ACCEPT_ALL);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .cookieHandler(cookieManager)
            .build();

    private long lastRequestAt;
    

    private volatile boolean sessionInitialized;

    public List<SourceTrain> queryTrains(
            LocalDate date,
            String fromCode,
            String toCode
    ) {
        String url = properties.getLeftTicketUrl()
                + "?leftTicketDTO.train_date=" + encode(date.toString())
                + "&leftTicketDTO.from_station=" + encode(fromCode)
                + "&leftTicketDTO.to_station=" + encode(toCode)
                + "&purpose_codes=ADULT";

        JsonNode root = getJson(url);
        JsonNode rows = root.path("data").path("result");
        if (!rows.isArray()) {
            throw new IllegalStateException("12306车次响应缺少data.result");
        }

        Map<String, SourceTrain> unique = new LinkedHashMap<>();
        for (JsonNode row : rows) {
            String[] fields = row.asText().split("\\|", -1);
            if (fields.length < 12 || fields[2].isBlank()
                    || fields[3].isBlank()) {
                continue;
            }

            SourceTrain train = new SourceTrain(
                    fields[2],
                    fields[3],
                    trainType(fields[3]),
                    fields[4],
                    fields[5]
            );
            unique.putIfAbsent(train.sourceTrainCode(), train);
        }
        return new ArrayList<>(unique.values());
    }

    public List<SourceStop> queryStops(
            LocalDate date,
            String sourceTrainCode
    ) {
        String url = properties.getTrainInfoUrl()
                + "?leftTicketDTO.train_no=" + encode(sourceTrainCode)
                + "&leftTicketDTO.train_date=" + encode(date.toString())
                + "&rand_code=";

        JsonNode rows = getJson(url).path("data").path("data");
        if (!rows.isArray() || rows.size() == 0) {
            throw new IllegalStateException(
                    "车次" + sourceTrainCode + "未返回经停站"
            );
        }

        List<SourceStop> result = new ArrayList<>();
        int dayOffset = 0;
        LocalTime previousTime = null;

        for (JsonNode row : rows) {
            int seq = parseInt(row.path("station_no").asText(),
                    result.size() + 1);
            String stationName = row.path("station_name").asText();
            LocalTime arrival = parseTime(row.path("arrive_time").asText());
            LocalTime departure = parseTime(row.path("start_time").asText());

            LocalTime first = arrival != null ? arrival : departure;
            if (previousTime != null && first != null
                    && first.isBefore(previousTime)) {
                dayOffset++;
            }
            int arrivalOffset = dayOffset;

            if (arrival != null && departure != null
                    && departure.isBefore(arrival)) {
                dayOffset++;
            }
            int departureOffset = dayOffset;

            LocalTime last = departure != null ? departure : arrival;
            if (last != null) {
                previousTime = last;
            }

            result.add(new SourceStop(
                    seq,
                    stationName,
                    arrival,
                    arrivalOffset,
                    departure,
                    departureOffset
            ));
        }
        return result;
    }

    private JsonNode getJson(String url) {
        RuntimeException last = null;

        for (int attempt = 1;
             attempt <= properties.getMaxRetry();
             attempt++) {

            try {
                initializeSession();
                throttle();

                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(20))
                        .header(HttpHeaders.USER_AGENT, USER_AGENT)
                        .header(
                                HttpHeaders.ACCEPT,
                                "application/json, text/javascript, */*; q=0.01"
                        )
                        .header(
                                HttpHeaders.ACCEPT_LANGUAGE,
                                "zh-CN,zh;q=0.9"
                        )
                        .header(
                                HttpHeaders.REFERER,
                                "https://kyfw.12306.cn/otn/leftTicket/init"
                        )
                        .header("X-Requested-With", "XMLHttpRequest")
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString(
                                StandardCharsets.UTF_8
                        )
                );

                if (response.statusCode() / 100 != 2) {
                    throw new IOException(
                            "HTTP " + response.statusCode()
                                    + "，URI=" + response.uri()
                    );
                }

                JsonNode root = parseJsonResponse(response);

                if (!root.path("status").asBoolean(false)) {
                    throw new IOException(
                            "来源返回status=false，message="
                                    + root.path("messages")
                    );
                }

                return root;
            } catch (Exception exception) {
                resetSession();

                last = new IllegalStateException(
                        "请求12306失败，第" + attempt + "次："
                                + exception.getMessage(),
                        exception
                );
            }
        }

        throw last == null
                ? new IllegalStateException("请求12306失败")
                : last;
    }

    private synchronized void throttle() throws InterruptedException {
        long interval = properties.getRequestInterval().toMillis();
        long wait = interval - (System.currentTimeMillis() - lastRequestAt);
        if (wait > 0) {
            Thread.sleep(wait);
        }
        lastRequestAt = System.currentTimeMillis();
    }

    private String trainType(String trainNo) {
        if (trainNo == null || trainNo.isBlank()) {
            return "OTHER";
        }
        return switch (Character.toUpperCase(trainNo.charAt(0))) {
            case 'G' -> "G";
            case 'D' -> "D";
            case 'C' -> "C";
            case 'Z' -> "Z";
            case 'T' -> "T";
            case 'K' -> "K";
            default -> "OTHER";
        };
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank() || "----".equals(value)) {
            return null;
        }
        return LocalTime.parse(value);
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 解析第三方接口返回的 JSON。
     * 12306 部分节点返回的正文开头可能包含 UTF-8 BOM（\uFEFF），
     * Jackson 从 String 解析时不会自动忽略该字符。
     */
    private JsonNode readJson(String responseBody) throws JsonProcessingException {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("12306接口返回空响应");
        }

        String normalized = responseBody;

        // 先删除普通空白，再删除可能存在的 BOM。
        normalized = normalized.stripLeading();

        while (!normalized.isEmpty()
                && normalized.charAt(0) == '\uFEFF') {
            normalized = normalized.substring(1).stripLeading();
        }

        if (normalized.isBlank()) {
            throw new IllegalStateException("12306接口响应去除BOM后为空");
        }

        return objectMapper.readTree(normalized);
    }

    /**
     * 先访问余票查询首页，让12306设置会话Cookie。
     */
    private synchronized void initializeSession()
            throws IOException, InterruptedException {

        if (sessionInitialized) {
            return;
        }

        throttle();

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(
                                "https://kyfw.12306.cn/otn/leftTicket/init"
                        )
                )
                .timeout(Duration.ofSeconds(20))
                .header(HttpHeaders.USER_AGENT, USER_AGENT)
                .header(
                        HttpHeaders.ACCEPT,
                        "text/html,application/xhtml+xml,"
                                + "application/xml;q=0.9,*/*;q=0.8"
                )
                .header(
                        HttpHeaders.ACCEPT_LANGUAGE,
                        "zh-CN,zh;q=0.9"
                )
                .GET()
                .build();

        HttpResponse<Void> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.discarding()
        );

        if (response.statusCode() / 100 != 2) {
            throw new IOException(
                    "初始化12306会话失败，HTTP "
                            + response.statusCode()
                            + "，URI=" + response.uri()
            );
        }

        sessionInitialized = true;
    }

    /**
     * 解析JSON，并兼容UTF-8 BOM。
     * 如果返回HTML，输出状态码、最终URI、Content-Type和正文摘要。
     */
    private JsonNode parseJsonResponse(
            HttpResponse<String> response
    ) throws IOException {

        String body = response.body();

        if (body == null || body.isBlank()) {
            throw new IOException(
                    "12306返回空响应，HTTP="
                            + response.statusCode()
                            + "，URI=" + response.uri()
            );
        }

        String normalized = body;

        while (!normalized.isEmpty()) {
            char first = normalized.charAt(0);

            if (first == '\uFEFF' || Character.isWhitespace(first)) {
                normalized = normalized.substring(1);
            } else {
                break;
            }
        }

        String contentType = response.headers()
                .firstValue(HttpHeaders.CONTENT_TYPE)
                .orElse("unknown");

        if (normalized.startsWith("<")) {
            String preview = normalized
                    .replaceAll("\\s+", " ");

            if (preview.length() > 300) {
                preview = preview.substring(0, 300);
            }

            throw new IOException(
                    "12306返回HTML而不是JSON"
                            + "，HTTP=" + response.statusCode()
                            + "，URI=" + response.uri()
                            + "，Content-Type=" + contentType
                            + "，响应开头=" + preview
            );
        }

        if (!normalized.startsWith("{")
                && !normalized.startsWith("[")) {

            String preview = normalized.length() > 300
                    ? normalized.substring(0, 300)
                    : normalized;

            String content = "";
            throw new IOException(
                    "12306返回内容不是JSON"
                            + "，HTTP=" + response.statusCode()
                            + "，URI=" + response.uri()
                            + "，Content-Type=" + content + contentType
                            + "，响应开头=" + preview
            );
        }

        return objectMapper.readTree(normalized);
    }

    private synchronized void resetSession() {
        sessionInitialized = false;
        cookieManager.getCookieStore().removeAll();
    }
}