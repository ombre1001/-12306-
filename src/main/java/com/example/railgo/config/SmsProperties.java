package com.example.railgo.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "app.sms")
public class SmsProperties {

    private boolean enabled = false;

    @NotBlank
    private String accessKeyId;

    @NotBlank
    private String accessKeySecret;

    @NotBlank
    private String endpoint = "dysmsapi.aliyuncs.com";

    @NotBlank
    private String signName;

    @NotBlank
    private String registerTemplateCode;

    @NotBlank
    private String loginTemplateCode;

    @NotBlank
    private String hashSecret;

    @NotNull
    private Duration codeTtl = Duration.ofMinutes(5);

    @NotNull
    private Duration sendCooldown = Duration.ofSeconds(60);

    @Min(1)
    private int phoneDailyLimit = 10;

    @Min(1)
    private int ipHourlyLimit = 30;

    @Min(1)
    private int maxVerifyAttempts = 5;

    public String templateCode(SmsPurpose purpose) {
        return switch (purpose) {
            case REGISTER -> registerTemplateCode;
            case LOGIN -> loginTemplateCode;
        };
    }
}
