package com.example.railgo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.railgo.data.po.TrainFare;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TrainFareMapper extends BaseMapper<TrainFare> {
}