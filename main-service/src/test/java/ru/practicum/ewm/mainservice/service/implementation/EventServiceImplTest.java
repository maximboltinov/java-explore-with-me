package ru.practicum.ewm.mainservice.service.implementation;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.mainservice.StatsClient;
import ru.practicum.ewm.mainservice.dto.event.*;
import ru.practicum.ewm.mainservice.dto.location.LocationDto;
import ru.practicum.ewm.mainservice.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.mainservice.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.mainservice.enums.EventState;
import ru.practicum.ewm.mainservice.enums.RequestStatus;
import ru.practicum.ewm.mainservice.enums.StateAction;
import ru.practicum.ewm.mainservice.exception.custom.ConflictException;
import ru.practicum.ewm.mainservice.exception.custom.IncorrectParametersException;
import ru.practicum.ewm.mainservice.exception.custom.ObjectNotFoundExceptionCust;
import ru.practicum.ewm.mainservice.model.*;
import ru.practicum.ewm.mainservice.repository.JpaEventRepository;
import ru.practicum.ewm.mainservice.service.*;
import ru.practicum.ewm.stats.statsdto.EndpointHit;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static ru.practicum.ewm.mainservice.enums.EventState.CANCELED;
import static ru.practicum.ewm.mainservice.enums.EventState.PENDING;
import static ru.practicum.ewm.mainservice.enums.SortType.VIEWS;
import static ru.practicum.ewm.mainservice.enums.StateAction.REJECT_EVENT;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {
    private EventService eventService;

    @Mock
    private JpaEventRepository jpaEventRepository;
    @Mock
    private UserService userService;
    @Mock
    private CategoryService categoryService;
    @Mock
    private LocationService locationService;
    @Mock
    private RequestService requestService;
    @Mock
    private StatsClient statsClient;

    private HttpServletRequest request = mock(HttpServletRequest.class);

    private final EasyRandom generator = new EasyRandom();

    @Captor
    private ArgumentCaptor<Event> eventArgumentCaptor;

    @BeforeEach
    public void setUp() {
        eventService = new EventServiceImpl(
                jpaEventRepository, userService, categoryService, locationService, requestService, statsClient);
    }

    @Test
    void createEvent_userNotFound_ObjectNotFoundExceptionCust() {
        when(userService.checkUserById(anyLong()))
                .thenThrow(new ObjectNotFoundExceptionCust(""));

        assertThrows(ObjectNotFoundExceptionCust.class, () -> eventService.createEvent(1L, new NewEventDto()));
    }

    @Test
    void createEvent_eventDateEarly_IllegalArgumentException() {
        NewEventDto newEventDto = NewEventDto.builder().eventDate(LocalDateTime.now()).build();

        assertThrows(IncorrectParametersException.class, () -> eventService.createEvent(1L, newEventDto));
    }

    @Test
    void createEvent_CategoryIncorrect_ObjectNotFoundExceptionCust() {
        NewEventDto newEventDto = NewEventDto.builder().eventDate(LocalDateTime.now().plusHours(3)).category(1L).build();

        when(categoryService.checkCategoryById(anyLong()))
                .thenThrow(new ObjectNotFoundExceptionCust(""));

        assertThrows(ObjectNotFoundExceptionCust.class, () -> eventService.createEvent(1L, newEventDto));
    }

    @Test
    void createEvent_correct() {
        NewEventDto newEventDto = generator.nextObject(NewEventDto.class);
        newEventDto.setEventDate(LocalDateTime.now().plusHours(5));
        User user = generator.nextObject(User.class);
        Category category = generator.nextObject(Category.class);
        Location location = generator.nextObject(Location.class);

        Event someEvent = generator.nextObject(Event.class);

        when(userService.checkUserById(anyLong()))
                .thenReturn(user);
        when(categoryService.checkCategoryById(anyLong()))
                .thenReturn(category);
        when(locationService.prepareLocation(any(LocationDto.class)))
                .thenReturn(location);
        when(jpaEventRepository.save(any(Event.class)))
                .thenReturn(someEvent);

        eventService.createEvent(1L, newEventDto);

        verify(jpaEventRepository).save(eventArgumentCaptor.capture());
        Event event = eventArgumentCaptor.getValue();

        assertEquals(newEventDto.getAnnotation(), event.getAnnotation());
        assertEquals(category, event.getCategory());
        assertEquals(newEventDto.getDescription(), event.getDescription());
        assertEquals(newEventDto.getEventDate(), event.getEventDate());
        assertEquals(location, event.getLocation());
        assertEquals(newEventDto.isPaid(), event.isPaid());
        assertEquals(newEventDto.getParticipantLimit(), event.getParticipantLimit());
        assertEquals(newEventDto.isRequestModeration(), event.isRequestModeration());
        assertEquals(newEventDto.getTitle(), event.getTitle());
    }

    @Test
    void updateEvent_userNotFound_ObjectNotFoundExceptionCust() {
        when(userService.checkUserById(anyLong()))
                .thenThrow(new ObjectNotFoundExceptionCust(""));

        assertThrows(ObjectNotFoundExceptionCust.class,
                () -> eventService.updateEvent(1L, 1L, new UpdateEventUserRequest()));
    }

    @Test
    void updateEvent_userNotOwner_ObjectNotFoundExceptionCust() {
        when(userService.checkUserById(anyLong()))
                .thenReturn(new User());
        when(jpaEventRepository.findEventByIdAndInitiatorId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        assertThrows(ObjectNotFoundExceptionCust.class,
                () -> eventService.updateEvent(1L, 1L, new UpdateEventUserRequest()));
    }

    @Test
    void updateEvent_eventStatePublished_ConflictException() {
        UpdateEventUserRequest updateEventUserRequest = generator.nextObject(UpdateEventUserRequest.class);
        updateEventUserRequest.setStateAction(StateAction.CANCEL_REVIEW);
        updateEventUserRequest.setEventDate(LocalDateTime.now().plusHours(5));

        Event event = generator.nextObject(Event.class);
        event.setState(EventState.PUBLISHED);

        when(userService.checkUserById(anyLong()))
                .thenReturn(new User());
        when(jpaEventRepository.findEventByIdAndInitiatorId(anyLong(), anyLong()))
                .thenReturn(Optional.of(event));

        assertThrows(ConflictException.class,
                () -> eventService.updateEvent(1L, 1L, updateEventUserRequest));
    }

    @Test
    void updateEvent_incorrectEventDate_IllegalArgumentException() {
        UpdateEventUserRequest updateEventUserRequest = generator.nextObject(UpdateEventUserRequest.class);
        updateEventUserRequest.setStateAction(StateAction.CANCEL_REVIEW);
        updateEventUserRequest.setEventDate(LocalDateTime.now().plusHours(1));

        Event event = generator.nextObject(Event.class);
        event.setState(PENDING);

        when(userService.checkUserById(anyLong()))
                .thenReturn(new User());
        when(jpaEventRepository.findEventByIdAndInitiatorId(anyLong(), anyLong()))
                .thenReturn(Optional.of(event));

        assertThrows(IncorrectParametersException.class,
                () -> eventService.updateEvent(1L, 1L, updateEventUserRequest));
    }

    @Test
    void updateEvent_correct() {
        UpdateEventUserRequest updateEventUserRequest = generator.nextObject(UpdateEventUserRequest.class);
        updateEventUserRequest.setStateAction(StateAction.CANCEL_REVIEW);
        updateEventUserRequest.setEventDate(LocalDateTime.now().plusHours(5));
        updateEventUserRequest.setCategory(1L);

        Event event = generator.nextObject(Event.class);
        event.setState(PENDING);

        Category category = Category.builder().id(1L).name("category").build();

        Event someEvent = generator.nextObject(Event.class);

        when(userService.checkUserById(anyLong()))
                .thenReturn(new User());
        when(jpaEventRepository.findEventByIdAndInitiatorId(anyLong(), anyLong()))
                .thenReturn(Optional.of(event));
        when(categoryService.checkCategoryById(anyLong()))
                .thenReturn(category);
        when(jpaEventRepository.save(any(Event.class)))
                .thenReturn(someEvent);
        when(locationService.prepareLocation(any(LocationDto.class)))
                .thenReturn(new Location(1L, updateEventUserRequest.getLocation().getLat(),
                        updateEventUserRequest.getLocation().getLon()));

        eventService.updateEvent(1L, 1L, updateEventUserRequest);

        verify(jpaEventRepository).save(eventArgumentCaptor.capture());
        Event eventUpdate = eventArgumentCaptor.getValue();

        assertEquals(updateEventUserRequest.getAnnotation(), eventUpdate.getAnnotation());
        assertEquals(updateEventUserRequest.getCategory(), eventUpdate.getCategory().getId());
        assertEquals(updateEventUserRequest.getDescription(), eventUpdate.getDescription());
        assertEquals(updateEventUserRequest.getEventDate(), eventUpdate.getEventDate());
        assertEquals(1L, eventUpdate.getLocation().getId());
        assertEquals(updateEventUserRequest.getLocation().getLat(), eventUpdate.getLocation().getLat());
        assertEquals(updateEventUserRequest.getLocation().getLon(), eventUpdate.getLocation().getLon());
        assertEquals(updateEventUserRequest.getPaid(), eventUpdate.isPaid());
        assertEquals(updateEventUserRequest.getParticipantLimit(), eventUpdate.getParticipantLimit());
        assertEquals(updateEventUserRequest.getRequestModeration(), eventUpdate.isRequestModeration());
        assertEquals(updateEventUserRequest.getTitle(), eventUpdate.getTitle());
        assertEquals(EventState.CANCELED, eventUpdate.getState());
    }

    @Test
    void getEventsByUserId_userNotFound_ObjectNotFoundExceptionCust() {
        when(userService.checkUserById(anyLong()))
                .thenThrow(new ObjectNotFoundExceptionCust(""));

        assertThrows(ObjectNotFoundExceptionCust.class,
                () -> eventService.getEventsByUserId(1L, 0, 10));

        verify(jpaEventRepository, never()).findAll(any(PageRequest.class));
    }

    @Test
    void getEventsByUserId_correct() {
        when(jpaEventRepository.findAll(any(PageRequest.class)))
                .thenReturn(Page.empty());

        eventService.getEventsByUserId(1L, 0, 10);

        verify(jpaEventRepository).findAll(any(PageRequest.class));
    }

    @Test
    void getEvent_eventNotFound_ObjectNotFoundExceptionCust() {
        when(jpaEventRepository.findEventByIdAndInitiatorId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        assertThrows(ObjectNotFoundExceptionCust.class, () -> eventService.getEvent(1L, 1L));
    }

    @Test
    void getEvent_correct() {
        Event event = generator.nextObject(Event.class);

        when(jpaEventRepository.findEventByIdAndInitiatorId(anyLong(), anyLong()))
                .thenReturn(Optional.of(event));

        EventFullDto result = eventService.getEvent(1L, 1L);

        assertEquals(event.getConfirmedRequests(), result.getConfirmedRequests());
        assertEquals(event.getEventDate(), result.getEventDate());
        assertEquals(event.getId(), result.getId());
    }

    @Test
    void getRequestByEventAndOwner_checkEventByOwnerAndEventId_ObjectNotFoundExceptionCust() {
        when(jpaEventRepository.findEventByIdAndInitiatorId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        assertThrows(ObjectNotFoundExceptionCust.class,
                () -> eventService.getRequestByEventAndOwner(1L, 1L));
    }

    @Test
    void getRequestByEventAndOwner_correct() {
        ParticipationRequest request = generator.nextObject(ParticipationRequest.class);

        when(jpaEventRepository.findEventByIdAndInitiatorId(anyLong(), anyLong()))
                .thenReturn(Optional.of(new Event()));

        when(requestService.getRequestsByEventId(anyLong()))
                .thenReturn(List.of(request));

        List<ParticipationRequestDto> result = eventService.getRequestByEventAndOwner(1L, 1L);

        assertEquals(1, result.size());
    }

    @Test
    void updateStatusRequest_confirmationIsNotRequired_ConflictException() {
        Event event = generator.nextObject(Event.class);
        event.setId(1L);
        event.setRequestModeration(false);
        event.setParticipantLimit(0);

        when(jpaEventRepository.findEventByIdAndInitiatorId(1L, 1L))
                .thenReturn(Optional.of(event));

        assertThrows(ConflictException.class,
                () -> eventService.updateStatusRequest(1L, 1L, new EventRequestStatusUpdateRequest()));

        verify(requestService, never()).saveAll(anyList());
    }

    @Test
    void updateStatusRequest_limitHasBeenReached_ConflictException() {
        Event event = generator.nextObject(Event.class);
        event.setId(1L);
        event.setRequestModeration(true);
        event.setParticipantLimit(20);
        event.setConfirmedRequests(20);

        EventRequestStatusUpdateRequest toUpdate = new EventRequestStatusUpdateRequest();
        toUpdate.setStatus(RequestStatus.CONFIRMED);
        toUpdate.setRequestIds(Set.of(1L, 2L));

        ParticipationRequest participationRequest1 = generator.nextObject(ParticipationRequest.class);
        ParticipationRequest participationRequest2 = generator.nextObject(ParticipationRequest.class);

        List<ParticipationRequest> requestsForUpdate = List.of(participationRequest1, participationRequest2);

        when(jpaEventRepository.findEventByIdAndInitiatorId(1L, 1L))
                .thenReturn(Optional.of(event));

        when(requestService.getRequestsByEventIdAndIdsAndStatus(event.getId(), toUpdate.getRequestIds(), RequestStatus.PENDING))
                .thenReturn(requestsForUpdate);

        assertThrows(ConflictException.class,
                () -> eventService.updateStatusRequest(1L, 1L, toUpdate));

        verify(requestService, never()).saveAll(anyList());
    }

    @Test
    void updateStatusRequest_correct() {
        Event event = generator.nextObject(Event.class);
        event.setId(1L);
        event.setRequestModeration(true);
        event.setParticipantLimit(20);
        event.setConfirmedRequests(10);

        EventRequestStatusUpdateRequest toUpdate = new EventRequestStatusUpdateRequest();
        toUpdate.setStatus(RequestStatus.CONFIRMED);
        toUpdate.setRequestIds(Set.of(1L, 2L));

        ParticipationRequest participationRequest1 = generator.nextObject(ParticipationRequest.class);
        ParticipationRequest participationRequest2 = generator.nextObject(ParticipationRequest.class);

        List<ParticipationRequest> requestsForUpdate = List.of(participationRequest1, participationRequest2);

        when(jpaEventRepository.findEventByIdAndInitiatorId(1L, 1L))
                .thenReturn(Optional.of(event));

        when(requestService.getRequestsByEventIdAndIdsAndStatus(
                event.getId(), toUpdate.getRequestIds(), RequestStatus.PENDING))
                .thenReturn(requestsForUpdate);

        eventService.updateStatusRequest(1L, 1L, toUpdate);

        verify(requestService).saveAll(anyList());
        verify(jpaEventRepository).save(event);
    }

    @Test
    void getFilteredEvents() {
        Event event = generator.nextObject(Event.class);
        EventRequestParams eventRequestParams = generator.nextObject(EventRequestParams.class);
        eventRequestParams.setSort(VIEWS);
        Page<Event> eventPage = Page.empty();
        eventPage.and(event);


        when(jpaEventRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(eventPage);
        doNothing().when(statsClient).create(any(EndpointHit.class));

        eventService.getFilteredEvents(eventRequestParams, 0, 10, request);
    }

    @Test
    void getEventById() {
    }

    @Test
    void updateEventAdmin() {
        UpdateEventAdminRequest updateEventAdminRequest = generator.nextObject(UpdateEventAdminRequest.class);
        updateEventAdminRequest.setStateAction(REJECT_EVENT);
        updateEventAdminRequest.setEventDate(LocalDateTime.now().plusDays(5));
        Event event = generator.nextObject(Event.class);
        event.setState(PENDING);
        Category category = generator.nextObject(Category.class);
        Location location = generator.nextObject(Location.class);

        when(jpaEventRepository.findById(anyLong()))
                .thenReturn(Optional.of(event));
        when(categoryService.checkCategoryById(updateEventAdminRequest.getCategory()))
                .thenReturn(category);
        when(locationService.prepareLocation(updateEventAdminRequest.getLocation()))
                .thenReturn(location);
        when(jpaEventRepository.save(event))
                .thenReturn(event);

        EventFullDto eventFullDto = eventService.updateEventAdmin(1L, updateEventAdminRequest);

        assertEquals(CANCELED, eventFullDto.getState());
    }
}