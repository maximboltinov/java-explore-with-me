package ru.practicum.ewm.mainservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.practicum.ewm.mainservice.model.Event;

import java.util.Optional;

public interface JpaEventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {
    Optional<Event> findEventByIdAndInitiatorId(Long eventId, Long userId);
}
