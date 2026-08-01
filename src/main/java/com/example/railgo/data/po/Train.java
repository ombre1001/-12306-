package com.example.railgo.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("train")
public class Train {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("train_no")
    private String trainNo;

    @TableField("train_type")
    private String trainType;

    /**
     * 对应请求中的 fromStationId
     */
    @TableField("origin_station_id")
    private Long originStationId;

    /**
     * 对应请求中的 toStationId
     */
    @TableField("destination_station_id")
    private Long destinationStationId;

    @TableField("status")
    private String status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}