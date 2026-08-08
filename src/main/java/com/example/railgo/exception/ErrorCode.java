package com.example.railgo.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    PARAM_ERROR(40001, HttpStatus.BAD_REQUEST, "参数校验失败"),

    INVALID_ROUTE(40002, HttpStatus.BAD_REQUEST, "出发站和到达站区间不合法"),
    INVALID_TRAVEL_DATE(40004, HttpStatus.BAD_REQUEST, "乘车日期不能早于当前日期"),
    INVALID_QUERY_TIME(40005, HttpStatus.BAD_REQUEST, "查询时间范围不合法"),
    INVALID_TRANSFER_TIME(40006, HttpStatus.BAD_REQUEST, "换乘等待时间范围不合法"),
    TRAIN_NOT_ON_SALE(40011, HttpStatus.BAD_REQUEST, "车次未开售或已经停售"),
    INSUFFICIENT_TICKETS(40012, HttpStatus.BAD_REQUEST, "所选席别余票不足，请重新选择"),
    SEAT_LOCK_FAILED(40013, HttpStatus.BAD_REQUEST, "锁座失败，请重新查询"),
    ORDER_EXPIRED(40014, HttpStatus.BAD_REQUEST, "订单已超时"),
    CHANGE_EXPIRED(40015, HttpStatus.BAD_REQUEST, "改签申请已超时"),

    VERIFICATION_CODE_ERROR(40003, HttpStatus.BAD_REQUEST, "验证码错误或已失效"),

    UNAUTHORIZED(40101, HttpStatus.UNAUTHORIZED, "未登录或令牌已失效"),

    LOGIN_FAILED(40102, HttpStatus.UNAUTHORIZED, "手机号或密码错误"),

    ACCOUNT_LOCKED(40103, HttpStatus.TOO_MANY_REQUESTS, "登录失败次数过多，请稍后再试"),

    REFRESH_TOKEN_INVALID(40104, HttpStatus.UNAUTHORIZED, "刷新令牌无效或已失效"),

    FORBIDDEN(40301, HttpStatus.FORBIDDEN, "无权限执行当前操作"),

    NOT_FOUND(40401, HttpStatus.NOT_FOUND, "资源不存在"),

    TRAIN_RUN_NOT_FOUND(40411, HttpStatus.NOT_FOUND, "运行实例不存在"),
    TRAIN_SEAT_NOT_FOUND(40412, HttpStatus.NOT_FOUND, "该车次尚未配置座位"),
    SEAT_TYPE_NOT_FOUND(40413, HttpStatus.NOT_FOUND, "席别不存在"),
    FARE_NOT_FOUND(40414, HttpStatus.NOT_FOUND, "该区间尚未配置票价"),

    DUPLICATE_CLIENT_REQUEST(40901, HttpStatus.CONFLICT, "请勿重复提交订单"),
    ORDER_STATUS_INVALID(40902, HttpStatus.CONFLICT, "当前订单状态不允许执行此操作"),
    PASSENGER_TRAVEL_CONFLICT(40903, HttpStatus.CONFLICT, "乘车人存在时间冲突的行程"),
    DUPLICATE_PASSENGER(40904, HttpStatus.CONFLICT, "同一订单不能重复选择乘车人"),
    PASSENGER_NOT_OWNED(40302, HttpStatus.FORBIDDEN, "乘车人不存在或不属于当前用户"),
    INVENTORY_NOT_INITIALIZED(40911, HttpStatus.CONFLICT, "该运行实例尚未初始化库存"),
    INVENTORY_INIT_STATUS_INVALID(40912, HttpStatus.CONFLICT, "当前运行状态不允许初始化库存"),
    TRAIN_STOP_NOT_ENOUGH(40913, HttpStatus.CONFLICT, "车次经停站不足，无法生成区间库存"),
    TICKET_CHANGE_NOT_ALLOWED(40931, HttpStatus.CONFLICT, "当前车票不允许改签"),
    TICKET_ALREADY_CHANGED(40932, HttpStatus.CONFLICT, "该车票已经成功改签过一次"),
    CHANGE_ALREADY_PROCESSING(40933, HttpStatus.CONFLICT, "该车票存在未完成的改签申请"),
    CHANGE_STATUS_INVALID(40934, HttpStatus.CONFLICT, "当前改签状态不允许执行此操作"),
    CHANGE_PAYMENT_NOT_REQUIRED(40935, HttpStatus.CONFLICT, "该改签无需补款"),
    CHANGE_DUPLICATE_REQUEST(40936, HttpStatus.CONFLICT, "请勿重复提交改签申请"),

    PHONE_EXISTS(40905, HttpStatus.CONFLICT, "手机号已注册"),

    OLD_PASSWORD_ERROR(40906, HttpStatus.CONFLICT, "原密码错误"),

    ACCOUNT_DISABLED(40907, HttpStatus.CONFLICT, "账号已被禁用"),

    PASSENGER_EXISTS(40908, HttpStatus.CONFLICT, "该证件对应的乘车人已存在"),

    PASSENGER_LIMIT_EXCEEDED(40909, HttpStatus.CONFLICT, "常用乘车人数量已达上限"),

    PASSENGER_IN_USE(40910, HttpStatus.CONFLICT, "乘车人已被有效订单使用，不能删除"),

    DATABASE_ERROR(50001, HttpStatus.INTERNAL_SERVER_ERROR, "数据库操作失败"),

    INVENTORY_INITIALIZATION_FAILED(50011, HttpStatus.INTERNAL_SERVER_ERROR, "区间库存初始化失败"),
    ORDER_CREATE_FAILED(50012, HttpStatus.INTERNAL_SERVER_ERROR, "订单创建失败"),
    ORDER_ITEM_CREATE_FAILED(50013, HttpStatus.INTERNAL_SERVER_ERROR, "订单明细创建失败"),
    ORDER_ITEM_UPDATE_FAILED(50014, HttpStatus.INTERNAL_SERVER_ERROR, "订单明细更新失败"),
    ORDER_CANCEL_FAILED(50015, HttpStatus.INTERNAL_SERVER_ERROR, "订单取消失败"),
    CHANGE_CREATE_FAILED(50016, HttpStatus.INTERNAL_SERVER_ERROR, "改签申请创建失败"),
    CHANGE_CONFIRM_FAILED(50017, HttpStatus.INTERNAL_SERVER_ERROR, "改签确认失败"),

    // 管理端车站
    STATION_NOT_FOUND(40421, HttpStatus.NOT_FOUND, "车站不存在"),
    STATION_CODE_EXISTS(40921, HttpStatus.CONFLICT, "车站编码已存在"),
    STATION_NAME_EXISTS(40922, HttpStatus.CONFLICT, "车站名称已存在"),
    STATION_STATUS_INVALID(40021, HttpStatus.BAD_REQUEST, "车站状态不合法"),

    // 管理端车次
    TRAIN_NOT_FOUND(40422, HttpStatus.NOT_FOUND, "车次不存在"),
    TRAIN_NO_EXISTS(40923, HttpStatus.CONFLICT, "车次编号已存在"),
    TRAIN_STATUS_INVALID(40022, HttpStatus.BAD_REQUEST, "车次状态不合法"),
    TRAIN_STRUCTURE_IN_USE(40924, HttpStatus.CONFLICT, "车次已生成运行实例，不能修改运行结构"),

    TRAIN_STOP_INVALID(40023, HttpStatus.BAD_REQUEST, "经停站数据不合法"),
    TRAIN_STOP_SEQ_INVALID(40024, HttpStatus.BAD_REQUEST, "经停站序必须从1开始连续递增"),
    TRAIN_STOP_TIME_INVALID(40025, HttpStatus.BAD_REQUEST, "经停站时刻不合法"),

    COACH_NOT_FOUND(40423, HttpStatus.NOT_FOUND, "车厢不存在"),
    COACH_DUPLICATED(40925, HttpStatus.CONFLICT, "同一车次的车厢号不能重复"),
    SEAT_TEMPLATE_INVALID(40026, HttpStatus.BAD_REQUEST, "座位模板不合法"),
    SEAT_ALREADY_EXISTS(40926, HttpStatus.CONFLICT, "该车厢已经生成座位"),

    FARE_INVALID(40027, HttpStatus.BAD_REQUEST, "票价数据不合法"),
    FARE_DUPLICATED(40927, HttpStatus.CONFLICT, "同一区间、席别的票价重复"),

    // 管理端运行计划
    RUN_DATE_RANGE_INVALID(40028, HttpStatus.BAD_REQUEST, "运行日期范围不合法"),
    RUN_SALE_STATUS_INVALID(40029, HttpStatus.BAD_REQUEST, "运行状态转换不合法"),
    RUN_ALREADY_HAS_ORDER(40928, HttpStatus.CONFLICT, "运行实例已有订单，只能停运，不能删除"),

    INTERNAL_ERROR(50000, HttpStatus.INTERNAL_SERVER_ERROR, "系统内部错误，请稍后重试");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(int code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
