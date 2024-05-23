package ru.practicum.ewm.mainservice.service;

import ru.practicum.ewm.mainservice.dto.location.LocationDto;
import ru.practicum.ewm.mainservice.model.Location;

public interface LocationService {
    Location prepareLocation(LocationDto locationDto);
}
