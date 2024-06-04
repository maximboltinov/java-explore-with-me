package ru.practicum.ewm.mainservice.service.implementation;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.mainservice.dto.location.LocationDto;
import ru.practicum.ewm.mainservice.mapper.LocationMapper;
import ru.practicum.ewm.mainservice.model.Location;
import ru.practicum.ewm.mainservice.repository.JpaLocationRepository;
import ru.practicum.ewm.mainservice.service.LocationService;

@Service
@AllArgsConstructor
public class LocationServiceImpl implements LocationService {
    private final JpaLocationRepository jpaLocationRepository;

    @Override
    public Location prepareLocation(LocationDto locationDto) {
        return jpaLocationRepository.getByLatAndLon(locationDto.getLat(), locationDto.getLon())
                .orElse(jpaLocationRepository.save(LocationMapper.locationDtoToLocation(locationDto)));
    }
}
