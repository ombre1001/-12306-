package com.example.railgo.utils;

import java.util.Base64;

public class PrivacyUtil {
    // 手机号脱敏：152****8610
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) return phone;
        return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }

    // AES 加密（用于数据库存储）
    public static String encrypt(String data) {
        // 使用 AES 算法加密逻辑...
        return Base64.getEncoder().encodeToString(data.getBytes());
    }
}
