package com.example.railgo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.railgo.data.po.TicketOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TicketOrderMapper extends BaseMapper<TicketOrder> {

    TicketOrder selectByClientRequestId(
            @Param("userId") Long userId,
            @Param("clientRequestId") String clientRequestId
    );
}