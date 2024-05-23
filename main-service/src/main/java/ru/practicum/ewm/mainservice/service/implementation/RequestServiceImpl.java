package ru.practicum.ewm.mainservice.service.implementation;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.mainservice.enums.RequestStatus;
import ru.practicum.ewm.mainservice.model.ParticipationRequest;
import ru.practicum.ewm.mainservice.repository.JpaRequestRepository;
import ru.practicum.ewm.mainservice.service.RequestService;

import java.util.List;
import java.util.Set;

@Service
@AllArgsConstructor
public class RequestServiceImpl implements RequestService {
    private final JpaRequestRepository jpaRequestRepository;

    @Override
    public List<ParticipationRequest> getRequestsByEventId(Long eventId) {
        return jpaRequestRepository.findAllByEventId(eventId);
    }

    @Override
    public List<ParticipationRequest> getRequestsByEventIdAndIdsAndStatus(
            Long eventId, Set<Long> requestIds, RequestStatus status) {
        return jpaRequestRepository.findAllByEventIdAndStatusAndIdInOrderById(eventId, status, requestIds);
    }

    @Override
    public int countByEventIdAndStatus(Long eventId, RequestStatus requestStatus) {
        return jpaRequestRepository.countByEventIdAndStatus(eventId, requestStatus);
    }

    @Override
    public void saveAll(List<ParticipationRequest> requests) {
        jpaRequestRepository.saveAll(requests);
    }
}
