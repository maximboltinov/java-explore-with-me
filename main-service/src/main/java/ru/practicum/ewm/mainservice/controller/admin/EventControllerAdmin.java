package ru.practicum.ewm.mainservice.controller.admin;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.mainservice.dto.event.EventFullDto;
import ru.practicum.ewm.mainservice.dto.event.EventRequestParamsAdmin;
import ru.practicum.ewm.mainservice.dto.event.UpdateEventAdminRequest;
import ru.practicum.ewm.mainservice.service.EventService;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.util.List;

@RestController
@RequestMapping("/admin/events")
@AllArgsConstructor
@Slf4j
@Validated
public class EventControllerAdmin {
    private final EventService eventService;

    @GetMapping
    public List<EventFullDto> getEventsAdmin(EventRequestParamsAdmin requestParamsAdmin,
                                             @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                             @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("Запрос GET /admin/events from {} size {} {}", from, size, requestParamsAdmin);
        List<EventFullDto> result = eventService.getEventsAdmin(requestParamsAdmin, from, size);
        log.info("Ответ GET /admin/events from {} size {} {}", from, size, result);
        return result;
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEventAdmin(@PathVariable Long eventId,
                                    @RequestBody @Valid UpdateEventAdminRequest updateEventAdminRequest) {
        log.info("Запрос PATCH /admin/events/{}", eventId);
        EventFullDto result = eventService.updateEventAdmin(eventId, updateEventAdminRequest);
        log.info("Ответ PATCH /admin/events/{} {}", eventId, result);
        return result;
    }
}
