package com.example.railgo.exception;

/**
 * 支付时发现订单过期后，需要提交“关闭订单并释放库存”的事务，
 * 因此该异常由 PaymentService 配置为不回滚，其余业务异常仍正常回滚。
 */
public class OrderExpiredException extends BusinessException {
    public OrderExpiredException() {
        super(ErrorCode.ORDER_EXPIRED);
    }
}
