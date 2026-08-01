package com.example.railgo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.railgo.data.dto.*;
import com.example.railgo.data.po.*;
import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import com.example.railgo.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminTrainService {

    private static final LocalDate BASE_DATE =
            LocalDate.of(2000, 1, 1);

    private final TrainMapper trainMapper;
    private final TrainStopMapper trainStopMapper;
    private final TrainCoachMapper trainCoachMapper;
    private final TrainSeatMapper trainSeatMapper;
    private final TrainFareMapper trainFareMapper;
    private final TrainRunMapper trainRunMapper;
    private final StationMapper stationMapper;
    private final SeatTypeMapper seatTypeMapper;

    public IPage<Train> page(
            long page,
            long size,
            String keyword,
            String trainType,
            String status
    ) {
        return trainMapper.selectPage(
                new Page<>(page, size),
                Wrappers.<Train>lambdaQuery()
                        .like(
                                keyword != null && !keyword.isBlank(),
                                Train::getTrainNo,
                                keyword == null ? null : keyword.trim()
                        )
                        .eq(
                                trainType != null && !trainType.isBlank(),
                                Train::getTrainType,
                                trainType
                        )
                        .eq(
                                status != null && !status.isBlank(),
                                Train::getStatus,
                                status
                        )
                        .orderByAsc(Train::getTrainNo)
        );
    }

    public Train getTrain(Long trainId) {
        return requireTrain(trainId);
    }

    @Transactional(rollbackFor = Exception.class)
    public  Train createTrain(AdminTrainSaveRequest request) {
        String trainNo = request.getTrainNo().trim().toUpperCase();
        String trainType = request.getTrainType().trim().toUpperCase();
        String status = request.getStatus().trim().toUpperCase();

        if (request.getFromStationId().equals(request.getToStationId())) {
            throw new IllegalArgumentException("始发站和终到站不能相同");
        }

        Station fromStation = stationMapper.selectById(request.getFromStationId());
        if (fromStation == null) {
            throw new IllegalArgumentException(
                    "始发站不存在，stationId=" + request.getFromStationId()
            );
        }

        Station toStation = stationMapper.selectById(request.getToStationId());
        if (toStation == null) {
            throw new IllegalArgumentException(
                    "终到站不存在，stationId=" + request.getToStationId()
            );
        }

        Long count = trainMapper.selectCount(
                Wrappers.<Train>lambdaQuery()
                        .eq(Train::getTrainNo, trainNo)
        );

        if (count > 0) {
            throw new IllegalArgumentException("车次号已存在：" + trainNo);
        }

        Train train = new Train();
        train.setTrainNo(trainNo);
        train.setTrainType(trainType);

        // 关键修复
        train.setOriginStationId(request.getFromStationId());
        train.setDestinationStationId(request.getToStationId());

        train.setStatus(status);
        train.setCreatedAt(LocalDateTime.now());
        train.setUpdatedAt(LocalDateTime.now());

        int affectedRows = trainMapper.insert(train);
        if (affectedRows != 1) {
            throw new IllegalStateException("新增车次失败");
        }

        return train;
    }

    @Transactional
    public Train updateTrain(
            Long trainId,
            AdminTrainSaveRequest request
    ) {
        Train train = requireTrain(trainId);

        checkTrainNoUnique(
                request.trainNo(),
                trainId
        );

        train.setTrainNo(
                request.trainNo().trim()
                        .toUpperCase(Locale.ROOT)
        );
        train.setTrainType(request.trainType());
        train.setStatus(request.status());
        train.setUpdatedAt(LocalDateTime.now());

        trainMapper.updateById(train);
        return train;
    }

    public List<TrainStop> getStops(Long trainId) {
        requireTrain(trainId);

        return trainStopMapper.selectList(
                Wrappers.<TrainStop>lambdaQuery()
                        .eq(TrainStop::getTrainId, trainId)
                        .orderByAsc(TrainStop::getStopSeq)
        );
    }

    @Transactional
    public void saveStops(
            Long trainId,
            AdminTrainStopsRequest request
    ) {
        requireTrain(trainId);
        ensureStructureEditable(trainId);

        List<AdminTrainStopsRequest.StopItem> stops =
                new ArrayList<>(request.stops());

        stops.sort(
                Comparator.comparing(
                        AdminTrainStopsRequest.StopItem::stopSeq
                )
        );

        validateStops(stops);

        trainStopMapper.delete(
                Wrappers.<TrainStop>lambdaQuery()
                        .eq(TrainStop::getTrainId, trainId)
        );

        for (AdminTrainStopsRequest.StopItem item : stops) {
            TrainStop stop = new TrainStop();
            stop.setTrainId(trainId);
            stop.setStationId(item.stationId());
            stop.setStopSeq(item.stopSeq());
            stop.setArrivalTime(item.arrivalTime());
            stop.setArrivalDayOffset(
                    item.arrivalDayOffset()
            );
            stop.setDepartureTime(item.departureTime());
            stop.setDepartureDayOffset(
                    item.departureDayOffset()
            );
            stop.setDistanceKm(item.distanceKm());

            trainStopMapper.insert(stop);
        }
    }

    public List<TrainCoach> getCoaches(Long trainId) {
        requireTrain(trainId);

        return trainCoachMapper.selectList(
                Wrappers.<TrainCoach>lambdaQuery()
                        .eq(TrainCoach::getTrainId, trainId)
                        .orderByAsc(TrainCoach::getCoachNo)
        );
    }

    @Transactional
    public void saveCoaches(
            Long trainId,
            AdminCoachesRequest request
    ) {
        requireTrain(trainId);
        ensureStructureEditable(trainId);

        Set<String> coachNumbers = new HashSet<>();

        for (AdminCoachesRequest.CoachItem item
                : request.coaches()) {

            String coachNo = item.coachNo().trim();

            if (!coachNumbers.add(coachNo)) {
                throw new BusinessException(
                        ErrorCode.COACH_DUPLICATED
                );
            }

            SeatType seatType =
                    seatTypeMapper.selectById(
                            item.seatTypeId()
                    );

            if (seatType == null) {
                throw new BusinessException(
                        ErrorCode.SEAT_TYPE_NOT_FOUND
                );
            }
        }

        List<TrainCoach> oldCoaches = getCoaches(trainId);

        for (TrainCoach coach : oldCoaches) {
            trainSeatMapper.delete(
                    Wrappers.<TrainSeat>lambdaQuery()
                            .eq(
                                    TrainSeat::getCoachId,
                                    coach.getId()
                            )
            );
        }

        trainCoachMapper.delete(
                Wrappers.<TrainCoach>lambdaQuery()
                        .eq(TrainCoach::getTrainId, trainId)
        );

        for (AdminCoachesRequest.CoachItem item
                : request.coaches()) {

            TrainCoach coach = new TrainCoach();
            coach.setTrainId(trainId);
            coach.setCoachNo(item.coachNo().trim());
            coach.setSeatTypeId(item.seatTypeId());
            coach.setCapacity(0);

            trainCoachMapper.insert(coach);
        }
    }

    @Transactional
    public int generateSeats(
            Long trainId,
            AdminSeatGenerateRequest request
    ) {
        requireTrain(trainId);
        ensureStructureEditable(trainId);

        int inserted = 0;

        for (AdminSeatGenerateRequest.CoachTemplate template
                : request.templates()) {

            if (template.startRow() > template.endRow()) {
                throw new BusinessException(
                        ErrorCode.SEAT_TEMPLATE_INVALID,
                        "起始排不能大于结束排"
                );
            }

            TrainCoach coach =
                    trainCoachMapper.selectOne(
                            Wrappers.<TrainCoach>lambdaQuery()
                                    .eq(
                                            TrainCoach::getTrainId,
                                            trainId
                                    )
                                    .eq(
                                            TrainCoach::getCoachNo,
                                            template.coachNo().trim()
                                    )
                    );

            if (coach == null) {
                throw new BusinessException(
                        ErrorCode.COACH_NOT_FOUND,
                        "车厢不存在：" + template.coachNo()
                );
            }

            Long seatCount =
                    trainSeatMapper.selectCount(
                            Wrappers.<TrainSeat>lambdaQuery()
                                    .eq(
                                            TrainSeat::getCoachId,
                                            coach.getId()
                                    )
                    );

            if (seatCount > 0
                    && !Boolean.TRUE.equals(
                    request.overwrite()
            )) {
                throw new BusinessException(
                        ErrorCode.SEAT_ALREADY_EXISTS,
                        "车厢" + coach.getCoachNo()
                                + "已经生成座位"
                );
            }

            if (seatCount > 0) {
                trainSeatMapper.delete(
                        Wrappers.<TrainSeat>lambdaQuery()
                                .eq(
                                        TrainSeat::getCoachId,
                                        coach.getId()
                                )
                );
            }

            Set<String> letters = new LinkedHashSet<>(
                    template.seatLetters()
            );

            for (int row = template.startRow();
                 row <= template.endRow();
                 row++) {

                for (String rawLetter : letters) {
                    String letter = rawLetter
                            .trim()
                            .toUpperCase(Locale.ROOT);

                    TrainSeat seat = new TrainSeat();
                    seat.setCoachId(coach.getId());
                    seat.setRowNo(row);
                    seat.setSeatLetter(letter);
                    seat.setSeatNo(
                            String.format("%02d%s", row, letter)
                    );
                    seat.setEnabled(true);

                    trainSeatMapper.insert(seat);
                    inserted++;
                }
            }

            coach.setCapacity(
                    (template.endRow()
                            - template.startRow() + 1)
                            * letters.size()
            );
            trainCoachMapper.updateById(coach);
        }

        return inserted;
    }

    public List<TrainFare> getFares(Long trainId) {
        requireTrain(trainId);

        return trainFareMapper.selectList(
                Wrappers.<TrainFare>lambdaQuery()
                        .eq(TrainFare::getTrainId, trainId)
                        .orderByAsc(TrainFare::getFromSeq)
                        .orderByAsc(TrainFare::getToSeq)
                        .orderByAsc(TrainFare::getSeatTypeId)
        );
    }

    @Transactional
    public void saveFares(
            Long trainId,
            AdminFaresRequest request
    ) {
        requireTrain(trainId);
        ensureStructureEditable(trainId);

        int stopCount = Math.toIntExact(
                trainStopMapper.selectCount(
                        Wrappers.<TrainStop>lambdaQuery()
                                .eq(
                                        TrainStop::getTrainId,
                                        trainId
                                )
                )
        );

        if (stopCount < 2) {
            throw new BusinessException(
                    ErrorCode.TRAIN_STOP_NOT_ENOUGH
            );
        }

        Set<String> uniqueKeys = new HashSet<>();

        for (AdminFaresRequest.FareItem item
                : request.fares()) {

            if (item.fromSeq() >= item.toSeq()
                    || item.toSeq() > stopCount) {
                throw new BusinessException(
                        ErrorCode.FARE_INVALID,
                        "票价区间不合法："
                                + item.fromSeq()
                                + "→"
                                + item.toSeq()
                );
            }

            SeatType seatType =
                    seatTypeMapper.selectById(
                            item.seatTypeId()
                    );

            if (seatType == null) {
                throw new BusinessException(
                        ErrorCode.SEAT_TYPE_NOT_FOUND
                );
            }

            String key = item.fromSeq()
                    + ":"
                    + item.toSeq()
                    + ":"
                    + item.seatTypeId();

            if (!uniqueKeys.add(key)) {
                throw new BusinessException(
                        ErrorCode.FARE_DUPLICATED
                );
            }
        }

        trainFareMapper.delete(
                Wrappers.<TrainFare>lambdaQuery()
                        .eq(TrainFare::getTrainId, trainId)
        );

        for (AdminFaresRequest.FareItem item
                : request.fares()) {

            TrainFare fare = new TrainFare();
            fare.setTrainId(trainId);
            fare.setFromSeq(item.fromSeq());
            fare.setToSeq(item.toSeq());
            fare.setSeatTypeId(item.seatTypeId());
            fare.setPrice(item.price());

            trainFareMapper.insert(fare);
        }
    }

    private void validateStops(
            List<AdminTrainStopsRequest.StopItem> stops
    ) {
        Set<Long> stationIds = new HashSet<>();
        Integer previousDistance = null;
        LocalDateTime previousTime = null;

        for (int index = 0; index < stops.size(); index++) {
            AdminTrainStopsRequest.StopItem item =
                    stops.get(index);

            int expectedSeq = index + 1;

            if (!Objects.equals(
                    item.stopSeq(),
                    expectedSeq
            )) {
                throw new BusinessException(
                        ErrorCode.TRAIN_STOP_SEQ_INVALID
                );
            }

            Station station =
                    stationMapper.selectById(item.stationId());

            if (station == null
                    || !"ACTIVE".equals(station.getStatus())) {
                throw new BusinessException(
                        ErrorCode.STATION_NOT_FOUND,
                        "站序" + item.stopSeq()
                                + "对应车站不存在或已停用"
                );
            }

            if (!stationIds.add(item.stationId())) {
                throw new BusinessException(
                        ErrorCode.TRAIN_STOP_INVALID,
                        "同一车次不能重复经过同一车站"
                );
            }

            boolean first = index == 0;
            boolean last = index == stops.size() - 1;

            if (first && item.arrivalTime() != null) {
                throw new BusinessException(
                        ErrorCode.TRAIN_STOP_TIME_INVALID,
                        "始发站到达时间必须为空"
                );
            }

            if (first && item.departureTime() == null) {
                throw new BusinessException(
                        ErrorCode.TRAIN_STOP_TIME_INVALID,
                        "始发站发车时间不能为空"
                );
            }

            if (last && item.departureTime() != null) {
                throw new BusinessException(
                        ErrorCode.TRAIN_STOP_TIME_INVALID,
                        "终到站发车时间必须为空"
                );
            }

            if (last && item.arrivalTime() == null) {
                throw new BusinessException(
                        ErrorCode.TRAIN_STOP_TIME_INVALID,
                        "终到站到达时间不能为空"
                );
            }

            if (!first
                    && !last
                    && (item.arrivalTime() == null
                    || item.departureTime() == null)) {
                throw new BusinessException(
                        ErrorCode.TRAIN_STOP_TIME_INVALID,
                        "中间站到达和发车时间不能为空"
                );
            }

            LocalDateTime arrival = toDateTime(
                    item.arrivalTime(),
                    item.arrivalDayOffset()
            );

            LocalDateTime departure = toDateTime(
                    item.departureTime(),
                    item.departureDayOffset()
            );

            if (arrival != null
                    && previousTime != null
                    && arrival.isBefore(previousTime)) {
                throw new BusinessException(
                        ErrorCode.TRAIN_STOP_TIME_INVALID,
                        "到达时间不能早于上一站时刻"
                );
            }

            if (arrival != null
                    && departure != null
                    && departure.isBefore(arrival)) {
                throw new BusinessException(
                        ErrorCode.TRAIN_STOP_TIME_INVALID,
                        "发车时间不能早于到达时间"
                );
            }

            previousTime =
                    departure != null ? departure : arrival;

            if (previousDistance != null
                    && item.distanceKm()
                    < previousDistance) {
                throw new BusinessException(
                        ErrorCode.TRAIN_STOP_INVALID,
                        "里程必须随站序递增"
                );
            }

            previousDistance = item.distanceKm();
        }
    }

    private LocalDateTime toDateTime(
            LocalTime time,
            Integer dayOffset
    ) {
        if (time == null) {
            return null;
        }

        return LocalDateTime.of(
                BASE_DATE.plusDays(dayOffset),
                time
        );
    }

    private Train requireTrain(Long trainId) {
        Train train = trainMapper.selectById(trainId);

        if (train == null) {
            throw new BusinessException(
                    ErrorCode.TRAIN_NOT_FOUND
            );
        }

        return train;
    }

    private void checkTrainNoUnique(
            String trainNo,
            Long excludeId
    ) {
        String normalized = trainNo.trim()
                .toUpperCase(Locale.ROOT);

        Long count = trainMapper.selectCount(
                Wrappers.<Train>lambdaQuery()
                        .eq(Train::getTrainNo, normalized)
                        .ne(
                                excludeId != null,
                                Train::getId,
                                excludeId
                        )
        );

        if (count > 0) {
            throw new BusinessException(
                    ErrorCode.TRAIN_NO_EXISTS
            );
        }
    }

    private void ensureStructureEditable(Long trainId) {
        Long count = trainRunMapper.selectCount(
                Wrappers.<TrainRun>lambdaQuery()
                        .eq(TrainRun::getTrainId, trainId)
                        .and(wrapper ->
                                wrapper.eq(
                                                TrainRun::getInventoryInitialized,
                                                true
                                        )
                                        .or()
                                        .eq(
                                                TrainRun::getSaleStatus,
                                                "ON_SALE"
                                        )
                        )
        );

        if (count > 0) {
            throw new BusinessException(
                    ErrorCode.TRAIN_STRUCTURE_IN_USE
            );
        }
    }
}