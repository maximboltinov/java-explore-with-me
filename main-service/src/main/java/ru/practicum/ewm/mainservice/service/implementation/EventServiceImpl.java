package ru.practicum.ewm.mainservice.service.implementation;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.mainservice.StatsClient;
import ru.practicum.ewm.mainservice.dto.event.*;
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
import ru.practicum.ewm.stats.statsdto.EndpointHit;
import ru.practicum.ewm.stats.statsdto.ViewStats;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static ru.practicum.ewm.mainservice.specification.EventSpecification.*;

@Service
@AllArgsConstructor
public class EventServiceImpl implements EventService {
    private final JpaEventRepository jpaEventRepository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final LocationService locationService;
    private final RequestService requestService;
    private final StatsClient statsClient;

    @Override
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        User user = userService.checkUserById(userId);

        LocalDateTime createdDate = LocalDateTime.now();

        checkDateTimeInFuture(newEventDto.getEventDate(), 2);

        Category category = categoryService.checkCategoryById(newEventDto.getCategory());

        Location location = locationService.prepareLocation(newEventDto.getLocation());

        Event event = EventMapper.newEventDtoToEvent(newEventDto, category, user, EventState.PENDING,
                createdDate, location);
        event.setConfirmedRequests(0);

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
                .forEach(entry -> eventUpdater(currentEvent, updateEventUserRequest, entry.getKey()));

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
        Event event = checkEventByOwnerAndEventId(eventId, userId);
        return EventMapper.eventToEventFullDto(event);
    }

    @Override
    public List<ParticipationRequestDto> getRequestByEventAndOwner(Long userId, Long eventId) {
        userService.checkUserById(userId);
        checkEventByOwnerAndEventId(eventId, userId);
        List<ParticipationRequest> requests = requestService.getRequestsByEventId(eventId);
        return requests.stream().map(RequestMapper::participationRequestToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateStatusRequest(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest inputUpdate) {
        userService.checkUserById(userId);
        Event event = checkEventByOwnerAndEventId(eventId, userId);

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
                    if (request.getStatus() == RequestStatus.CONFIRMED) {
                        throw new ConflictException("Нельзя отклонить подтвержденную заявку");
                    }
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

        if (!result.getConfirmedRequests().isEmpty()) {
            event.setConfirmedRequests(event.getConfirmedRequests() + result.getConfirmedRequests().size());
        }

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
    @Transactional
    public List<EventFullDto> getEventsAdmin(EventRequestParamsAdmin requestParamsAdmin, int from, int size) {
        PageRequest pageRequest = PageRequest.of(from / size, size, Sort.by(Sort.Direction.ASC, "id"));
        List<Specification<Event>> specifications = requestParamsAdminToSpecifications(requestParamsAdmin);
        Specification<Event> eventSpecification = specifications.stream().reduce(Specification::and).orElse(null);

        List<Event> events;

//        if (eventSpecification == null) {
//            events = jpaEventRepository.findAll(pageRequest).getContent();
//        } else {
            events = jpaEventRepository.findAll(eventSpecification, pageRequest).getContent();
//        }


        List<EventFullDto> result = events.stream().map(EventMapper::eventToEventFullDto).collect(Collectors.toList());

        List<Long> eventsResultIds = result.stream().map(EventFullDto::getId).collect(Collectors.toList());
        Map<Long, Long> views = getViewForEvents(eventsResultIds);

        result.forEach(event -> event.setViews(getViewsForEvent(event.getId(), views)));

        return result;
    }

    @Override
    public EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest) {
        Event event = jpaEventRepository.findById(eventId).orElseThrow(() ->
                new ObjectNotFoundExceptionCust("Событие с id = " + eventId + " не существует"));

        toMap(updateEventAdminRequest).entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .forEach(entry -> eventUpdaterAdmin(event, updateEventAdminRequest, entry.getKey()));

        Event savedEvent = jpaEventRepository.save(event);

        return EventMapper.eventToEventFullDto(savedEvent);
    }


    private void checkDateTimeInFuture(LocalDateTime time, int hoursPlus) {
        if (time.isBefore(LocalDateTime.now().plusHours(hoursPlus))) {
            throw new IncorrectParametersException(String.format(
                    "Поле: eventDate. Ошибка: должно содержать дату, не ранее чем через %s часа. Значение: "
                            + time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")), hoursPlus));
        }
    }

    private Event checkEventByOwnerAndEventId(Long eventId, Long userId) {
        return jpaEventRepository.findEventByIdAndInitiatorId(eventId, userId).orElseThrow(
                () -> new ObjectNotFoundExceptionCust("Событие с id = " + eventId + " с инициатором с id = " + userId +
                        " не существует"));
    }

    private void eventUpdater(Event event, UpdateEventUserRequest updateEventUserRequest, String fieldName) {

        switch (fieldName) {
            case "annotation":
                String annotation = updateEventUserRequest.getAnnotation();
                if (!annotation.isBlank()) {
                    event.setAnnotation(annotation);
                }
                break;
            case "category":
                Category category = categoryService.checkCategoryById(updateEventUserRequest.getCategory());
                event.setCategory(category);
                break;
            case "description":
                String description = updateEventUserRequest.getDescription();
                if (!description.isBlank()) {
                    event.setDescription(description);
                }
                break;
            case "eventDate":
                LocalDateTime eventDate = updateEventUserRequest.getEventDate();
                checkDateTimeInFuture(eventDate, 2);
                event.setEventDate(eventDate);
                break;
            case "location":
                Location location = locationService.prepareLocation(updateEventUserRequest.getLocation());
                event.setLocation(location);
                break;
            case "paid":
                event.setPaid(updateEventUserRequest.getPaid());
                break;
            case "participantLimit":
                event.setParticipantLimit(updateEventUserRequest.getParticipantLimit());
                break;
            case "requestModeration":
                event.setRequestModeration(updateEventUserRequest.getRequestModeration());
                break;
            case "stateAction":
                StateAction stateAction = updateEventUserRequest.getStateAction();
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
                String title = updateEventUserRequest.getTitle();
                if (!title.isBlank()) {
                    event.setTitle(title);
                }
                break;
        }
    }

    private void eventUpdaterAdmin(Event event, UpdateEventAdminRequest updateEventAdminRequest, String fieldName) {

        switch (fieldName) {
            case "annotation":
                String annotation = updateEventAdminRequest.getAnnotation();
                if (!annotation.isBlank()) {
                    event.setAnnotation(annotation);
                }
                break;
            case "category":
                Category category = categoryService.checkCategoryById(updateEventAdminRequest.getCategory());
                event.setCategory(category);
                break;
            case "description":
                String description = updateEventAdminRequest.getDescription();
                if (!description.isBlank()) {
                    event.setDescription(description);
                }
                break;
            case "eventDate":
                LocalDateTime eventDate = updateEventAdminRequest.getEventDate();
                checkDateTimeInFuture(eventDate, 1);
                event.setEventDate(eventDate);
                break;
            case "location":
                Location location = locationService.prepareLocation(updateEventAdminRequest.getLocation());
                event.setLocation(location);
                break;
            case "paid":
                event.setPaid(updateEventAdminRequest.getPaid());
                break;
            case "participantLimit":
                event.setParticipantLimit(updateEventAdminRequest.getParticipantLimit());
                break;
            case "requestModeration":
                event.setRequestModeration(updateEventAdminRequest.getRequestModeration());
                break;
            case "stateAction":
                StateAction stateAction = updateEventAdminRequest.getStateAction();
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
                break;
            case "title":
                String title = updateEventAdminRequest.getTitle();
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

//        resultSpecification.add(eventDateAfterOrEqual(requestParamsAdmin.getRangeStart()));
//        resultSpecification.add(eventDateBeforeOrEqual(requestParamsAdmin.getRangeEnd()));

//        resultSpecification.add(isAvailable(requestParamsAdmin.isOnlyAvailable()));
        return resultSpecification.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public void loadShortEventsViewsNumber(List<EventShortDto> eventShortDtos) {
        Map<Long, Long> eventsViews = getViewForEvents(eventShortDtos.stream().map(EventShortDto::getId).collect(Collectors.toList()));
        for (EventShortDto dto : eventShortDtos) {
            dto.setViews(eventsViews.get(dto.getId()));
        }
    }

    private Map<Long, Long> getViewForEvents(List<Long> eventsIds) {
        if (eventsIds.isEmpty()) {
            return Map.of();
        }

        List<String> uris = eventsIds.stream().map(id -> "/events/" + id).collect(Collectors.toList());

        ResponseEntity<List<ViewStats>> response = statsClient.getStats(
                LocalDateTime.now().minusYears(300).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                LocalDateTime.now().plusMinutes(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                uris, true);

        List<ViewStats> viewStatsList = response.getBody();

        assert viewStatsList != null;
        Map<Long, Long> eventsViews = viewStatsList.stream()
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

    @Override
    public Long getViewsForEvent(Long eventId, Map<Long, Long> views) {
        if (views.isEmpty() || !views.containsKey(eventId)) {
            return 0L;
        } else {
            return views.get(eventId);
        }
    }
}