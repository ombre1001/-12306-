package com.example._2306.utils;

import com.example._2306.data.po.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SecureDigestAlgorithm;
import lombok.SneakyThrows;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
* JWT加解密工具类
*
* author: koishikiss
* launch: 2024/11/1
* last update: 2025/7/17
* */

public class JwtUtil {

    //jwt加密方式
    private static final SecureDigestAlgorithm<SecretKey,SecretKey> ALGORITHM = Jwts.SIG.HS256;

    private static final String CLAIM_KEY_CAS_ID = "casID";

    private static final String CLAIM_KEY_NAME = "name";

    private static final String CLAIM_KEY_ROLE = "role";
    private static final long LONG_TERM_EXPIRE = 30L * 24 * 60 * 60 * 1000;

    // 十秒过期
    private static final long expire = 360000;

    private static final byte[] salt = "KOISHIKISHIKAWAIIKAWAIIKISSKISSLOVELY".getBytes(StandardCharsets.UTF_8);

    private static final int iterationCount = 114514;

    private static final Map<String, SecretKey> KEY_CACHE = new ConcurrentHashMap<>();

    @SneakyThrows
    private static SecretKey generateSecretKey(String key) {
        SecretKey secretKey = KEY_CACHE.get(key);
        if (secretKey == null) {
            PBEKeySpec spec = new PBEKeySpec(key.toCharArray(), salt, iterationCount, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] secretBytes = factory.generateSecret(spec).getEncoded();
            secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
            KEY_CACHE.put(key, secretKey);
        }
        return secretKey;
    }

    public static String generate(String key, String casID, String name) {
        return createToken(key, casID, name, expire);
    }

    /**
     * 新增：生成长效/永久 Token (100 年)
     */
    public static String generateLongTerm(String key, String casID, String name) {
        return createToken(key, casID, name, LONG_TERM_EXPIRE);
    }

    /**
     * 内部通用构建方法
     */
    private static String createToken(String key, String casID, String name, long expirationTime) {
        SecretKey secretKey = generateSecretKey(key);
        return Jwts.builder()
                .header().add("type", "JWT")
                .and()
                .claim(CLAIM_KEY_CAS_ID, casID)
                .claim(CLAIM_KEY_NAME, name)
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(secretKey, ALGORITHM)
                .compact();
    }

    public static String generateWithRole(String key, String casID, String name, Integer role) {
        SecretKey secretKey = generateSecretKey(key);
        return Jwts.builder()
                .header().add("type", "JWT")
                .and()
                .claim(CLAIM_KEY_CAS_ID, casID)
                .claim(CLAIM_KEY_NAME, name)
                .claim(CLAIM_KEY_ROLE, role) // 将 role 打包进 token
                .expiration(new Date(System.currentTimeMillis() + expire))
                .signWith(secretKey, ALGORITHM)
                .compact();
    }

    //解析token，得到包装的casId
    public static User getClaim(String token, String key){
        SecretKey secretKey = generateSecretKey(key);
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String casID = claims.get(CLAIM_KEY_CAS_ID, String.class);
            String name = claims.get(CLAIM_KEY_NAME, String.class);
            Integer role = claims.get(CLAIM_KEY_ROLE, Integer.class);

            User user = new User(casID, name);
            // 假设你的 User 实体类有 setRole 方法
            if (role != null) {
                user.setRole(role);
            }
            return user;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }


}

/*
* using dependency:
<!--Java JSON Web Token , 生成Token-->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt</artifactId>
    <version>0.12.3</version>
</dependency>
* */
