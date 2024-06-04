package ru.practicum.ewm.mainservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.mainservice.enums.RequestStatus;
import ru.practicum.ewm.mainservice.model.ParticipationRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface JpaRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    List<ParticipationRequest> findAllByEventId(Long eventId);

    List<ParticipationRequest> findAllByEventIdAndStatusAndIdInOrderById(Long eventId, RequestStatus status, Set<Long> ids);

    List<ParticipationRequest> findAllByEventIdAndIdInOrderById(Long eventId, Set<Long> ids);

    Optional<ParticipationRequest> findByRequesterIdAndEventId(Long userId, Long eventId);

    List<ParticipationRequest> findAllByRequesterId(Long id);

    int countByEventIdAndStatus(Long eventId, RequestStatus requestStatus);

    @Query(value = "select pr.event.id, count(pr.id) from ParticipationRequest pr where pr.event.id in ?1 and pr.status = ?2 group by pr.event.id")
    Map<Long, Integer> countByEventsIdAndStatus(List<Long> eventIds, RequestStatus status);
}
