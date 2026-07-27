package com.example.railgo.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("station")
public class Station {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String stationCode;

    private String name;

    private String normalizedName;

    private String pinyin;

    private String pinyinInitial;

    private String province;

    private String city;

    private String district;

    private String address;

    private String railwayBureau;

    private Boolean passengerService;

    private Boolean luggageService;

    private Boolean parcelService;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private Integer hotScore;

    private String status;

    private String sourceUrl;

    private String sourceHash;

    private LocalDateTime sourceFetchedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}