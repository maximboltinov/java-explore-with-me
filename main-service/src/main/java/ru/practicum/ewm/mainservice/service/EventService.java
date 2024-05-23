package ru.practicum.ewm.mainservice.service;

import ru.practicum.ewm.mainservice.dto.event.EventFullDto;
import ru.practicum.ewm.mainservice.dto.event.EventShortDto;
import ru.practicum.ewm.mainservice.dto.event.NewEventDto;
import ru.practicum.ewm.mainservice.dto.event.UpdateEventUserRequest;
import ru.practicum.ewm.mainservice.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.mainservice.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.ewm.mainservice.dto.request.ParticipationRequestDto;

import java.util.List;

public interface EventService {
    EventFullDto createEvent(Long userId, NewEventDto newEventDto);

    EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest);

    List<EventShortDto> getEventsByUserId(Long userId, Integer from, Integer size);

    EventFullDto getEvent(Long userId, Long eventId);

    List<ParticipationRequestDto> getRequestByEventAndOwner(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateStatusRequest(Long userId, Long eventId, EventRequestStatusUpdateRequest inputUpdate);
}
