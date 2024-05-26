package ru.practicum.ewm.mainservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.mainservice.enums.RequestStatus;
import ru.practicum.ewm.mainservice.model.ParticipationRequest;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface JpaRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    List<ParticipationRequest> findAllByEventId(Long eventId);

    List<ParticipationRequest> findAllByEventIdAndStatusAndIdInOrderById(Long eventId, RequestStatus status, Set<Long> ids);

    int countByEventIdAndStatus(Long eventId, RequestStatus requestStatus);

    Optional<ParticipationRequest> findByRequesterIdAndEventId(Long userId, Long eventId);

    List<ParticipationRequest> findAllByRequesterId(Long id);
}
