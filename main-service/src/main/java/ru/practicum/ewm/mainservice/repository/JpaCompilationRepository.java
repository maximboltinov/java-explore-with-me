package ru.practicum.ewm.mainservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.mainservice.model.Compilation;

public interface JpaCompilationRepository extends JpaRepository<Compilation, Long> {
}
