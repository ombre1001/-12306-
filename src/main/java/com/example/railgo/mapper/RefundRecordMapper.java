package com.example.railgo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.railgo.data.po.RefundRecord;
import com.example.railgo.data.vo.RefundDetailResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RefundRecordMapper extends BaseMapper<RefundRecord> {

    RefundDetailResponse selectOwnedRefundDetail(@Param("refundId") Long refundId,
                                                  @Param("userId") Long userId);
}
