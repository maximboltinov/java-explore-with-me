package ru.practicum.ewm.mainservice.service.implementation;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.mainservice.StatsClient;
import ru.practicum.ewm.mainservice.dto.event.NewEventDto;
import ru.practicum.ewm.mainservice.dto.event.UpdateEventUserRequest;
import ru.practicum.ewm.mainservice.dto.location.LocationDto;
import ru.practicum.ewm.mainservice.enums.EventState;
import ru.practicum.ewm.mainservice.enums.StateAction;
import ru.practicum.ewm.mainservice.exception.custom.ConflictException;
import ru.practicum.ewm.mainservice.exception.custom.IncorrectParametersException;
import ru.practicum.ewm.mainservice.exception.custom.ObjectNotFoundExceptionCust;
import ru.practicum.ewm.mainservice.model.Category;
import ru.practicum.ewm.mainservice.model.Event;
import ru.practicum.ewm.mainservice.model.Location;
import ru.practicum.ewm.mainservice.model.User;
import ru.practicum.ewm.mainservice.repository.JpaEventRepository;
import ru.practicum.ewm.mainservice.service.*;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        event.setState(EventState.PENDING);

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
        event.setState(EventState.PENDING);

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
    void getEventsByUserId() {
    }

    @Test
    void getEvent() {
    }

    @Test
    void getRequestByEventAndOwner() {
    }

    @Test
    void updateStatusRequest() {
    }

    @Test
    void getFilteredEvents() {
    }

    @Test
    void getEventById() {
    }

    @Test
    void getEventsAdmin() {
    }

    @Test
    void updateEventAdmin() {
    }

    @Test
    void loadShortEventsViewsNumber() {
    }

    @Test
    void checkEvent() {
    }

    @Test
    void testUpdateEvent() {
    }

    @Test
    void getEventViewsNumber() {
    }
}