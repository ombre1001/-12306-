package com.example.railgo.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.railgo.data.vo.MyTicketDetailResponse;
import com.example.railgo.data.vo.MyTicketResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MyTicketMapper {

    IPage<MyTicketResponse> selectMyTicketPage(
            Page<MyTicketResponse> page,
            @Param("userId") Long userId,
            @Param("status") String status);

    MyTicketDetailResponse selectOwnedTicketDetail(
            @Param("ticketId") Long ticketId,
            @Param("userId") Long userId);
}
