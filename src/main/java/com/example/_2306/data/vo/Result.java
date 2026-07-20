package com.example._2306.data.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * REST 接口统一响应对象。
 *
 * <p>响应示例：</p>
 * <pre>
 * {
 *   "code": 0,
 *   "message": "success",
 *   "data": {},
 *   "requestId": "01f...",
 *   "timestamp": "2026-07-20 19:30:00"
 * }
 * </pre>
 *
 * @param <T> 响应数据类型
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
public class Result<T> {

    /**
     * 业务状态码：0 表示成功；4xxxx 表示客户端或业务错误；5xxxx 表示服务端错误。
     */
    private Integer code;

    /**
     * 响应数据；错误响应通常为 null。
     */
    private T data;

    /**
     * 面向用户的提示信息，禁止直接写入 SQL、堆栈或服务器路径。
     */
    private String message;

    /**
     * 请求追踪号，用于结合后端日志定位问题。
     */
    private String requestId;

    /**
     * 响应生成时间。
     */
    private String timestamp;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 保留三参数构造方式，兼容原有 new Result(code, data, msg) 的调用。
     */
    public Result(Integer code, T data, String message) {
        this(code, data, message, generateRequestId(), now());
    }

    public Result(Integer code, T data, String message, String requestId, String timestamp) {
        this.code = code;
        this.data = data;
        this.message = message;
        this.requestId = requestId;
        this.timestamp = timestamp;
    }

    /**
     * 构造带正确 HTTP 状态码的响应。
     */
    public static <T> ResponseEntity<Result<T>> build(Result<T> result) {
        if (result == null) {
            Result<T> fallback = new Result<>(50000, null, "系统内部错误，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(fallback);
        }

        result.ensureMetadata();
        return ResponseEntity.status(result.httpStatus()).body(result);
    }

    /**
     * 返回成功结果。
     */
    public static <T> ResponseEntity<Result<T>> success(T data, String message) {
        String safeMessage = (message == null || message.isBlank()) ? "success" : message;
        return build(new Result<>(0, data, safeMessage));
    }

    /**
     * 返回成功结果，使用默认提示信息。
     */
    public static <T> ResponseEntity<Result<T>> success(T data) {
        return success(data, "success");
    }

    /**
     * 返回成功结果，无响应数据。
     */
    public static ResponseEntity<Result<Void>> ok() {
        return build(new Result<>(0, null, "success"));
    }

    /**
     * 返回错误结果。
     *
     * <p>code 可以是 HTTP 状态码（如 400、404、409），也可以是业务错误码
     * （如 40012、40902、50001）。HTTP 状态由 {@link #httpStatus()} 自动映射。</p>
     */
    public static ResponseEntity<Result<Void>> error(Integer code, String message) {
        int safeCode = code == null ? 50000 : code;
        String safeMessage = (message == null || message.isBlank())
                ? "请求处理失败"
                : message;
        return build(new Result<>(safeCode, null, safeMessage));
    }

    /**
     * 兼容原有只传错误信息的调用。
     */
    public static ResponseEntity<Result<Void>> error(String message) {
        return error(400, message);
    }

    /**
     * 根据业务状态码确定 HTTP 状态码。
     */
    public HttpStatus httpStatus() {
        if (code == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        if (code == 0 || code == 200) {
            return HttpStatus.OK;
        }

        // 允许直接使用标准 HTTP 状态码，例如 400、404、405、409、415、500。
        if (code >= 100 && code <= 599) {
            HttpStatus resolved = HttpStatus.resolve(code);
            return resolved == null ? HttpStatus.INTERNAL_SERVER_ERROR : resolved;
        }

        // 业务错误码映射，例如 40012 -> 400，40902 -> 409，50001 -> 500。
        if (code >= 40000 && code <= 40099) {
            return HttpStatus.BAD_REQUEST;
        }
        if (code >= 40100 && code <= 40199) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (code >= 40300 && code <= 40399) {
            return HttpStatus.FORBIDDEN;
        }
        if (code >= 40400 && code <= 40499) {
            return HttpStatus.NOT_FOUND;
        }
        if (code >= 40900 && code <= 40999) {
            return HttpStatus.CONFLICT;
        }
        if (code >= 50000 && code <= 59999) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        // 未识别的非成功业务码不应错误地返回 HTTP 200。
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * 使用全局异常处理器生成的 requestId 覆盖默认值，使响应体与日志一致。
     */
    public Result<T> withRequestId(String requestId) {
        if (requestId != null && !requestId.isBlank()) {
            this.requestId = requestId;
        }
        return this;
    }

    /**
     * 兼容旧代码中的 getMsg()/setMsg()，JSON 响应仍统一输出 message 字段。
     */
    @Deprecated
    @JsonIgnore
    public String getMsg() {
        return message;
    }

    @Deprecated
    @JsonIgnore
    public void setMsg(String msg) {
        this.message = msg;
    }

    private void ensureMetadata() {
        if (requestId == null || requestId.isBlank()) {
            requestId = generateRequestId();
        }
        if (timestamp == null || timestamp.isBlank()) {
            timestamp = now();
        }
    }

    private static String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String now() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }
}