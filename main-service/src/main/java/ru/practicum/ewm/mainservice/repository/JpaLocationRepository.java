package ru.practicum.ewm.mainservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.mainservice.model.Location;

import java.util.Optional;

public interface JpaLocationRepository extends JpaRepository<Location, Long> {
    Optional<Location> getByLatAndLon(Float lat, Float lon);
}
