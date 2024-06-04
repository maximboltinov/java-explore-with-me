package ru.practicum.ewm.mainservice.service.implementation;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.mainservice.dto.location.LocationDto;
import ru.practicum.ewm.mainservice.model.Location;
import ru.practicum.ewm.mainservice.repository.JpaLocationRepository;
import ru.practicum.ewm.mainservice.service.LocationService;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceImplTest {
    private LocationService locationService;

    @Mock
    private JpaLocationRepository jpaLocationRepository;

    @BeforeEach
    public void setUp() {
        locationService = new LocationServiceImpl(jpaLocationRepository);
    }

    private final EasyRandom generator = new EasyRandom();

    @Test
    void prepareLocation_isPresent() {
        LocationDto locationDto = generator.nextObject(LocationDto.class);
        Location location = Location.builder().id(1L).lat(locationDto.getLat()).lon(locationDto.getLon()).build();

        when(jpaLocationRepository.getByLatAndLon(locationDto.getLat(), locationDto.getLon()))
                .thenReturn(Optional.of(location));

        locationService.prepareLocation(locationDto);

        verify(jpaLocationRepository, never()).save(location);
    }

    @Test
    void prepareLocation_isNotPresent() {
        LocationDto locationDto = generator.nextObject(LocationDto.class);

        when(jpaLocationRepository.getByLatAndLon(locationDto.getLat(), locationDto.getLon()))
                .thenReturn(Optional.empty());

        locationService.prepareLocation(locationDto);

        verify(jpaLocationRepository).save(any(Location.class));
    }
}