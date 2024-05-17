package ru.practicum.ewm.stats.statsserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.statsdto.EndpointHit;
import ru.practicum.ewm.stats.statsdto.ViewStats;
import ru.practicum.ewm.stats.statsserver.exception.InvalidTimePeriodException;
import ru.practicum.ewm.stats.statsserver.mapper.StatMapper;
import ru.practicum.ewm.stats.statsserver.repository.JpaStatRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatServiceImpl implements StatService {
    private final JpaStatRepository jpaStatRepository;

    @Override
    public void create(EndpointHit endpointHit) {
        jpaStatRepository.save(StatMapper.endpointHitToStatMapper(endpointHit));
    }

    @Override
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        if (start.isAfter(end) || start.equals(end) || start.isAfter(LocalDateTime.now())) {
            throw new InvalidTimePeriodException("некорректный временной период");
        }

        if (!uris.isEmpty()) {
            if (unique) {
                return jpaStatRepository.getStatsByUriAndUniqueIp(
                        start, end, uris).orElse(List.of());
            } else {
                return jpaStatRepository.getStatsByUriAndNotUniqueIp(
                        start, end, uris).orElse(List.of());
            }
        } else {
            if (unique) {
                return jpaStatRepository.getStatsByWithoutUriAndUniqueIp(
                        start, end).orElse(List.of());
            } else {
                return jpaStatRepository.getStatsByWithoutUriAndNotUniqueIp(
                        start, end).orElse(List.of());
            }
        }
    }
}
