package ru.practicum.ewm.mainservice.controller.priv;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.mainservice.dto.event.EventFullDto;
import ru.practicum.ewm.mainservice.dto.event.EventShortDto;
import ru.practicum.ewm.mainservice.dto.event.NewEventDto;
import ru.practicum.ewm.mainservice.dto.event.UpdateEventUserRequest;
import ru.practicum.ewm.mainservice.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.mainservice.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.ewm.mainservice.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.mainservice.service.EventService;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events")
@AllArgsConstructor
@Slf4j
@Validated
public class EventControllerPrivate {
    private final EventService eventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto createEvent(@PathVariable @Positive Long userId,
                                    @RequestBody @Valid NewEventDto newEventDto) {
        log.info("Запрос POST /users/{}/events {}", userId, newEventDto);
        EventFullDto eventFullDto = eventService.createEvent(userId, newEventDto);
        log.info("Ответ POST /users/{}/events значение {}", userId, eventFullDto);
        return eventFullDto;
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEvent(@PathVariable @Positive Long userId,
                                    @PathVariable @Positive Long eventId,
                                    @RequestBody @Valid UpdateEventUserRequest updateEventUserRequest) {
        log.info("Запрос PATCH /users/{}/events/{} {}", userId, eventId, updateEventUserRequest);
        EventFullDto eventFullDto = eventService.updateEvent(userId, eventId, updateEventUserRequest);
        log.info("Ответ PATCH /users/{}/events/{} значение {}", userId, eventId, eventFullDto);
        return eventFullDto;
    }


    @GetMapping
    public List<EventShortDto> getEventsByUserId(@PathVariable @Positive Long userId,
                                                 @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                                 @RequestParam(defaultValue = "10") @Positive Integer size) {
        log.info("Запрос GET /users/{}/events?from={}&size={}", userId, from, size);
        List<EventShortDto> eventShortDtoList = eventService.getEventsByUserId(userId, from, size);
        log.info("Ответ GET /users/{}/events?from={}&size={}", userId, from, size);
        return eventShortDtoList;
    }

    @GetMapping("/{eventId}")
    public EventFullDto getEvent(@PathVariable @Positive Long userId,
                                 @PathVariable @Positive Long eventId) {
        log.info("Запрос GET /users/{}/events/{}", userId, eventId);
        EventFullDto eventFullDto = eventService.getEvent(userId, eventId);
        log.info("Ответ GET /users/{}/events/{} {}", userId, eventId, eventFullDto);
        return eventFullDto;
    }

    @GetMapping("/{eventId}/requests")
    public List<ParticipationRequestDto> getRequestByEventAndOwner(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long eventId) {
        log.info("Запрос GET /users/{}/events/{}/requests", userId, eventId);
        List<ParticipationRequestDto> list = eventService.getRequestByEventAndOwner(userId, eventId);
        log.info("Ответ GET /users/{}/events/{}/requests {}", userId, eventId, list);
        return list;
    }

    @PatchMapping("/{eventId}/requests")
    public EventRequestStatusUpdateResult updateStatusRequestFromOwner(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long eventId,
            @RequestBody EventRequestStatusUpdateRequest inputUpdate) {
        log.info("Запрос PATCH /users/{}/events/{}/requests {}", userId, eventId, inputUpdate);
        EventRequestStatusUpdateResult result = eventService.updateStatusRequest(userId, eventId, inputUpdate);
        log.info("Ответ PATCH /users/{}/events/{}/requests {}", userId, eventId, result);
        return result;
    }
}
