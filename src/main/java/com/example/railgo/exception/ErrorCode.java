package com.example.railgo.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    PARAM_ERROR(
            40001,
            HttpStatus.BAD_REQUEST,
            "参数校验失败"
    ),

    VERIFICATION_CODE_ERROR(
            40003,
            HttpStatus.BAD_REQUEST,
            "验证码错误或已失效"
    ),

    UNAUTHORIZED(
            40101,
            HttpStatus.UNAUTHORIZED,
            "未登录或令牌已失效"
    ),

    LOGIN_FAILED(
            40102,
            HttpStatus.UNAUTHORIZED,
            "手机号或密码错误"
    ),

    ACCOUNT_LOCKED(
            40103,
            HttpStatus.TOO_MANY_REQUESTS,
            "登录失败次数过多，请稍后再试"
    ),

    REFRESH_TOKEN_INVALID(
            40104,
            HttpStatus.UNAUTHORIZED,
            "刷新令牌无效或已失效"
    ),

    FORBIDDEN(
            40301,
            HttpStatus.FORBIDDEN,
            "无权限执行当前操作"
    ),

    NOT_FOUND(
            40401,
            HttpStatus.NOT_FOUND,
            "资源不存在"
    ),

    PHONE_EXISTS(
            40905,
            HttpStatus.CONFLICT,
            "手机号已注册"
    ),

    OLD_PASSWORD_ERROR(
            40906,
            HttpStatus.CONFLICT,
            "原密码错误"
    ),

    ACCOUNT_DISABLED(
            40907,
            HttpStatus.CONFLICT,
            "账号已被禁用"
    ),

    DATABASE_ERROR(
            50001,
            HttpStatus.INTERNAL_SERVER_ERROR,
            "数据库操作失败"
    ),

    INTERNAL_ERROR(
            50000,
            HttpStatus.INTERNAL_SERVER_ERROR,
            "系统内部错误，请稍后重试"
    );

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(int code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}