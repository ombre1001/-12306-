package com.example.railgo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.railgo.data.po.Station;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StationMapper
        extends BaseMapper<Station> {

    /**
     * 查询最近一段时间内购票次数最多的车站。
     *
     * 每张支付成功的车票：
     * 1. 出发站计一次；
     * 2. 到达站计一次。
     */
    List<Station> selectRecentlyPurchasedHotStations(
            @Param("recentDays") int recentDays,
            @Param("limit") int limit
    );
}