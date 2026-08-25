package com.example.railgo.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@Validated
@ConfigurationProperties(prefix = "app.email-code")
public class EmailCodeProperties {
    @NotBlank
    private String from;
    @NotBlank
    private String hashSecret;
    @NotNull
    private Duration codeTtl = Duration.ofMinutes(10);
    @NotNull
    private Duration sendCooldown = Duration.ofSeconds(60);
    @Min(1)
    private int emailDailyLimit = 10;
    @Min(1)
    private int ipHourlyLimit = 30;
    @Min(1)
    private int maxVerifyAttempts = 5;
}
