package com.example.railgo.security;


import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String secret;

    private long accessTokenSeconds = 7200;

    private long refreshTokenSeconds = 2592000;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenSeconds() {
        return accessTokenSeconds;
    }

    public void setAccessTokenSeconds(long accessTokenSeconds) {
        this.accessTokenSeconds = accessTokenSeconds;
    }

    public long getRefreshTokenSeconds() {
        return refreshTokenSeconds;
    }

    public void setRefreshTokenSeconds(long refreshTokenSeconds) {
        this.refreshTokenSeconds = refreshTokenSeconds;
    }
}