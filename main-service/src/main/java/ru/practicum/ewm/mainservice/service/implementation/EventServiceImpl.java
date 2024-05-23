package ru.practicum.ewm.mainservice.service.implementation;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.mainservice.dto.event.EventFullDto;
import ru.practicum.ewm.mainservice.dto.event.EventShortDto;
import ru.practicum.ewm.mainservice.dto.event.NewEventDto;
import ru.practicum.ewm.mainservice.dto.event.UpdateEventUserRequest;
import ru.practicum.ewm.mainservice.dto.location.LocationDto;
import ru.practicum.ewm.mainservice.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.mainservice.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.ewm.mainservice.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.mainservice.enums.EventState;
import ru.practicum.ewm.mainservice.enums.RequestStatus;
import ru.practicum.ewm.mainservice.enums.StateAction;
import ru.practicum.ewm.mainservice.exception.custom.ConflictException;
import ru.practicum.ewm.mainservice.exception.custom.IncorrectParametersException;
import ru.practicum.ewm.mainservice.exception.custom.ObjectNotFoundExceptionCust;
import ru.practicum.ewm.mainservice.mapper.EventMapper;
import ru.practicum.ewm.mainservice.mapper.RequestMapper;
import ru.practicum.ewm.mainservice.model.*;
import ru.practicum.ewm.mainservice.repository.JpaEventRepository;
import ru.practicum.ewm.mainservice.service.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class EventServiceImpl implements EventService {
    private final JpaEventRepository jpaEventRepository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final LocationService locationService;
    private final RequestService requestService;

    @Override
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        User user = userService.checkUserById(userId);

        LocalDateTime createdDate = LocalDateTime.now();

        checkDateTimeInFuture(newEventDto.getEventDate(), 2);

        Category category = categoryService.checkCategoryById(newEventDto.getCategory());

        Location location = locationService.prepareLocation(newEventDto.getLocation());

        Event event = EventMapper.newEventDtoToEvent(newEventDto, category, user, EventState.PENDING,
                createdDate, location);

        return EventMapper.eventToEventFullDto(jpaEventRepository.save(event));
    }

    @Override
    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest) {
        userService.checkUserById(userId);
        Event currentEvent = checkEventByOwnerAndEventId(eventId, userId);

        if (currentEvent.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Невозможно изменить событие со статусом PUBLISHED");
        }

        toMap(updateEventUserRequest).entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .forEach(entry -> eventUpdater(currentEvent, entry));

        return EventMapper.eventToEventFullDto(jpaEventRepository.save(currentEvent));
    }

    @Override
    public List<EventShortDto> getEventsByUserId(Long userId, Integer from, Integer size) {
        userService.checkUserById(userId);

        PageRequest pageRequest = PageRequest.of(from / size, size, Sort.by(Sort.Direction.ASC, "id"));
        return jpaEventRepository.findAll(pageRequest).getContent()
                .stream().map(EventMapper::toEventShortDto).collect(Collectors.toList());
    }

    @Override
    public EventFullDto getEvent(Long userId, Long eventId) {
        userService.checkUserById(userId);
        Event event = checkEventByOwnerAndEventId(userId, eventId);
        return EventMapper.eventToEventFullDto(event);
    }

    @Override
    public List<ParticipationRequestDto> getRequestByEventAndOwner(Long userId, Long eventId) {
        userService.checkUserById(userId);
        checkEventByOwnerAndEventId(userId, eventId);
        List<ParticipationRequest> requests = requestService.getRequestsByEventId(eventId);
        return requests.stream().map(RequestMapper::participationRequestToDto).collect(Collectors.toList());
    }

    @Override
    public EventRequestStatusUpdateResult updateStatusRequest(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest inputUpdate) {
        userService.checkUserById(userId);
        Event event = checkEventByOwnerAndEventId(userId, eventId);

        if (!event.isRequestModeration() || event.getParticipantLimit() == 0) {
            throw new ConflictException("Для данного события подтверждение запросов не требуется");
        }

        List<ParticipationRequest> requestsForUpdate =
                requestService.getRequestsByEventIdAndIdsAndStatus(
                        eventId, inputUpdate.getRequestIds(), RequestStatus.PENDING);

        int confirmedRequestsCount = requestService.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);


        switch (inputUpdate.getStatus()) {
            case CONFIRMED:
                if (event.getParticipantLimit() == confirmedRequestsCount) {
                    throw new ConflictException("Лимит участников исчерпан");
                }

                for (ParticipationRequest request : requestsForUpdate) {
                    if (confirmedRequestsCount < event.getParticipantLimit()) {
                        request.setStatus(RequestStatus.CONFIRMED);
                        confirmedRequestsCount++;
                    } else {
                        request.setStatus(RequestStatus.REJECTED);
                    }
                }
                break;
            case REJECTED:
                for (ParticipationRequest request : requestsForUpdate) {
                    request.setStatus(RequestStatus.REJECTED);
                }
                break;
            default:
                throw new IncorrectParametersException("Некорректный статус - " + inputUpdate.getStatus());
        }
        requestService.saveAll(requestsForUpdate);

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();

        result.setConfirmedRequests(requestsForUpdate.stream()
                .filter(request -> request.getStatus().equals(RequestStatus.CONFIRMED))
                .map(RequestMapper::participationRequestToDto)
                .collect(Collectors.toList()));

        result.setRejectedRequests(requestsForUpdate.stream()
                .filter(request -> request.getStatus().equals(RequestStatus.REJECTED))
                .map(RequestMapper::participationRequestToDto)
                .collect(Collectors.toList()));

        return result;
    }


    private void checkDateTimeInFuture(LocalDateTime time, int hoursPlus) {
        if (time.isBefore(LocalDateTime.now().plusHours(hoursPlus))) {
            throw new IllegalArgumentException(String.format(
                    "Поле: eventDate. Ошибка: должно содержать дату, не ранее чем через %s часа. Значение: "
                            + time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")), hoursPlus));
        }
    }

    private Event checkEventByOwnerAndEventId(Long eventId, Long userId) {
        return jpaEventRepository.findByIdAndInitiatorId(userId, eventId).orElseThrow(
                () -> new ObjectNotFoundExceptionCust("Событие с id = " + eventId + " с инициатором с id = " + userId +
                        " не существует"));
    }

    private void eventUpdater(Event event, Map.Entry<String, Object> entry) {
        Object object = entry.getValue();

        switch (entry.getKey()) {
            case "annotation":
                String annotation = (String) object;
                if (!annotation.isBlank()) {
                    event.setAnnotation(annotation);
                }
                break;
            case "category":
                Category category = categoryService.checkCategoryById(((Category) object).getId());
                event.setCategory(category);
                break;
            case "description":
                String description = (String) object;
                if (!description.isBlank()) {
                    event.setDescription(description);
                }
                break;
            case "eventDate":
                LocalDateTime eventDate = (LocalDateTime) object;
                checkDateTimeInFuture(eventDate, 2);
                event.setEventDate(eventDate);
                break;
            case "location":
                assert object instanceof LocationDto;
                Location location = locationService.prepareLocation((LocationDto) object);
                event.setLocation(location);
                break;
            case "paid":
                event.setPaid((Boolean) object);
                break;
            case "participantLimit":
                assert object instanceof Integer;
                event.setParticipantLimit((Integer) object);
                break;
            case "requestModeration":
                assert object instanceof Boolean;
                event.setRequestModeration((Boolean) object);
                break;
            case "stateAction":
                assert object instanceof StateAction;
                StateAction stateAction = (StateAction) object;
                switch (stateAction) {
                    case SEND_TO_REVIEW:
                        event.setState(EventState.PENDING);
                        break;
                    case CANCEL_REVIEW:
                        event.setState(EventState.CANCELED);
                        break;
                }
                break;
            case "title":
                String title = (String) object;
                if (!title.isBlank()) {
                    event.setTitle(title);
                }
                break;
        }
    }

    private Map<String, Object> toMap(UpdateEventUserRequest updateEventUserRequest) {
        Gson gson = new Gson();
        String json = gson.toJson(updateEventUserRequest);
        return gson.fromJson(json, new TypeToken<Map<String, Object>>() {
        }.getType());
    }
}