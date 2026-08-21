package com.example.railgo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.railgo.data.po.Station;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StationMapper extends BaseMapper<Station> {

    List<Long> selectMatchedStationIds(
            @Param("keyword") String keyword,
            @Param("normalizedKeyword") String normalizedKeyword,
            @Param("keywordUpper") String keywordUpper,
            @Param("limit") int limit
    );

    List<Station> selectRecentlyPurchasedHotStations(
            @Param("recentDays") int recentDays,
            @Param("limit") int limit
    );
}