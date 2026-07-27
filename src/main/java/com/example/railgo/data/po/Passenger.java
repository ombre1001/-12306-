package com.example.railgo.data.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("passenger")
public class Passenger {

    @TableId(type = com.baomidou.mybatisplus.annotation.IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private String idType;

    private String idNoCipher;

    private String idNoHash;

    private String passengerType;

    private String phone;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}