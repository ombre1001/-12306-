package com.example.railgo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.railgo.data.po.TrainSeat;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TrainSeatMapper extends BaseMapper<TrainSeat> {
}