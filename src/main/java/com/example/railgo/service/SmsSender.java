package com.example.railgo.service;

import com.example.railgo.config.SmsPurpose;

public interface SmsSender {
    void sendVerificationCode(String phone, String code, SmsPurpose purpose);
}
