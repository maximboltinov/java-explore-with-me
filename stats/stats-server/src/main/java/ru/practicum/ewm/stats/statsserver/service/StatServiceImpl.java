package ru.practicum.ewm.stats.statsserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.statsdto.EndpointHit;
import ru.practicum.ewm.stats.statsdto.ViewStats;
import ru.practicum.ewm.stats.statsserver.mapper.StatMapper;
import ru.practicum.ewm.stats.statsserver.repository.JpaStatRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StatServiceImpl implements StatService {
    private final JpaStatRepository jpaStatRepository;

    @Override
    public void create(EndpointHit endpointHit) {
        jpaStatRepository.save(StatMapper.endpointHitToStatMapper(endpointHit));
    }

    @Override
    public List<ViewStats> getStats(String start, String end, Optional<String[]> uris, boolean unique) {
        LocalDateTime decodedStart;
        LocalDateTime decodedEnd;
        try {
            decodedStart = StatMapper.encodedStingToLocalDateTime(start);
            decodedEnd = StatMapper.encodedStingToLocalDateTime(end);
        } catch (IllegalArgumentException | DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("неверный формат даты");
        }

        if (uris.isPresent()) {
            if (unique) {
                return jpaStatRepository.getStatsByUriAndUniqueIp(
                        decodedStart, decodedEnd, uris.get()).orElse(List.of());
            } else {
                return jpaStatRepository.getStatsByUriAndNotUniqueIp(
                        decodedStart, decodedEnd, uris.get()).orElse(List.of());
            }
        } else {
            if (unique) {
                return jpaStatRepository.getStatsByWithoutUriAndUniqueIp(
                        decodedStart, decodedEnd).orElse(List.of());
            } else {
                return jpaStatRepository.getStatsByWithoutUriAndNotUniqueIp(
                        decodedStart, decodedEnd).orElse(List.of());
            }
        }
    }
}
