package com.example.railgo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.railgo.data.po.TrainRun;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TrainRunMapper extends BaseMapper<TrainRun> {

    @Select("""
            SELECT COUNT(*)
            FROM order_item
            WHERE run_id = #{runId}
              AND status IN ('LOCKED', 'ISSUED')
            """)
    int countEffectiveOrderItems(@Param("runId") Long runId);
}