package com.example.railgo.config;

import com.example.railgo.data.po.SysOperationLog;
import com.example.railgo.security.RailUserPrincipal;
import com.example.railgo.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAuditInterceptor implements HandlerInterceptor {
    private static final String START_ATTRIBUTE = AdminAuditInterceptor.class.getName() + ".start";
    private static final String REQUEST_ID_ATTRIBUTE = "X-Request-Id";
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "newpassword", "oldpassword", "token", "refreshtoken", "authorization");

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (shouldAudit(request, handler)) {
            request.setAttribute(START_ATTRIBUTE, System.nanoTime());
            if (request.getAttribute(REQUEST_ID_ATTRIBUTE) == null) {
                String supplied = request.getHeader(REQUEST_ID_ATTRIBUTE);
                request.setAttribute(REQUEST_ID_ATTRIBUTE,
                        supplied == null || supplied.isBlank() ? UUID.randomUUID().toString().replace("-", "") : supplied);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception exception) {
        Object startValue = request.getAttribute(START_ATTRIBUTE);
        if (!(startValue instanceof Long start) || !(handler instanceof HandlerMethod handlerMethod)) return;

        try {
            SysOperationLog operation = new SysOperationLog();
            operation.setRequestId(String.valueOf(request.getAttribute(REQUEST_ID_ATTRIBUTE)));
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof RailUserPrincipal principal) {
                operation.setOperatorId(principal.userId());
                operation.setOperatorPhone(principal.phone());
            }
            operation.setModule(resolveModule(request.getRequestURI()));
            operation.setAction(handlerMethod.getBeanType().getSimpleName() + "." + handlerMethod.getMethod().getName());
            operation.setRequestMethod(request.getMethod());
            operation.setRequestUri(limit(request.getRequestURI(), 500));
            operation.setRequestParams(serializeParameters(request));
            operation.setClientIp(limit(resolveClientIp(request), 64));
            operation.setResponseStatus(response.getStatus());
            operation.setResult(response.getStatus() < 400 && exception == null ? "SUCCESS" : "FAILED");
            operation.setErrorMessage(exception == null ? null : limit(exception.getMessage(), 1000));
            operation.setDurationMs((System.nanoTime() - start) / 1_000_000);
            operation.setOperatedAt(LocalDateTime.now());
            operationLogService.save(operation);
        } catch (Exception logException) {
            // 审计日志失败不能覆盖原业务响应，但必须留下服务日志以便修复表结构或连接问题。
            log.error("后台操作日志写入失败: method={}, uri={}",
                    request.getMethod(), request.getRequestURI(), logException);
        }
    }

    private boolean shouldAudit(HttpServletRequest request, Object handler) {
        return handler instanceof HandlerMethod && WRITE_METHODS.contains(request.getMethod());
    }

    private String resolveModule(String uri) {
        String[] parts = uri.split("/");
        for (int index = 0; index < parts.length; index++) {
            if (("admin".equals(parts[index]) || "system".equals(parts[index])) && index + 1 < parts.length) {
                return limit(parts[index + 1].replace('-', '_').toUpperCase(Locale.ROOT), 50);
            }
        }
        return "SYSTEM";
    }

    private String serializeParameters(HttpServletRequest request)  {
        Map<String, Object> safe = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (SENSITIVE_KEYS.contains(key.toLowerCase(Locale.ROOT))) safe.put(key, "******");
            else safe.put(key, values.length == 1 ? values[0] : Arrays.asList(values));
        });
        return limit(objectMapper.writeValueAsString(safe), 4000);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        String realIp = request.getHeader("X-Real-IP");
        return realIp == null || realIp.isBlank() ? request.getRemoteAddr() : realIp.trim();
    }

    private String limit(String value, int length) {
        if (value == null || value.length() <= length) return value;
        return value.substring(0, length);
    }
}
