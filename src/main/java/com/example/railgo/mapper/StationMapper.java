package com.example.railgo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.railgo.data.po.Station;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

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

    @Update("""
            UPDATE train
            SET origin_station_id = #{keepId}
            WHERE origin_station_id = #{duplicateId}
            """)
    int replaceTrainOriginStation(
            @Param("keepId") Long keepId,
            @Param("duplicateId") Long duplicateId
    );

    @Update("""
            UPDATE train
            SET destination_station_id = #{keepId}
            WHERE destination_station_id = #{duplicateId}
            """)
    int replaceTrainDestinationStation(
            @Param("keepId") Long keepId,
            @Param("duplicateId") Long duplicateId
    );

    @Update("""
            UPDATE train_stop
            SET station_id = #{keepId}
            WHERE station_id = #{duplicateId}
            """)
    int replaceTrainStopStation(
            @Param("keepId") Long keepId,
            @Param("duplicateId") Long duplicateId
    );

    @Update("""
            UPDATE order_item
            SET from_station_id = #{keepId}
            WHERE from_station_id = #{duplicateId}
            """)
    int replaceOrderItemFromStation(
            @Param("keepId") Long keepId,
            @Param("duplicateId") Long duplicateId
    );

    @Update("""
            UPDATE order_item
            SET to_station_id = #{keepId}
            WHERE to_station_id = #{duplicateId}
            """)
    int replaceOrderItemToStation(
            @Param("keepId") Long keepId,
            @Param("duplicateId") Long duplicateId
    );
}
