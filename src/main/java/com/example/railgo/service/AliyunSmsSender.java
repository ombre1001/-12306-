package com.example.railgo.service;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.example.railgo.config.SmsProperties;
import com.example.railgo.config.SmsPurpose;
import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Service
@ConditionalOnProperty(prefix = "app.sms", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class AliyunSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(AliyunSmsSender.class);

    private final Client aliyunSmsClient;
    private final SmsProperties properties;

    @Override
    public void sendVerificationCode(String phone, String code, SmsPurpose purpose) {
        SendSmsRequest request = new SendSmsRequest()
                .setPhoneNumbers(phone)
                .setSignName(properties.getSignName())
                .setTemplateCode(properties.templateCode(purpose))
                .setTemplateParam("{\"code\":\"" + code + "\"}");

        try {
            SendSmsResponse response = aliyunSmsClient.sendSms(request);
            String responseCode = response.getBody() == null
                    ? null
                    : response.getBody().getCode();

            if (!"OK".equals(responseCode)) {
                String requestId = response.getBody() == null
                        ? null
                        : response.getBody().getRequestId();
                log.error("阿里云短信发送失败: responseCode={}, requestId={}", responseCode, requestId);
                throw new BusinessException(ErrorCode.SMS_SEND_FAILED);
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("调用阿里云短信服务失败", exception);
            throw new BusinessException(ErrorCode.SMS_SEND_FAILED);
        }
    }
}
