package com.example.railgo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.railgo.data.po.TicketReturn;
import com.example.railgo.data.vo.TicketReturnResponse;
import com.example.railgo.data.vo.row.ReturnableTicketRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TicketReturnMapper extends BaseMapper<TicketReturn> {

    ReturnableTicketRow selectReturnableTicketForUpdate(@Param("ticketId") Long ticketId,
                                                         @Param("userId") Long userId);

    TicketReturn selectByUserAndClientRequestId(@Param("userId") Long userId,
                                                 @Param("clientRequestId") String clientRequestId);

    int countIssuedTickets(@Param("orderId") Long orderId);

    int updateOrderStatus(@Param("orderId") Long orderId,
                          @Param("status") String status);

    TicketReturnResponse selectReturnDetail(@Param("returnId") Long returnId,
                                             @Param("userId") Long userId);
}
