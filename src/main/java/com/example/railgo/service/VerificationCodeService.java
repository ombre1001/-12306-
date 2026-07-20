package com.example.railgo.service;


import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VerificationCodeService {

    private final String developmentCode;

    public VerificationCodeService(
            @Value("${app.verification.development-code:123456}")
            String developmentCode) {

        this.developmentCode = developmentCode;
    }

    public void verify(
            String phone,
            String verificationCode) {

        if (verificationCode == null
                || !developmentCode.equals(
                verificationCode
        )) {

            throw new BusinessException(
                    ErrorCode.VERIFICATION_CODE_ERROR
            );
        }
    }
}