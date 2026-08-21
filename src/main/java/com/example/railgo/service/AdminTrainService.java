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
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
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
    private final JdbcTemplate jdbcTemplate;

    private int batchInsertSeats(
            Long coachId,
            Integer startRow,
            Integer endRow,
            Collection<String> seatLetters
    ) {
        List<Object[]> batchArgs = new ArrayList<>();

        for (int row = startRow; row <= endRow; row++) {
            for (String rawLetter : seatLetters) {
                String letter = rawLetter
                        .trim()
                        .toUpperCase(Locale.ROOT);

                batchArgs.add(
                        new Object[]{
                                coachId,
                                String.format("%02d%s", row, letter),
                                row,
                                letter,
                                true
                        }
                );
            }
        }

        if (batchArgs.isEmpty()) {
            return 0;
        }

        int[] affectedRows = jdbcTemplate.batchUpdate(
                """
                INSERT INTO train_seat
                    (
                        coach_id,
                        seat_no,
                        row_no,
                        seat_letter,
                        enabled
                    )
                VALUES (?, ?, ?, ?, ?)
                """,
                batchArgs
        );

        int insertedCount = 0;

        for (int affectedRow : affectedRows) {
            if (affectedRow > 0) {
                insertedCount += affectedRow;
            } else if (affectedRow == -2) {
                /*
                 * JDBC SUCCESS_NO_INFO，表示执行成功但驱动没有返回具体行数。
                 */
                insertedCount++;
            }
        }

        return insertedCount;
    }

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

    private SeatType findSeatTypeByCode(String code) {
        SeatType seatType = seatTypeMapper.selectOne(
                Wrappers.<SeatType>lambdaQuery()
                        .eq(SeatType::getCode, code)
                        .last("LIMIT 1")
        );

        if (seatType == null) {
            throw new BusinessException(
                    ErrorCode.SEAT_TYPE_NOT_FOUND,
                    "席别不存在：" + code
            );
        }

        return seatType;
    }

    private LinkedHashSet<String> normalizeSeatLetters(
            List<String> seatLetters
    ) {
        LinkedHashSet<String> letters = seatLetters.stream()
                .map(String::trim)
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (letters.size() != seatLetters.size()) {
            throw new BusinessException(
                    ErrorCode.SEAT_TEMPLATE_INVALID,
                    "座位字母不能重复"
            );
        }

        return letters;
    }

    /**
     * 根据座位排列自动判断席别。
     *
     * 一等座：A、C、D、F
     * 二等座：A、B、C、D、F
     */
    private SeatType resolveSeatType(
            String coachNo,
            Set<String> letters,
            SeatType firstClassSeatType,
            SeatType secondClassSeatType
    ) {
        Set<String> firstClassLetters =
                Set.of("A", "C", "D", "F");

        Set<String> secondClassLetters =
                Set.of("A", "B", "C", "D", "F");

        if (letters.equals(firstClassLetters)) {
            return firstClassSeatType;
        }

        if (letters.equals(secondClassLetters)) {
            return secondClassSeatType;
        }

        throw new BusinessException(
                ErrorCode.SEAT_TEMPLATE_INVALID,
                "车厢" + coachNo
                        + "的座位排列无法识别席别，"
                        + "一等座应为[A,C,D,F]，"
                        + "二等座应为[A,B,C,D,F]"
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminAllTrainSeatInitResult initializeAllTrainSeats(
            AdminAllTrainSeatInitRequest request
    ) {
        boolean overwrite = Boolean.TRUE.equals(request.overwrite());

        /*
         * 当前爬虫同步逻辑会写入 source_train_code。
         * 因此这里只查询爬虫同步车次，不处理管理员手动创建的车次。
         */
        List<Train> trains = trainMapper.selectList(
                Wrappers.<Train>lambdaQuery()
                        .isNotNull(Train::getSourceTrainCode)
                        .ne(Train::getSourceTrainCode, "")
                        .orderByAsc(Train::getId)

        );

        if (trains.isEmpty()) {
            throw new IllegalStateException(
                    "没有查询到爬虫同步车次，请检查train.source_train_code是否有值"
            );
        }

        SeatType firstClassSeatType = findSeatTypeByCode("FIRST");
        SeatType secondClassSeatType = findSeatTypeByCode("SECOND");

        /*
         * 提前验证所有模板，防止处理中途因为模板错误而整体回滚。
         */
        Set<String> coachNoSet = new HashSet<>();

        for (AdminAllTrainSeatInitRequest.CoachTemplate template
                : request.templates()) {

            String coachNo = template.coachNo().trim();

            if (!coachNoSet.add(coachNo)) {
                throw new BusinessException(
                        ErrorCode.COACH_DUPLICATED,
                        "车厢号重复：" + coachNo
                );
            }

            if (template.startRow() > template.endRow()) {
                throw new BusinessException(
                        ErrorCode.SEAT_TEMPLATE_INVALID,
                        "车厢" + coachNo + "的起始排不能大于结束排"
                );
            }

            LinkedHashSet<String> letters =
                    normalizeSeatLetters(template.seatLetters());

            resolveSeatType(
                    coachNo,
                    letters,
                    firstClassSeatType,
                    secondClassSeatType
            );
        }

        int initializedTrainCount = 0;
        int existingStructureSkippedCount = 0;
        int inventoryLockedSkippedCount = 0;
        int generatedCoachCount = 0;
        int generatedSeatCount = 0;

        List<String> inventoryLockedTrainNos = new ArrayList<>();

        for (Train train : trains) {
            Long trainId = train.getId();
            log.info(
                    "开始初始化车厢座位：trainId={}, trainNo={}, 当前进度={}/{}",
                    trainId,
                    train.getTrainNo(),
                    initializedTrainCount + 1,
                    trains.size()
            );

            /*
             * 已开售或已初始化库存的车次不能修改车厢、座位结构。
             * 以前这里直接抛异常，导致整个事务回滚。
             * 现在改为记录并跳过。
             */
            if (!isStructureEditable(trainId)) {
                inventoryLockedSkippedCount++;
                inventoryLockedTrainNos.add(train.getTrainNo());
                continue;
            }

            List<TrainCoach> oldCoaches =
                    trainCoachMapper.selectList(
                            Wrappers.<TrainCoach>lambdaQuery()
                                    .eq(TrainCoach::getTrainId, trainId)
                                    .orderByAsc(TrainCoach::getId)
                    );

            /*
             * overwrite=false 时，已有车厢结构直接跳过。
             */
            if (!oldCoaches.isEmpty() && !overwrite) {
                existingStructureSkippedCount++;
                continue;
            }

            /*
             * overwrite=true 时，先删除旧座位，再删除旧车厢。
             */
            if (!oldCoaches.isEmpty()) {
                List<Long> oldCoachIds = oldCoaches.stream()
                        .map(TrainCoach::getId)
                        .filter(Objects::nonNull)
                        .toList();

                if (!oldCoachIds.isEmpty()) {
                    trainSeatMapper.delete(
                            Wrappers.<TrainSeat>lambdaQuery()
                                    .in(
                                            TrainSeat::getCoachId,
                                            oldCoachIds
                                    )
                    );
                }

                trainCoachMapper.delete(
                        Wrappers.<TrainCoach>lambdaQuery()
                                .eq(TrainCoach::getTrainId, trainId)
                );
            }

            for (AdminAllTrainSeatInitRequest.CoachTemplate template
                    : request.templates()) {

                String coachNo = template.coachNo().trim();

                LinkedHashSet<String> letters =
                        normalizeSeatLetters(template.seatLetters());

                SeatType seatType = resolveSeatType(
                        coachNo,
                        letters,
                        firstClassSeatType,
                        secondClassSeatType
                );

                int capacity =
                        (template.endRow()
                                - template.startRow()
                                + 1)
                                * letters.size();

                TrainCoach coach = new TrainCoach();
                coach.setTrainId(trainId);
                coach.setCoachNo(coachNo);
                coach.setSeatTypeId(seatType.getId());
                coach.setCapacity(capacity);

                int coachAffectedRows = trainCoachMapper.insert(coach);

                if (coachAffectedRows != 1 || coach.getId() == null) {
                    throw new IllegalStateException(
                            "车厢保存失败，trainId="
                                    + trainId
                                    + "，trainNo="
                                    + train.getTrainNo()
                                    + "，coachNo="
                                    + coachNo
                    );
                }

                generatedCoachCount++;

                int expectedSeatCount =
                        (template.endRow()
                                - template.startRow()
                                + 1)
                                * letters.size();

                int actualSeatCount = batchInsertSeats(
                        coach.getId(),
                        template.startRow(),
                        template.endRow(),
                        letters
                );

                if (actualSeatCount != expectedSeatCount) {
                    throw new IllegalStateException(
                            "座位批量保存数量异常，trainId="
                                    + trainId
                                    + "，trainNo="
                                    + train.getTrainNo()
                                    + "，coachNo="
                                    + coachNo
                                    + "，预计生成="
                                    + expectedSeatCount
                                    + "，实际生成="
                                    + actualSeatCount
                    );
                }

                generatedSeatCount += actualSeatCount;
            }

            initializedTrainCount++;
            log.info(
                    "车厢座位初始化完成：trainId={}, trainNo={}, 已生成车厢总数={}, 已生成座位总数={}",
                    trainId,
                    train.getTrainNo(),
                    generatedCoachCount,
                    generatedSeatCount
            );
        }

        return new AdminAllTrainSeatInitResult(
                trains.size(),
                initializedTrainCount,
                existingStructureSkippedCount,
                inventoryLockedSkippedCount,
                generatedCoachCount,
                generatedSeatCount,
                inventoryLockedTrainNos
        );
    }


    private boolean isStructureEditable(Long trainId) {
        Long lockedRunCount = trainRunMapper.selectCount(
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

        return lockedRunCount == null || lockedRunCount == 0;
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