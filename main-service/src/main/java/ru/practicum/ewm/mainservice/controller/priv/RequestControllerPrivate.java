package ru.practicum.ewm.mainservice.controller.priv;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.mainservice.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.mainservice.service.RequestService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/requests")
@AllArgsConstructor
@Slf4j
@Validated
public class RequestControllerPrivate {
    private final RequestService requestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto create(@PathVariable Long userId,
                                              @RequestParam Long eventId) {
        log.info("Запрос POST /users/{}/requests", userId);
        ParticipationRequestDto result = requestService.create(userId, eventId);
        log.info("Ответ POST /users/{}/requests {}", userId, result);
        return result;
    }

    @GetMapping
    public List<ParticipationRequestDto> getRequests(@PathVariable Long userId) {
        log.info("Запрос GET /users/{}/requests", userId);
        List<ParticipationRequestDto> result = requestService.getRequests(userId);
        log.info("Ответ GET /users/{}/requests {}", userId, result);
        return result;
    }

    @PatchMapping("/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable Long userId, @PathVariable Long requestId) {
        log.info("Запрос PATCH /users/{}/requests/{}/cancel", userId, requestId);
        ParticipationRequestDto result = requestService.cancelRequest(userId, requestId);
        log.info("Ответ PATCH /users/{}/requests/{}/cancel {}", userId, requestId, result);
        return result;
    }
}
