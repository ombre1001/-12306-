package com.example.railgo.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("seat_type")
public class SeatType {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 席别编码，例如 SECOND_CLASS。
     */
    private String code;

    /**
     * 席别名称，例如 二等座。
     */
    private String name;

    /**
     * 前端显示顺序。
     */
    @TableField("sort_no")
    private Integer sortNo;
}