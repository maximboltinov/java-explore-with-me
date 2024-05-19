package ru.practicum.ewm.mainservice.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.mainservice.model.User;

import java.util.List;

public interface JpaUserRepository extends JpaRepository<User, Long> {
    List<User> getByIdIn(List<Long> ids, Pageable pageable);
}
