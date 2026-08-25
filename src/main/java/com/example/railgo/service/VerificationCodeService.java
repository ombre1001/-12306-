package com.example.railgo.service;

import com.example.railgo.config.SmsProperties;
import com.example.railgo.config.SmsPurpose;
import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;

@Service
@ConditionalOnProperty(prefix = "app.sms", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class VerificationCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final DefaultRedisScript<Long> VERIFY_SCRIPT =
            new DefaultRedisScript<>("""
                    local stored = redis.call('GET', KEYS[1])
                    if not stored then return 0 end
                    if stored == ARGV[1] then
                        redis.call('DEL', KEYS[1], KEYS[2])
                        return 1
                    end
                    local attempts = redis.call('INCR', KEYS[2])
                    redis.call('EXPIRE', KEYS[2], ARGV[3])
                    if attempts >= tonumber(ARGV[2]) then
                        redis.call('DEL', KEYS[1])
                    end
                    return -1
                    """, Long.class);

    private static final DefaultRedisScript<Long> LIMIT_SCRIPT =
            new DefaultRedisScript<>("""
                    local count = redis.call('INCR', KEYS[1])
                    if count == 1 then
                        redis.call('EXPIRE', KEYS[1], ARGV[1])
                    end
                    if count > tonumber(ARGV[2]) then return 0 end
                    return 1
                    """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final SmsSender smsSender;
    private final SmsProperties properties;

    public void send(String phone, SmsPurpose purpose, String clientIp) {
        String cooldownKey = "sms:cooldown:" + purpose + ":" + phone;
        Boolean reserved = redisTemplate.opsForValue()
                .setIfAbsent(cooldownKey, "1", properties.getSendCooldown());

        if (!Boolean.TRUE.equals(reserved)) {
            throw new BusinessException(ErrorCode.SMS_TOO_FREQUENT);
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            enforceCounter(
                    "sms:daily:" + now.toLocalDate() + ":" + phone,
                    properties.getPhoneDailyLimit(),
                    Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay())
                            .plusSeconds(1)
            );
            enforceCounter(
                    "sms:ip-hour:" + now.truncatedTo(ChronoUnit.HOURS)
                            + ":" + normalizeIp(clientIp),
                    properties.getIpHourlyLimit(),
                    Duration.ofHours(1).plusMinutes(1)
            );

            String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
            String codeKey = codeKey(phone, purpose);
            redisTemplate.opsForValue().set(
                    codeKey,
                    digest(phone, purpose, code),
                    properties.getCodeTtl()
            );

            try {
                smsSender.sendVerificationCode(phone, code, purpose);
            } catch (RuntimeException exception) {
                redisTemplate.delete(codeKey);
                throw exception;
            }
        } catch (RuntimeException exception) {
            redisTemplate.delete(cooldownKey);
            throw exception;
        }
    }

    public void verifyAndConsume(String phone, String code, SmsPurpose purpose) {
        Long result = redisTemplate.execute(
                VERIFY_SCRIPT,
                List.of(codeKey(phone, purpose), attemptsKey(phone, purpose)),
                digest(phone, purpose, code),
                Integer.toString(properties.getMaxVerifyAttempts()),
                Long.toString(properties.getCodeTtl().toSeconds())
        );

        if (!Long.valueOf(1L).equals(result)) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_ERROR);
        }
    }

    private void enforceCounter(String key, int limit, Duration ttl) {
        Long allowed = redisTemplate.execute(
                LIMIT_SCRIPT,
                List.of(key),
                Long.toString(Math.max(1, ttl.toSeconds())),
                Integer.toString(limit)
        );
        if (!Long.valueOf(1L).equals(allowed)) {
            throw new BusinessException(ErrorCode.SMS_LIMIT_EXCEEDED);
        }
    }

    private String codeKey(String phone, SmsPurpose purpose) {
        return "sms:code:" + purpose + ":" + phone;
    }

    private String attemptsKey(String phone, SmsPurpose purpose) {
        return "sms:attempts:" + purpose + ":" + phone;
    }

    private String digest(String phone, SmsPurpose purpose, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    properties.getHashSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));
            byte[] value = (purpose + ":" + phone + ":" + code)
                    .getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(mac.doFinal(value));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("无法计算验证码摘要", exception);
        }
    }

    private String normalizeIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "unknown";
        }
        return clientIp.replace(':', '_');
    }
}
