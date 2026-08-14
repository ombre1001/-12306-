package com.example.railgo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.railgo.data.po.TrainRun;
import com.example.railgo.data.vo.admin.AdminTrainRunResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface TrainRunMapper extends BaseMapper<TrainRun> {

    @Select("""
            SELECT COUNT(*)
            FROM order_item
            WHERE run_id = #{runId}
              AND status IN ('LOCKED', 'ISSUED')
            """)
    int countEffectiveOrderItems(
            @Param("runId") Long runId
    );

    @Select("""
            <script>
            SELECT
                tr.id,
                tr.train_id,
                t.train_no,
                tr.run_date,
                tr.sale_status,
                tr.inventory_initialized,
                tr.inventory_initialized_at,
                tr.source_managed,
                tr.source_status,
                tr.source_checked_at,
                tr.source_last_seen_at,
                tr.source_valid_until,
                tr.source_hash,
                tr.created_at,
                tr.updated_at
            FROM train_run tr
            INNER JOIN train t
                ON t.id = tr.train_id
            WHERE 1 = 1
            <if test="trainId != null">
                AND tr.train_id = #{trainId}
            </if>
            <if test="startDate != null">
                AND tr.run_date &gt;= #{startDate}
            </if>
            <if test="endDate != null">
                AND tr.run_date &lt;= #{endDate}
            </if>
            <if test="saleStatus != null and saleStatus != ''">
                AND tr.sale_status = #{saleStatus}
            </if>
            ORDER BY
                tr.run_date DESC,
                t.train_no ASC,
                tr.id ASC
            </script>
            """)
    IPage<AdminTrainRunResponse> selectAdminRunPage(
            Page<AdminTrainRunResponse> page,
            @Param("trainId") Long trainId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("saleStatus") String saleStatus
    );
}