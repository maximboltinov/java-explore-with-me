package ru.practicum.ewm.stats.statsserver.service;

import ru.practicum.ewm.stats.statsdto.EndpointHit;
import ru.practicum.ewm.stats.statsdto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

public interface StatService {
    void create(EndpointHit endpointHit);

    List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique);
}
