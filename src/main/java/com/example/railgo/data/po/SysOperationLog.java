package com.example.railgo.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class SysOperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String requestId;
    private Long operatorId;
    private String operatorPhone;
    private String module;
    private String action;
    private String requestMethod;
    private String requestUri;
    private String requestParams;
    private String clientIp;
    private Integer responseStatus;
    private String result;
    private String errorMessage;
    private Long durationMs;
    private LocalDateTime operatedAt;
}
