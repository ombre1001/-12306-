package com.example.railgo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.railgo.data.po.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {

    PaymentRecord selectByOrderAndClientRequestId(@Param("orderId") Long orderId,
                                                   @Param("clientRequestId") String clientRequestId);

    PaymentRecord selectSuccessfulByOrderIdForUpdate(@Param("orderId") Long orderId);

    PaymentRecord selectProcessingByOrderIdForUpdate(@Param("orderId") Long orderId);

    PaymentRecord selectOwnedByPaymentNo(@Param("paymentNo") String paymentNo,
                                         @Param("userId") Long userId);

    PaymentRecord selectOwnedByPaymentNoForUpdate(@Param("paymentNo") String paymentNo,
                                                  @Param("userId") Long userId);
}
