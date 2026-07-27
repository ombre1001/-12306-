package com.example.railgo.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.railgo.data.enums.TrainType;
import com.example.railgo.data.po.SeatType;
import com.example.railgo.data.vo.SeatTypeResponse;
import com.example.railgo.data.vo.TrainTypeResponse;
import com.example.railgo.mapper.SeatTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DictionaryService {

    private final SeatTypeMapper seatTypeMapper;

    /**
     * 查询数据库中的全部席别。
     */
    @Transactional(readOnly = true)
    public List<SeatTypeResponse> getSeatTypes() {

        List<SeatType> seatTypes =
                seatTypeMapper.selectList(
                        Wrappers.<SeatType>lambdaQuery()
                                .orderByAsc(
                                        SeatType::getSortNo
                                )
                                .orderByAsc(
                                        SeatType::getId
                                )
                );

        return seatTypes.stream()
                .map(this::toSeatTypeResponse)
                .toList();
    }

    /**
     * 查询系统支持的车次类型。
     */
    public List<TrainTypeResponse> getTrainTypes() {

        return Arrays.stream(
                        TrainType.values()
                )
                .map(trainType ->
                        new TrainTypeResponse(
                                trainType.getCode(),
                                trainType.getName(),
                                trainType.getSortNo()
                        )
                )
                .toList();
    }

    private SeatTypeResponse toSeatTypeResponse(
            SeatType seatType) {

        return new SeatTypeResponse(
                seatType.getId(),
                seatType.getCode(),
                seatType.getName(),
                seatType.getSortNo()
        );
    }
}