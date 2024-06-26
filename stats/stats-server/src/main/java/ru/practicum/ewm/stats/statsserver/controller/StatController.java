package ru.practicum.ewm.stats.statsserver.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.stats.statsdto.EndpointHit;
import ru.practicum.ewm.stats.statsdto.ViewStats;
import ru.practicum.ewm.stats.statsserver.service.StatService;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class StatController {
    private final StatService statService;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@RequestBody @Valid EndpointHit endpointHit) {
        log.info("Запрос POST /hit body {}", endpointHit);
        statService.create(endpointHit);
        log.info("Ответ POST /hit status {}", HttpStatus.CREATED);
    }

    @GetMapping("/stats")
    public List<ViewStats> getStats(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
                                    @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
                                    @RequestParam(defaultValue = "") List<String> uris,
                                    @RequestParam(defaultValue = "false") boolean unique) {
        log.info("Запрос GET /stats start {}, end {}, uris {}, unique {}", start, end, uris, unique);
        List<ViewStats> viewStatsList = statService.getStats(start, end, uris, unique);
        log.info("Ответ GET /stats body {}", viewStatsList);
        return viewStatsList;
    }
}
