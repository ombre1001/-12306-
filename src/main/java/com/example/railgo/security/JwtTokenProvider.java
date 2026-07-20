package com.example.railgo.security;


import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    public static final String ACCESS_TYPE = "access";
    public static final String REFRESH_TYPE = "refresh";

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;

        if (properties.getSecret() == null
                || properties.getSecret().isBlank()) {
            throw new IllegalStateException(
                    "app.jwt.secret未配置"
            );
        }

        this.secretKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(
                        properties.getSecret()
                )
        );
    }

    public String createAccessToken(
            Long userId,
            String phone,
            List<String> roles) {

        return createToken(
                userId,
                phone,
                roles,
                ACCESS_TYPE,
                properties.getAccessTokenSeconds()
        );
    }

    public String createRefreshToken(
            Long userId,
            String phone,
            List<String> roles) {

        return createToken(
                userId,
                phone,
                roles,
                REFRESH_TYPE,
                properties.getRefreshTokenSeconds()
        );
    }

    public TokenClaims parseAccessToken(String token) {
        return parseAndRequireType(
                token,
                ACCESS_TYPE,
                ErrorCode.UNAUTHORIZED
        );
    }

    public TokenClaims parseRefreshToken(String token) {
        return parseAndRequireType(
                token,
                REFRESH_TYPE,
                ErrorCode.REFRESH_TOKEN_INVALID
        );
    }

    public long getAccessTokenSeconds() {
        return properties.getAccessTokenSeconds();
    }

    private String createToken(
            Long userId,
            String phone,
            List<String> roles,
            String type,
            long validSeconds) {

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .claim("phone", phone)
                .claim("roles", roles)
                .claim("type", type)
                .issuedAt(Date.from(now))
                .expiration(
                        Date.from(
                                now.plusSeconds(validSeconds)
                        )
                )
                .signWith(secretKey)
                .compact();
    }

    private TokenClaims parseAndRequireType(
            String token,
            String expectedType,
            ErrorCode errorCode) {

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String type = claims.get(
                    "type",
                    String.class
            );

            if (!expectedType.equals(type)) {
                throw new BusinessException(errorCode);
            }

            List<?> rawRoles = claims.get(
                    "roles",
                    List.class
            );

            List<String> roles = rawRoles == null
                    ? List.of()
                    : rawRoles.stream()
                    .map(String::valueOf)
                    .toList();

            return new TokenClaims(
                    Long.valueOf(claims.getSubject()),
                    claims.get("phone", String.class),
                    roles,
                    type,
                    claims.getId(),
                    claims.getExpiration().toInstant()
            );

        } catch (BusinessException exception) {
            throw exception;

        } catch (Exception exception) {
            throw new BusinessException(errorCode);
        }
    }
}
