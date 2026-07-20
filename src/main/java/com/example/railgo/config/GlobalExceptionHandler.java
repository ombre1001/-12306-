package com.example.railgo.config;

import com.example.railgo.data.vo.Result;
import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.CustomException;
import com.example.railgo.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.TransactionException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.sql.SQLDataException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * <p>职责：</p>
 * <ul>
 *     <li>统一接口错误响应，避免将堆栈和数据库细节暴露给前端；</li>
 *     <li>对参数、权限、业务、数据库和系统异常进行分类；</li>
 *     <li>为每个异常响应附加 X-Request-Id，便于前后端联合排查；</li>
 *     <li>保证火车票查询、下单、支付、退票、改签和后台维护等模块的错误信息清晰。</li>
 * </ul>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@SuppressWarnings("rawtypes")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    /* ========================= 参数校验异常 ========================= */

    /**
     * 处理 @RequestBody + @Valid 校验失败。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String message = buildBindingMessage(ex);
        log.warn("[requestId={}] 请求体参数校验失败: {}", requestId(request), message);
        return error(400, message, request);
    }

    /**
     * 处理表单对象、查询对象绑定失败。
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result> handleBindException(
            BindException ex,
            HttpServletRequest request) {

        String message = buildBindingMessage(ex);
        log.warn("[requestId={}] 参数绑定失败: {}", requestId(request), message);
        return error(400, message, request);
    }

    /**
     * 处理 @RequestParam、@PathVariable 等参数约束校验失败。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result> handleConstraintViolationException(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .filter(item -> item != null && !item.isBlank())
                .distinct()
                .limit(5)
                .collect(Collectors.joining("；"));

        if (message.isBlank()) {
            message = "参数校验失败";
        }

        log.warn("[requestId={}] 请求参数约束校验失败: {}", requestId(request), message);
        return error(400, message, request);
    }

    /**
     * 必填查询参数缺失。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        String message = "缺少必填参数：" + ex.getParameterName();
        log.warn("[requestId={}] {}", requestId(request), message);
        return error(400, message, request);
    }

    /**
     * 参数类型转换失败，例如 runId、stationId、日期格式不正确。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String requiredType = ex.getRequiredType() == null
                ? "正确的数据类型"
                : ex.getRequiredType().getSimpleName();
        String message = "参数“" + ex.getName() + "”格式错误，应为 " + requiredType;

        log.warn("[requestId={}] 参数类型不匹配: 参数={}, 目标类型={}, 原始值={}",
                requestId(request), ex.getName(), requiredType, ex.getValue());
        return error(400, message, request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(
            BusinessException exception) {

        ErrorCode errorCode = exception.getErrorCode();

        Result<Void> result = new Result<>(
                errorCode.getCode(),
                null,
                exception.getMessage()
        );

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(result);
    }

    /**
     * JSON 缺失、JSON 语法错误、日期/枚举反序列化失败。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        log.warn("[requestId={}] 请求体无法解析: {}", requestId(request), ex.getMessage());
        return error(400, "请求体格式错误，请检查 JSON、日期和枚举值", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        String message = safeMessage(ex, "请求参数不合法");
        log.warn("[requestId={}] 非法参数: {}", requestId(request), message);
        return error(400, message, request);
    }

    /* ========================= 认证与权限异常 ========================= */

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {

        log.warn("[requestId={}] 身份认证失败: {}", requestId(request), ex.getMessage());
        return error(401, "登录状态已失效或身份认证失败，请重新登录", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {

        log.warn("[requestId={}] 权限不足: {}", requestId(request), ex.getMessage());
        return error(403, "无权执行当前操作", request);
    }

    /* ========================= 业务与资源异常 ========================= */

    /**
     * 业务异常应由 Service 层主动抛出，例如：
     * 余票不足、订单超时、订单状态不允许支付、车票不允许退改签等。
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Result> handleCustomException(
            CustomException ex,
            HttpServletRequest request) {

        String message = safeMessage(ex, "业务处理失败");
        log.warn("[requestId={}] 业务异常: {}", requestId(request), message);
        return error(400, message, request);
    }

    /**
     * 业务状态冲突，例如重复支付、重复退款、重复提交、非法状态流转。
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Result> handleIllegalStateException(
            IllegalStateException ex,
            HttpServletRequest request) {

        String message = safeMessage(ex, "当前业务状态不允许执行该操作");
        log.warn("[requestId={}] 业务状态冲突: {}", requestId(request), message);
        return error(409, message, request);
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<Result> handleEmptyResultDataAccessException(
            EmptyResultDataAccessException ex,
            HttpServletRequest request) {

        log.warn("[requestId={}] 未找到目标数据: {}", requestId(request), ex.getMessage());
        return error(404, "未找到相关数据，或数据已被删除", request);
    }

    /* ========================= 数据库与事务异常 ========================= */

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result> handleDuplicateKeyException(
            DuplicateKeyException ex,
            HttpServletRequest request) {

        log.warn("[requestId={}] 唯一约束冲突: {}", requestId(request), rootMessage(ex));
        return error(409, "数据已存在，请勿重复提交", request);
    }

    /**
     * MyBatis/JPA 通常会把外键、唯一键、非空约束等异常包装为该异常。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        String message = integrityMessage(ex);
        log.warn("[requestId={}] 数据完整性约束异常: {}", requestId(request), rootMessage(ex));
        return error(message.startsWith("数据已存在") ? 409 : 400, message, request);
    }

    /**
     * 处理未被 Spring DAO 层包装的 JDBC 完整性异常。
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public ResponseEntity<Result> handleSqlIntegrityConstraintViolationException(
            SQLIntegrityConstraintViolationException ex,
            HttpServletRequest request) {

        String message = integrityMessage(ex);
        log.warn("[requestId={}] SQL 完整性约束异常: {}", requestId(request), rootMessage(ex));
        return error(message.startsWith("数据已存在") ? 409 : 400, message, request);
    }

    @ExceptionHandler(SQLDataException.class)
    public ResponseEntity<Result> handleSqlDataException(
            SQLDataException ex,
            HttpServletRequest request) {

        log.warn("[requestId={}] SQL 数据格式异常: {}", requestId(request), ex.getMessage());
        return error(400, "数据格式或长度不符合要求，请检查输入", request);
    }

    @ExceptionHandler(TransactionException.class)
    public ResponseEntity<Result> handleTransactionException(
            TransactionException ex,
            HttpServletRequest request) {

        log.error("[requestId={}] 数据库事务执行失败", requestId(request), ex);
        return error(500, "业务事务执行失败，操作已回滚，请稍后重试", request);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Result> handleDataAccessException(
            DataAccessException ex,
            HttpServletRequest request) {

        Throwable root = rootCause(ex);

        if (root instanceof SQLIntegrityConstraintViolationException) {
            String message = integrityMessage(root);
            log.warn("[requestId={}] 数据访问完整性异常: {}", requestId(request), rootMessage(ex));
            return error(message.startsWith("数据已存在") ? 409 : 400, message, request);
        }

        if (root instanceof SQLDataException) {
            log.warn("[requestId={}] 数据访问格式异常: {}", requestId(request), rootMessage(ex));
            return error(400, "数据格式或长度不符合要求，请检查输入", request);
        }

        log.error("[requestId={}] 数据库访问异常", requestId(request), ex);
        return error(500, "数据库操作失败，请稍后重试", request);
    }

    /* ========================= HTTP、文件与系统异常 ========================= */

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        log.warn("[requestId={}] 请求方法不支持: method={}, uri={}",
                requestId(request), request.getMethod(), request.getRequestURI());
        return error(405, "当前接口不支持该请求方法", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {

        log.warn("[requestId={}] 请求媒体类型不支持: {}", requestId(request), ex.getContentType());
        return error(415, "不支持当前请求数据格式，请使用 application/json", request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request) {

        log.warn("[requestId={}] 上传文件超过大小限制", requestId(request));
        return error(400, "上传文件超过系统允许的大小", request);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Result> handleIOException(
            IOException ex,
            HttpServletRequest request) {

        log.error("[requestId={}] 文件或网络 IO 异常", requestId(request), ex);
        return error(500, "文件读取、导入或数据同步失败，请稍后重试", request);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Result> handleNullPointerException(
            NullPointerException ex,
            HttpServletRequest request) {

        log.error("[requestId={}] 空指针异常", requestId(request), ex);
        return error(500, "系统内部错误，请稍后重试", request);
    }

    /**
     * 最后兜底：不得向前端直接返回 ex.getMessage()，防止泄露 SQL、路径、密钥等内部信息。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result> handleException(
            Exception ex,
            HttpServletRequest request) {

        log.error("[requestId={}] 未处理的系统异常: method={}, uri={}",
                requestId(request), request.getMethod(), request.getRequestURI(), ex);
        return error(500, "系统内部错误，请稍后重试", request);
    }

    /* ========================= 工具方法 ========================= */

    private String buildBindingMessage(BindException ex) {
        Set<String> messages = new LinkedHashSet<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            if (messages.size() >= 5) {
                break;
            }
            String errorMessage = fieldError.getDefaultMessage();
            if (errorMessage == null || errorMessage.isBlank()) {
                errorMessage = "格式不正确";
            }
            messages.add(fieldError.getField() + "：" + errorMessage);
        }

        ex.getBindingResult().getGlobalErrors().stream()
                .map(item -> item.getDefaultMessage() == null ? "参数校验失败" : item.getDefaultMessage())
                .filter(item -> !item.isBlank())
                .limit(Math.max(0, 5 - messages.size()))
                .forEach(messages::add);

        return messages.isEmpty() ? "参数校验失败" : String.join("；", messages);
    }

    private String integrityMessage(Throwable ex) {
        String message = rootMessage(ex).toLowerCase(Locale.ROOT);

        if (containsAny(message, "duplicate", "unique constraint", "唯一约束", "重复")) {
            return "数据已存在，请勿重复提交";
        }
        if (containsAny(message, "foreign key", "constraint fails", "外键")) {
            return "关联数据不存在，或当前数据仍被订单、车票等业务记录引用";
        }
        if (containsAny(message, "cannot be null", "not null", "非空")) {
            return "必填数据缺失，请检查输入";
        }
        if (containsAny(message, "data too long", "out of range", "too large")) {
            return "数据长度或数值范围超出限制，请检查输入";
        }

        return "数据完整性校验失败，请检查输入或当前业务状态";
    }

    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable result = throwable;
        Set<Throwable> visited = new LinkedHashSet<>();

        while (result.getCause() != null && visited.add(result)) {
            result = result.getCause();
        }
        return result;
    }

    private String rootMessage(Throwable throwable) {
        Throwable root = rootCause(throwable);
        return root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
    }

    private String safeMessage(Throwable throwable, String defaultMessage) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? defaultMessage : message;
    }

    private String requestId(HttpServletRequest request) {
        Object cached = request.getAttribute(REQUEST_ID_HEADER);
        if (cached instanceof String value && !value.isBlank()) {
            return value;
        }

        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank() || requestId.length() > 64) {
            requestId = UUID.randomUUID().toString().replace("-", "");
        }

        request.setAttribute(REQUEST_ID_HEADER, requestId);
        return requestId;
    }

    /**
     * 复用现有 Result.error(...)，同时将请求追踪号返回给前端。
     */
    private ResponseEntity<Result> error(int status, String message, HttpServletRequest request) {
        ResponseEntity<Result<Void>> original = Result.error(status, message);

        HttpHeaders headers = new HttpHeaders();
        headers.putAll(original.getHeaders());
        headers.set(REQUEST_ID_HEADER, requestId(request));

        return new ResponseEntity<>(original.getBody(), headers, original.getStatusCode());
    }
}
