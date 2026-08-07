package com.example.railgo.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("train_sync_log")
public class TrainSyncLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String batchId;
    private String source;
    private LocalDate sourceDate;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String result;
    private Integer trainCount;
    private Integer stopCount;
    private String errorText;
}