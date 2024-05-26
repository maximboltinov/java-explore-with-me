package ru.practicum.ewm.mainservice.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.mainservice.dto.event.EventFullDto;
import ru.practicum.ewm.mainservice.dto.event.EventShortDto;
import ru.practicum.ewm.mainservice.dto.event.NewEventDto;
import ru.practicum.ewm.mainservice.enums.EventState;
import ru.practicum.ewm.mainservice.model.Category;
import ru.practicum.ewm.mainservice.model.Event;
import ru.practicum.ewm.mainservice.model.Location;
import ru.practicum.ewm.mainservice.model.User;

import java.time.LocalDateTime;

@UtilityClass
public class EventMapper {
    public Event newEventDtoToEvent(NewEventDto newEventDto, Category category, User initiator,
                                    EventState state, LocalDateTime createdDate, Location location) {
        return Event.builder()
                .id(null)
                .annotation(newEventDto.getAnnotation())
                .description(newEventDto.getDescription())
                .eventDate(newEventDto.getEventDate())
                .location(LocationMapper.locationDtoToLocation(newEventDto.getLocation()))
                .paid(newEventDto.isPaid())
                .participantLimit(newEventDto.getParticipantLimit())
                .requestModeration(newEventDto.isRequestModeration())
                .title(newEventDto.getTitle())
                .category(category)
                .initiator(initiator)
                .state(state)
                .createdDate(createdDate)
                .location(location)
                .build();
    }

    public EventFullDto eventToEventFullDto(Event event) {
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.categoryToCategoryDto(event.getCategory()))
                .createdOn(event.getCreatedDate())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .initiator(UserMapper.userToUserShortDto(event.getInitiator()))
                .location(LocationMapper.locationToLocationDto(event.getLocation()))
                .paid(event.isPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedDate())
                .requestModeration(event.isRequestModeration())
                .state(event.getState())
                .title(event.getTitle())
                .build();
    }

    public EventShortDto eventToEventShortDto(Event event) {
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.categoryToCategoryDto(event.getCategory()))
                .eventDate(event.getEventDate())
                .initiator(UserMapper.userToUserShortDto(event.getInitiator()))
                .paid(event.isPaid())
                .title(event.getTitle())
                .build();
    }
}