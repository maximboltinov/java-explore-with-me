package ru.practicum.ewm.mainservice.service;

import ru.practicum.ewm.mainservice.enums.RequestStatus;
import ru.practicum.ewm.mainservice.model.ParticipationRequest;

import java.util.List;
import java.util.Set;

public interface RequestService {
    List<ParticipationRequest> getRequestsByEventId(Long eventId);

    List<ParticipationRequest> getRequestsByEventIdAndIdsAndStatus(
            Long eventId, Set<Long> requestIds, RequestStatus status);

    int countByEventIdAndStatus(Long eventId, RequestStatus requestStatus);

    void saveAll(List<ParticipationRequest> requests);
}
