package com.example.railgo.service;

import com.example.railgo.config.SmsPurpose;
import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(SmsSender.class)
public class DisabledSmsSender implements SmsSender {
    @Override
    public void sendVerificationCode(String phone, String code, SmsPurpose purpose) {
        throw new BusinessException(ErrorCode.SMS_SEND_FAILED, "短信功能暂未开通，请使用邮箱注册");
    }
}
