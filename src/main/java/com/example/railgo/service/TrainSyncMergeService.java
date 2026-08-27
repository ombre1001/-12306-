package com.example.railgo.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.railgo.data.po.Station;
import com.example.railgo.config.TrainSyncProperties;
import com.example.railgo.data.po.Train;
import com.example.railgo.data.po.TrainRun;
import com.example.railgo.data.po.TrainStop;
import com.example.railgo.mapper.StationMapper;
import com.example.railgo.mapper.TrainMapper;
import com.example.railgo.mapper.TrainRunMapper;
import com.example.railgo.mapper.TrainStopMapper;
import com.example.railgo.service.source.SourceStop;
import com.example.railgo.service.source.SourceTrain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainSyncMergeService {

    private final TrainMapper trainMapper;
    private final TrainStopMapper trainStopMapper;
    private final TrainRunMapper trainRunMapper;
    private final StationMapper stationMapper;
    private final StationAutoRepairService stationAutoRepairService;
    private final TrainSyncProperties properties;

    @Transactional
    public MergeResult merge(
            LocalDate date,
            SourceTrain source,
            List<SourceStop> sourceStops
    ) {
        if (sourceStops.size() < 2) {
            throw new IllegalArgumentException(
                    source.trainNo() + "经停站少于2个"
            );
        }

        LocalDateTime now = LocalDateTime.now();
        Train train = trainMapper.selectOne(
                Wrappers.<Train>lambdaQuery()
                        .eq(Train::getTrainNo, source.trainNo())
        );
        boolean created = train == null;
        if (created) {
            train = new Train();
            train.setTrainNo(source.trainNo());
            train.setStatus("ACTIVE");
            train.setCreatedAt(now);
        }

        train.setSourceTrainCode(source.sourceTrainCode());
        train.setTrainType(source.trainType());
        train.setOriginStationId(
                stationAutoRepairService.resolveEndpoint(
                        source.fromStationCode(),
                        sourceStops.getFirst().stationName()
                ).getId()
        );
        train.setDestinationStationId(
                stationAutoRepairService.resolveEndpoint(
                        source.toStationCode(),
                        sourceStops.getLast().stationName()
                ).getId()
        );
        train.setUpdatedAt(now);

        if (created) {
            trainMapper.insert(train);
        } else {
            trainMapper.updateById(train);
        }

        List<TrainStop> oldStops = trainStopMapper.selectList(
                Wrappers.<TrainStop>lambdaQuery()
                        .eq(TrainStop::getTrainId, train.getId())
                        .orderByAsc(TrainStop::getStopSeq)
        );
        String newHash = scheduleHash(sourceStops);
        boolean scheduleChanged = !newHash.equals(scheduleHash(oldStops));

        if (scheduleChanged) {
            Long initializedRuns = trainRunMapper.selectCount(
                    Wrappers.<TrainRun>lambdaQuery()
                            .eq(TrainRun::getTrainId, train.getId())
                            .eq(TrainRun::getInventoryInitialized, true)
            );
            if (initializedRuns > 0 && !oldStops.isEmpty()) {
                throw new IllegalStateException(
                        source.trainNo()
                                + "时刻表发生变化，但已有运行实例初始化库存；请人工处理"
                );
            }

            trainStopMapper.delete(
                    Wrappers.<TrainStop>lambdaQuery()
                            .eq(TrainStop::getTrainId, train.getId())
            );
            for (SourceStop sourceStop : sourceStops) {
                TrainStop stop = new TrainStop();
                stop.setTrainId(train.getId());
                stop.setStationId(
                        stationAutoRepairService.resolveStop(
                                sourceStop.stationName()
                        ).getId()
                );
                stop.setStopSeq(sourceStop.stopSeq());
                stop.setArrivalTime(sourceStop.arrivalTime());
                stop.setArrivalDayOffset(sourceStop.arrivalDayOffset());
                stop.setDepartureTime(sourceStop.departureTime());
                stop.setDepartureDayOffset(sourceStop.departureDayOffset());
                stop.setDistanceKm(0);
                trainStopMapper.insert(stop);
            }
        }

        TrainRun run = trainRunMapper.selectOne(
                Wrappers.<TrainRun>lambdaQuery()
                        .eq(TrainRun::getTrainId, train.getId())
                        .eq(TrainRun::getRunDate, date)
        );
        if (run == null) {
            run = new TrainRun();
            run.setTrainId(train.getId());
            run.setRunDate(date);
            run.setSaleStatus("NOT_ON_SALE");
            run.setInventoryInitialized(false);
            run.setCreatedAt(now);
        }
        run.setSourceManaged(true);
        run.setSourceStatus("CONFIRMED");
        run.setSourceCheckedAt(now);
        run.setSourceLastSeenAt(now);
        run.setSourceValidUntil(now.plus(properties.getFreshness()));
        run.setSourceHash(newHash);
        run.setUpdatedAt(now);

        if (run.getId() == null) {
            trainRunMapper.insert(run);
        } else {
            trainRunMapper.updateById(run);
        }

        return new MergeResult(scheduleChanged, sourceStops.size());
    }

    private String normalizeStationName(String name) {
        String value = name == null
                ? ""
                : name.trim()
                .replace("（", "(")
                .replace("）", ")")
                .replaceAll("\\s+", "");

        for (String suffix : List.of(
                "(高铁)", "(客运)", "(城际)", "(动车)"
        )) {
            if (value.endsWith(suffix)) {
                value = value.substring(
                        0, value.length() - suffix.length()
                );
                break;
            }
        }

        return value.endsWith("站")
                ? value.substring(0, value.length() - 1)
                : value;
    }

    private String scheduleHash(List<?> stops) {
        StringBuilder content = new StringBuilder();
        for (Object item : stops) {
            if (item instanceof SourceStop stop) {
                content.append(stop.stopSeq()).append('|')
                        .append(normalizeStationName(stop.stationName())).append('|')
                        .append(stop.arrivalTime()).append('|')
                        .append(stop.arrivalDayOffset()).append('|')
                        .append(stop.departureTime()).append('|')
                        .append(stop.departureDayOffset()).append(';');
            } else if (item instanceof TrainStop stop) {
                Station station = stationMapper.selectById(stop.getStationId());
                content.append(stop.getStopSeq()).append('|')
                        .append(station == null
                                ? stop.getStationId()
                                : normalizeStationName(station.getName()))
                        .append('|').append(stop.getArrivalTime())
                        .append('|').append(stop.getArrivalDayOffset())
                        .append('|').append(stop.getDepartureTime())
                        .append('|').append(stop.getDepartureDayOffset())
                        .append(';');
            }
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(content.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("计算时刻表摘要失败", exception);
        }
    }

    public record MergeResult(boolean scheduleChanged, int stopCount) {
    }
}
