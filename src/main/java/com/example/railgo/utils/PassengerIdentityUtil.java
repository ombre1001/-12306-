package com.example.railgo.utils;

import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class PassengerIdentityUtil {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKeySpec encryptionKey;
    private final SecretKeySpec hashKey;

    private final SecureRandom secureRandom =
            new SecureRandom();

    public PassengerIdentityUtil(
            @Value("${app.passenger.identity-secret}")
            String base64Secret) {

        byte[] key;

        try {
            key = Base64.getDecoder()
                    .decode(base64Secret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "乘车人证件密钥不是合法Base64",
                    exception
            );
        }

        if (key.length != 32) {
            throw new IllegalStateException(
                    "乘车人证件密钥解码后必须为32字节"
            );
        }

        this.encryptionKey =
                new SecretKeySpec(key, "AES");

        this.hashKey =
                new SecretKeySpec(key, "HmacSHA256");
    }

    public String normalize(String idNo) {
        if (idNo == null) {
            throw new BusinessException(
                    ErrorCode.PARAM_ERROR,
                    "证件号码不能为空"
            );
        }

        return idNo
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);
    }

    public String encrypt(String value) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher =
                    Cipher.getInstance(
                            "AES/GCM/NoPadding"
                    );

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    encryptionKey,
                    new GCMParameterSpec(
                            GCM_TAG_LENGTH,
                            iv
                    )
            );

            byte[] encrypted =
                    cipher.doFinal(
                            value.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            byte[] result =
                    new byte[iv.length + encrypted.length];

            System.arraycopy(
                    iv,
                    0,
                    result,
                    0,
                    iv.length
            );

            System.arraycopy(
                    encrypted,
                    0,
                    result,
                    iv.length,
                    encrypted.length
            );

            return Base64.getEncoder()
                    .encodeToString(result);

        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "证件号码加密失败",
                    exception
            );
        }
    }

    public String decrypt(String value) {
        try {
            byte[] input =
                    Base64.getDecoder().decode(value);

            if (input.length <= GCM_IV_LENGTH) {
                throw new GeneralSecurityException(
                        "证件号码密文格式错误"
                );
            }

            byte[] iv =
                    new byte[GCM_IV_LENGTH];

            byte[] encrypted =
                    new byte[
                            input.length - GCM_IV_LENGTH
                            ];

            System.arraycopy(
                    input,
                    0,
                    iv,
                    0,
                    iv.length
            );

            System.arraycopy(
                    input,
                    iv.length,
                    encrypted,
                    0,
                    encrypted.length
            );

            Cipher cipher =
                    Cipher.getInstance(
                            "AES/GCM/NoPadding"
                    );

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    encryptionKey,
                    new GCMParameterSpec(
                            GCM_TAG_LENGTH,
                            iv
                    )
            );

            return new String(
                    cipher.doFinal(encrypted),
                    StandardCharsets.UTF_8
            );

        } catch (
                GeneralSecurityException
                | IllegalArgumentException exception
        ) {
            throw new IllegalStateException(
                    "证件号码解密失败",
                    exception
            );
        }
    }

    public String hash(String value) {
        try {
            Mac mac =
                    Mac.getInstance("HmacSHA256");

            mac.init(hashKey);

            byte[] result =
                    mac.doFinal(
                            value.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat.of()
                    .formatHex(result);

        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "证件号码摘要计算失败",
                    exception
            );
        }
    }

    public String mask(String idNo) {
        if (idNo == null || idNo.isBlank()) {
            return "";
        }

        int length = idNo.length();

        if (length <= 4) {
            return "*".repeat(length);
        }

        if (length <= 8) {
            return idNo.substring(0, 2)
                    + "*".repeat(length - 4)
                    + idNo.substring(length - 2);
        }

        return idNo.substring(0, 6)
                + "*".repeat(length - 10)
                + idNo.substring(length - 4);
    }
}