package com.example.railgo.service.source;

/** 12306 station_name.js 中的一条车站主数据。 */
public record SourceStation(
        String stationCode,
        String stationName,
        String pinyin,
        String pinyinInitial
) {
}
