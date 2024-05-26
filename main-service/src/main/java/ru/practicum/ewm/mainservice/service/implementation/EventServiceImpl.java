package ru.practicum.ewm.mainservice.service.implementation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.mainservice.dto.event.*;
import ru.practicum.ewm.mainservice.dto.location.LocationDto;
import ru.practicum.ewm.mainservice.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.mainservice.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.ewm.mainservice.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.mainservice.enums.EventState;
import ru.practicum.ewm.mainservice.enums.RequestStatus;
import ru.practicum.ewm.mainservice.enums.SortType;
import ru.practicum.ewm.mainservice.enums.StateAction;
import ru.practicum.ewm.mainservice.exception.custom.ConflictException;
import ru.practicum.ewm.mainservice.exception.custom.IncorrectParametersException;
import ru.practicum.ewm.mainservice.exception.custom.ObjectNotFoundExceptionCust;
import ru.practicum.ewm.mainservice.mapper.EventMapper;
import ru.practicum.ewm.mainservice.mapper.RequestMapper;
import ru.practicum.ewm.mainservice.model.*;
import ru.practicum.ewm.mainservice.repository.JpaEventRepository;
import ru.practicum.ewm.mainservice.service.*;
import ru.practicum.ewm.stats.statsclient.StatsClient;
import ru.practicum.ewm.stats.statsdto.EndpointHit;
import ru.practicum.ewm.stats.statsdto.ViewStats;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.net.URLEncoder.encode;
import static ru.practicum.ewm.mainservice.specification.EventSpecification.*;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final JpaEventRepository jpaEventRepository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final LocationService locationService;
    private final RequestService requestService;
    private final ObjectMapper objectMapper;


    private final StatsClient statsClient = new StatsClient("http://stats-server:9090", new RestTemplateBuilder());

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
                .stream().map(EventMapper::eventToEventShortDto).collect(Collectors.toList());
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

    @Override
    public List<EventShortDto> getFilteredEvents(
            EventRequestParams eventRequestParams, Integer from, Integer size, HttpServletRequest request) {
        validateDateRange(eventRequestParams);

        PageRequest pageRequest = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "eventDate"));
        List<Specification<Event>> specifications = eventRequestParamsToSpecifications(eventRequestParams);
        List<Event> events = jpaEventRepository.findAll(specifications.stream().reduce(Specification::and).orElse(null),
                pageRequest).getContent();

        List<EventShortDto> eventShortDtos = events.stream().map(EventMapper::eventToEventShortDto).collect(Collectors.toList());
        loadShortEventsViewsNumber(eventShortDtos);

        if (eventRequestParams.getSort() != null && eventRequestParams.getSort().equals(SortType.VIEWS)) {
            eventShortDtos.sort(Comparator.comparing(EventShortDto::getViews));
        }

        statsClient.create(new EndpointHit("main-service", request.getRequestURI(), request.getRemoteAddr(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));

        return events.stream().map(EventMapper::eventToEventShortDto).collect(Collectors.toList());
    }

    @Override
    public EventFullDto getEventById(Long eventId, HttpServletRequest request) {
        Event event = checkEvent(eventId);
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ObjectNotFoundExceptionCust("Событие с id = " + eventId + " не опубликовано");
        }

        Long eventViewsNumbers = getEventViewsNumber(eventId);

        statsClient.create(new EndpointHit("main-service", request.getRequestURI(), request.getRemoteAddr(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));

        EventFullDto eventFullDto = EventMapper.eventToEventFullDto(event);
        eventFullDto.setViews(eventViewsNumbers);
        return eventFullDto;
    }

    @Override
    public List<EventFullDto> getEventsAdmin(EventRequestParamsAdmin requestParamsAdmin, int from, int size) {
        PageRequest pageRequest = PageRequest.of(from / size, size, Sort.by(Sort.Direction.ASC, "id"));
        List<Specification<Event>> specifications = requestParamsAdminToSpecifications(requestParamsAdmin);
        List<Event> events = jpaEventRepository.findAll(
                specifications.stream().reduce(Specification::and).orElse(null),
                pageRequest).getContent();
        return events.stream().map(EventMapper::eventToEventFullDto).collect(Collectors.toList());
    }

    @Override
    public EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest) {
        Event event = jpaEventRepository.findById(eventId).orElseThrow(() ->
                new ObjectNotFoundExceptionCust("Событие с id = " + eventId + " не существует"));

        toMap(updateEventAdminRequest).entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .forEach(entry -> eventUpdater(event, entry));

        Event savedEvent = jpaEventRepository.save(event);

        return EventMapper.eventToEventFullDto(savedEvent);
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
                    case PUBLISH_EVENT:
                        checkIfEventIsCanceled(event);
                        checkIfEventIsAlreadyPublished(event);
                        event.setState(EventState.PUBLISHED);
                        event.setPublishedDate(LocalDateTime.now());
                        break;
                    case REJECT_EVENT:
                        checkIfEventIsAlreadyPublished(event);
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

    private Map<String, Object> toMap(UpdateEventAdminRequest updateEventAdminRequest) {
        Gson gson = new Gson();
        String json = gson.toJson(updateEventAdminRequest);
        return gson.fromJson(json, new TypeToken<Map<String, Object>>() {
        }.getType());
    }

    private void validateDateRange(EventRequestParams eventRequestParams) {
        if (eventRequestParams.getRangeStart() != null && eventRequestParams.getRangeEnd() != null) {
            if (eventRequestParams.getRangeStart().isAfter(eventRequestParams.getRangeEnd())) {
                throw new IncorrectParametersException("Некорректный временной диапазон");
            }
        }

        if (eventRequestParams.getRangeStart() == null && eventRequestParams.getRangeEnd() != null) {
            if (eventRequestParams.getRangeEnd().isBefore(LocalDateTime.now())) {
                throw new IncorrectParametersException("Некорректный временной диапазон");
            }
        }
    }

    private List<Specification<Event>> eventRequestParamsToSpecifications(EventRequestParams eventRequestParams) {
        List<Specification<Event>> resultSpecification = new ArrayList<>();
        resultSpecification.add(eventStatusEquals(EventState.PUBLISHED));
        resultSpecification.add(likeText(eventRequestParams.getText()));
        resultSpecification.add(categoryIn(eventRequestParams.getCategories()));
        resultSpecification.add(isPaid(eventRequestParams.getPaid()));
        resultSpecification.add(eventDateInRange(eventRequestParams.getRangeStart(), eventRequestParams.getRangeEnd()));
        resultSpecification.add(isAvailable(eventRequestParams.getOnlyAvailable()));
        return resultSpecification.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private List<Specification<Event>> requestParamsAdminToSpecifications(EventRequestParamsAdmin requestParamsAdmin) {
        List<Specification<Event>> resultSpecification = new ArrayList<>();
        resultSpecification.add(eventStatusIn(requestParamsAdmin.getStates()));
        resultSpecification.add(initiatorIdIn(requestParamsAdmin.getUsers()));
        resultSpecification.add(categoryIn(requestParamsAdmin.getCategories()));
        resultSpecification.add(eventDateInRange(requestParamsAdmin.getRangeStart(), requestParamsAdmin.getRangeEnd()));
        resultSpecification.add(isAvailable(requestParamsAdmin.isOnlyAvailable()));
        return resultSpecification.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    public void loadShortEventsViewsNumber(List<EventShortDto> eventShortDtos) {
        Map<Long, Long> eventsViews = getViewForEvents(eventShortDtos.stream().map(EventShortDto::getId).collect(Collectors.toList()));
        for (EventShortDto dto : eventShortDtos) {
            dto.setViews(eventsViews.get(dto.getId()));
        }
    }

    private Map<Long, Long> getViewForEvents(List<Long> eventsIds) {
        List<String> uris = eventsIds.stream().map(id -> "/events/" + id).collect(Collectors.toList());
        ResponseEntity<Object> response = statsClient.getStats(
                encode(LocalDateTime.MIN.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), StandardCharsets.UTF_8),
                encode(LocalDateTime.MAX.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), StandardCharsets.UTF_8),
                uris, true);

        List<ViewStats> viewsStats = objectMapper.convertValue(response.getBody(), new TypeReference<>() {
        });

        Map<Long, Long> eventsViews = viewsStats.stream()
                .collect(Collectors.toMap(
                        viewStats -> Long.parseLong(viewStats.getUri().substring(viewStats.getUri().lastIndexOf("/") + 1)),
                        ViewStats::getHits));


        for (Long eventId : eventsIds) {
            if (!eventsViews.containsKey(eventId)) {
                eventsViews.put(eventId, 0L);
            }
        }
        return eventsViews;
    }

    @Override
    public Event checkEvent(Long eventId) {
        return jpaEventRepository.findById(eventId)
                .orElseThrow(() -> new ObjectNotFoundExceptionCust("События с id = " + eventId + " не существует"));
    }

    @Override
    public void updateEvent(Event event) {
        jpaEventRepository.save(event);
    }

    public Long getEventViewsNumber(Long eventId) {
        Map<Long, Long> eventViews = getViewForEvents(List.of(eventId));
        return eventViews.get(eventId);
    }

    private void updateEventState(StateAction stateAction, Event event) {
        if (stateAction == null) {
            return;
        }
        switch (stateAction) {
            case PUBLISH_EVENT:
                checkIfEventIsCanceled(event);
                checkIfEventIsAlreadyPublished(event);
                event.setState(EventState.PUBLISHED);
                event.setPublishedDate(LocalDateTime.now());
                break;
            case REJECT_EVENT:
                checkIfEventIsAlreadyPublished(event);
                event.setState(EventState.CANCELED);
                break;
        }
    }

    private void checkIfEventIsCanceled(Event event) {
        if (event.getState().equals(EventState.CANCELED)) {
            throw new ConflictException("Нельзя опубликовать отмененное событие");
        }
    }

    private void checkIfEventIsAlreadyPublished(Event event) {
        if (event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Событие уже опубликовано");
        }
    }
}