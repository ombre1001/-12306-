package com.example.railgo.service;

import com.example.railgo.config.EmailCodeProperties;
import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailVerificationCodeService {
    private static final Logger log = LoggerFactory.getLogger(EmailVerificationCodeService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final DefaultRedisScript<Long> VERIFY_SCRIPT = new DefaultRedisScript<>("""
            local stored = redis.call('GET', KEYS[1])
            if not stored then return 0 end
            if stored == ARGV[1] then
                redis.call('DEL', KEYS[1], KEYS[2])
                return 1
            end
            local attempts = redis.call('INCR', KEYS[2])
            redis.call('EXPIRE', KEYS[2], ARGV[3])
            if attempts >= tonumber(ARGV[2]) then redis.call('DEL', KEYS[1]) end
            return -1
            """, Long.class);

    private static final DefaultRedisScript<Long> LIMIT_SCRIPT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
            if count > tonumber(ARGV[2]) then return 0 end
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;
    private final EmailCodeProperties properties;

    public void sendRegistrationCode(String rawEmail, String clientIp) {
        String email = normalize(rawEmail);
        String emailKey = keyId(email);
        String cooldownKey = "email:cooldown:register:" + emailKey;
        Boolean reserved = redisTemplate.opsForValue()
                .setIfAbsent(cooldownKey, "1", properties.getSendCooldown());
        if (!Boolean.TRUE.equals(reserved)) {
            throw new BusinessException(ErrorCode.EMAIL_TOO_FREQUENT);
        }

        String codeKey = "email:code:register:" + emailKey;
        try {
            LocalDateTime now = LocalDateTime.now();
            enforceCounter("email:daily:" + now.toLocalDate() + ":" + emailKey,
                    properties.getEmailDailyLimit(),
                    Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).plusSeconds(1));
            enforceCounter("email:ip-hour:" + now.truncatedTo(ChronoUnit.HOURS) + ":" + normalizeIp(clientIp),
                    properties.getIpHourlyLimit(), Duration.ofHours(1).plusMinutes(1));

            String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
            redisTemplate.opsForValue().set(codeKey, digest(email, code), properties.getCodeTtl());

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.getFrom());
            message.setTo(email);
            message.setSubject("RailGo 注册验证码");
            message.setText("您的 RailGo 注册验证码为：" + code
                    + "\n\n验证码 " + properties.getCodeTtl().toMinutes()
                    + " 分钟内有效，请勿转发或泄露。若非本人操作，请忽略本邮件。");
            mailSender.send(message);
        } catch (MailException exception) {
            redisTemplate.delete(codeKey);
            redisTemplate.delete(cooldownKey);
            log.error("注册验证码邮件发送失败: emailHash={}", emailKey, exception);
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        } catch (RuntimeException exception) {
            redisTemplate.delete(cooldownKey);
            throw exception;
        }
    }

    public void verifyAndConsume(String rawEmail, String code) {
        String email = normalize(rawEmail);
        String emailKey = keyId(email);
        Long result = redisTemplate.execute(
                VERIFY_SCRIPT,
                List.of("email:code:register:" + emailKey, "email:attempts:register:" + emailKey),
                digest(email, code),
                Integer.toString(properties.getMaxVerifyAttempts()),
                Long.toString(properties.getCodeTtl().toSeconds()));
        if (!Long.valueOf(1L).equals(result)) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_ERROR);
        }
    }

    public String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void enforceCounter(String key, int limit, Duration ttl) {
        Long allowed = redisTemplate.execute(LIMIT_SCRIPT, List.of(key),
                Long.toString(Math.max(1, ttl.toSeconds())), Integer.toString(limit));
        if (!Long.valueOf(1L).equals(allowed)) {
            throw new BusinessException(ErrorCode.EMAIL_LIMIT_EXCEEDED);
        }
    }

    private String digest(String email, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getHashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(("REGISTER:" + email + ":" + code)
                    .getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("无法计算邮箱验证码摘要", exception);
        }
    }

    private String keyId(String email) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(email.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("无法计算邮箱标识", exception);
        }
    }

    private String normalizeIp(String clientIp) {
        return clientIp == null || clientIp.isBlank() ? "unknown" : clientIp.replace(':', '_');
    }
}
