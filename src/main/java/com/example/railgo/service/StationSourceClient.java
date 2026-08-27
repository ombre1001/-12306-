package com.example.railgo.service;

import com.example.railgo.config.TrainSyncProperties;
import com.example.railgo.service.source.SourceStation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StationSourceClient {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                    + "AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/138.0.0.0 Safari/537.36";

    private final TrainSyncProperties properties;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private volatile StationDirectory cachedDirectory;
    private volatile Instant cacheExpiresAt = Instant.EPOCH;
    private volatile Instant retryAfter = Instant.EPOCH;
    private volatile RuntimeException lastFailure;

    public StationDirectory getDirectory() {
        StationDirectory current = cachedDirectory;
        if (current != null && Instant.now().isBefore(cacheExpiresAt)) {
            return current;
        }
        if (current == null && Instant.now().isBefore(retryAfter)
                && lastFailure != null) {
            throw lastFailure;
        }
        return refreshDirectory();
    }

    public synchronized StationDirectory refreshDirectory() {
        if (cachedDirectory != null && Instant.now().isBefore(cacheExpiresAt)) {
            return cachedDirectory;
        }

        RuntimeException last = null;
        for (int attempt = 1; attempt <= properties.getMaxRetry(); attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(
                                URI.create(properties.getStationNameUrl()))
                        .timeout(Duration.ofSeconds(20))
                        .header(HttpHeaders.USER_AGENT, USER_AGENT)
                        .header(HttpHeaders.ACCEPT, "*/*")
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                );
                if (response.statusCode() / 100 != 2) {
                    throw new IOException("HTTP " + response.statusCode());
                }

                StationDirectory directory = parse(response.body());
                if (directory.size() < 1000) {
                    throw new IOException(
                            "车站主数据数量异常：" + directory.size()
                    );
                }
                cachedDirectory = directory;
                cacheExpiresAt = Instant.now().plus(
                        properties.getStationDirectoryTtl()
                );
                retryAfter = Instant.EPOCH;
                lastFailure = null;
                return directory;
            } catch (Exception exception) {
                last = new IllegalStateException(
                        "获取12306车站主数据失败，第" + attempt + "次："
                                + exception.getMessage(),
                        exception
                );
            }
        }

        // 短暂网络故障时允许使用已经过期的快照，避免整批车次失败。
        if (cachedDirectory != null) {
            cacheExpiresAt = Instant.now().plus(Duration.ofMinutes(10));
            return cachedDirectory;
        }
        RuntimeException failure = last == null
                ? new IllegalStateException("获取12306车站主数据失败")
                : last;
        lastFailure = failure;
        retryAfter = Instant.now().plus(Duration.ofMinutes(10));
        throw failure;
    }

    static StationDirectory parse(String javascript) {
        if (javascript == null || javascript.isBlank()) {
            throw new IllegalArgumentException("station_name.js为空");
        }

        Map<String, SourceStation> byCode = new LinkedHashMap<>();
        Map<String, SourceStation> byName = new LinkedHashMap<>();
        for (String item : javascript.replace("\uFEFF", "").split("@")) {
            String[] fields = item.split("\\|", -1);
            // 格式：简拼|站名|电报码|全拼|拼音首字母|序号
            if (fields.length < 5) {
                continue;
            }
            String name = normalizeName(fields[1]);
            String code = normalizeCode(fields[2]);
            if (name.isBlank() || code.length() != 3) {
                continue;
            }
            SourceStation station = new SourceStation(
                    code,
                    name,
                    fields[3].trim().toLowerCase(Locale.ROOT),
                    fields[4].trim().toLowerCase(Locale.ROOT)
            );
            byCode.put(code, station);
            byName.put(name, station);
        }
        return new StationDirectory(Map.copyOf(byCode), Map.copyOf(byName));
    }

    static String normalizeName(String value) {
        String name = value == null ? "" : value.trim()
                .replace("（", "(")
                .replace("）", ")")
                .replaceAll("\\s+", "");
        for (String suffix : new String[]{
                "(高铁)", "(客运)", "(城际)", "(动车)"
        }) {
            if (name.endsWith(suffix)) {
                name = name.substring(0, name.length() - suffix.length());
                break;
            }
        }
        return name.endsWith("站")
                ? name.substring(0, name.length() - 1)
                : name;
    }

    static String normalizeCode(String value) {
        return value == null
                ? ""
                : value.trim().toUpperCase(Locale.ROOT);
    }

    public record StationDirectory(
            Map<String, SourceStation> byCode,
            Map<String, SourceStation> byName
    ) {
        public SourceStation findByCode(String code) {
            return byCode.get(normalizeCode(code));
        }

        public SourceStation findByName(String name) {
            return byName.get(normalizeName(name));
        }

        public int size() {
            return byCode.size();
        }
    }
}
