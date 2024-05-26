package ru.practicum.ewm.mainservice.controller.pub;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.mainservice.dto.event.EventFullDto;
import ru.practicum.ewm.mainservice.dto.event.EventRequestParams;
import ru.practicum.ewm.mainservice.dto.event.EventShortDto;
import ru.practicum.ewm.mainservice.service.EventService;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.util.List;

@RestController
@RequestMapping("/events")
@AllArgsConstructor
@Slf4j
@Validated
public class EventControllerPublic {
    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> getEvents(EventRequestParams eventRequestParams,
                                         @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                         @RequestParam(defaultValue = "10") @Positive Integer size,
                                         HttpServletRequest request) {
        log.info("Запрос GET /events параметры from {} size {} {}", from, size, eventRequestParams);
        List<EventShortDto> result = eventService.getFilteredEvents(eventRequestParams, from, size, request);
        log.info("Ответ GET /events {}", result);
        return result;
    }

    @GetMapping("/{eventId}")
    public EventFullDto getEventById(@PathVariable(value = "eventId") @Positive Long eventId,
                                     HttpServletRequest request) {
        log.info("Запрос GET /events/{}", eventId);
        EventFullDto result = eventService.getEventById(eventId, request);
        log.info("Ответ GET /events/{} {}", eventId, result);
        return result;
    }
}
