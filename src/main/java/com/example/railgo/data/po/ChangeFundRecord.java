package com.example.railgo.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("change_fund_record")
public class ChangeFundRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long changeId;
    private String fundNo;
    private String fundType;
    private BigDecimal amount;
    private String status;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
}
