package ru.practicum.ewm.stats.statsserver.service;

import ru.practicum.ewm.stats.statsdto.EndpointHit;
import ru.practicum.ewm.stats.statsdto.ViewStats;

import java.util.List;
import java.util.Optional;

public interface StatService {
    void create(EndpointHit endpointHit);

    List<ViewStats> getStats(String start, String end, Optional<String[]> uris, boolean unique);
}
