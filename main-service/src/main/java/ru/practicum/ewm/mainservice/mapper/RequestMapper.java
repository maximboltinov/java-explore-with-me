package ru.practicum.ewm.mainservice.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.mainservice.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.mainservice.model.ParticipationRequest;

@UtilityClass
public class RequestMapper {
    public ParticipationRequestDto participationRequestToDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .event(request.getEvent().getId())
                .created(request.getCreated())
                .requester(request.getId())
                .status(request.getStatus())
                .build();
    }
}
