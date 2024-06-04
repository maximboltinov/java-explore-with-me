package ru.practicum.ewm.mainservice.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.mainservice.dto.location.LocationDto;
import ru.practicum.ewm.mainservice.model.Location;

@UtilityClass
public class LocationMapper {
    public Location locationDtoToLocation(LocationDto locationDto) {
        return Location.builder()
                .lat(locationDto.getLat())
                .lon(locationDto.getLon())
                .build();
    }

    public LocationDto locationToLocationDto(Location location) {
        return LocationDto.builder()
                .lat(location.getLat())
                .lon(location.getLon())
                .build();
    }
}
