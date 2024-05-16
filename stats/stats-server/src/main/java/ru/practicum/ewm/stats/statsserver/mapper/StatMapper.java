package ru.practicum.ewm.stats.statsserver.mapper;

import ru.practicum.ewm.stats.statsdto.EndpointHit;
import ru.practicum.ewm.stats.statsserver.model.Stat;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.net.URLDecoder.decode;

public final class StatMapper {
    private StatMapper() {
    }

    public static Stat endpointHitToStatMapper(EndpointHit endpointHit) {
        return Stat.builder()
                .app(endpointHit.getApp())
                .uri(endpointHit.getUri())
                .ip(endpointHit.getIp())
                .timestamp(encodedStingToLocalDateTime(endpointHit.getTimestamp()))
                .build();
    }

    public static String decodedTimestampString(String encodedString) {
        return decode(encodedString, StandardCharsets.UTF_8);
    }

    public static LocalDateTime stringToLocalDateTime(String string) {
        return LocalDateTime.parse(string, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static LocalDateTime encodedStingToLocalDateTime(String encodedString) {
        return stringToLocalDateTime(decodedTimestampString(encodedString));
    }
}
