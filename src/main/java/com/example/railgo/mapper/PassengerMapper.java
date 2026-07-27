package com.example.railgo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.railgo.data.po.Passenger;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PassengerMapper
        extends BaseMapper<Passenger> {
}