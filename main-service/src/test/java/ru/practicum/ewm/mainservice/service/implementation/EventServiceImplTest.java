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
import ru.practicum.ewm.mainservice.StatsClient;
import ru.practicum.ewm.mainservice.dto.event.EventFullDto;
import ru.practicum.ewm.mainservice.dto.event.EventRequestParams;
import ru.practicum.ewm.mainservice.dto.event.NewEventDto;
import ru.practicum.ewm.mainservice.dto.event.UpdateEventUserRequest;
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

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

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
        EventRequestParams eventRequestParams = generator.nextObject(EventRequestParams.class);
        eventRequestParams.setRangeStart(LocalDateTime.now().plusDays(5));
        eventRequestParams.setRangeEnd(LocalDateTime.now().plusDays(3));



        assertThrows(IncorrectParametersException.class, () -> eventService.getFilteredEvents(eventRequestParams, 0, 10, new HttpServletRequest() {
            @Override
            public Object getAttribute(String s) {
                return null;
            }

            @Override
            public Enumeration<String> getAttributeNames() {
                return null;
            }

            @Override
            public String getCharacterEncoding() {
                return null;
            }

            @Override
            public void setCharacterEncoding(String s) throws UnsupportedEncodingException {

            }

            @Override
            public int getContentLength() {
                return 0;
            }

            @Override
            public long getContentLengthLong() {
                return 0;
            }

            @Override
            public String getContentType() {
                return null;
            }

            @Override
            public ServletInputStream getInputStream() throws IOException {
                return null;
            }

            @Override
            public String getParameter(String s) {
                return null;
            }

            @Override
            public Enumeration<String> getParameterNames() {
                return null;
            }

            @Override
            public String[] getParameterValues(String s) {
                return new String[0];
            }

            @Override
            public Map<String, String[]> getParameterMap() {
                return null;
            }

            @Override
            public String getProtocol() {
                return null;
            }

            @Override
            public String getScheme() {
                return null;
            }

            @Override
            public String getServerName() {
                return null;
            }

            @Override
            public int getServerPort() {
                return 0;
            }

            @Override
            public BufferedReader getReader() throws IOException {
                return null;
            }

            @Override
            public String getRemoteAddr() {
                return null;
            }

            @Override
            public String getRemoteHost() {
                return null;
            }

            @Override
            public void setAttribute(String s, Object o) {

            }

            @Override
            public void removeAttribute(String s) {

            }

            @Override
            public Locale getLocale() {
                return null;
            }

            @Override
            public Enumeration<Locale> getLocales() {
                return null;
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public RequestDispatcher getRequestDispatcher(String s) {
                return null;
            }

            @Override
            public String getRealPath(String s) {
                return null;
            }

            @Override
            public int getRemotePort() {
                return 0;
            }

            @Override
            public String getLocalName() {
                return null;
            }

            @Override
            public String getLocalAddr() {
                return null;
            }

            @Override
            public int getLocalPort() {
                return 0;
            }

            @Override
            public ServletContext getServletContext() {
                return null;
            }

            @Override
            public AsyncContext startAsync() throws IllegalStateException {
                return null;
            }

            @Override
            public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
                return null;
            }

            @Override
            public boolean isAsyncStarted() {
                return false;
            }

            @Override
            public boolean isAsyncSupported() {
                return false;
            }

            @Override
            public AsyncContext getAsyncContext() {
                return null;
            }

            @Override
            public DispatcherType getDispatcherType() {
                return null;
            }

            @Override
            public String getAuthType() {
                return null;
            }

            @Override
            public Cookie[] getCookies() {
                return new Cookie[0];
            }

            @Override
            public long getDateHeader(String s) {
                return 0;
            }

            @Override
            public String getHeader(String s) {
                return null;
            }

            @Override
            public Enumeration<String> getHeaders(String s) {
                return null;
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                return null;
            }

            @Override
            public int getIntHeader(String s) {
                return 0;
            }

            @Override
            public String getMethod() {
                return null;
            }

            @Override
            public String getPathInfo() {
                return null;
            }

            @Override
            public String getPathTranslated() {
                return null;
            }

            @Override
            public String getContextPath() {
                return null;
            }

            @Override
            public String getQueryString() {
                return null;
            }

            @Override
            public String getRemoteUser() {
                return null;
            }

            @Override
            public boolean isUserInRole(String s) {
                return false;
            }

            @Override
            public Principal getUserPrincipal() {
                return null;
            }

            @Override
            public String getRequestedSessionId() {
                return null;
            }

            @Override
            public String getRequestURI() {
                return null;
            }

            @Override
            public StringBuffer getRequestURL() {
                return null;
            }

            @Override
            public String getServletPath() {
                return null;
            }

            @Override
            public HttpSession getSession(boolean b) {
                return null;
            }

            @Override
            public HttpSession getSession() {
                return null;
            }

            @Override
            public String changeSessionId() {
                return null;
            }

            @Override
            public boolean isRequestedSessionIdValid() {
                return false;
            }

            @Override
            public boolean isRequestedSessionIdFromCookie() {
                return false;
            }

            @Override
            public boolean isRequestedSessionIdFromURL() {
                return false;
            }

            @Override
            public boolean isRequestedSessionIdFromUrl() {
                return false;
            }

            @Override
            public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException {
                return false;
            }

            @Override
            public void login(String s, String s1) throws ServletException {

            }

            @Override
            public void logout() throws ServletException {

            }

            @Override
            public Collection<Part> getParts() throws IOException, ServletException {
                return null;
            }

            @Override
            public Part getPart(String s) throws IOException, ServletException {
                return null;
            }

            @Override
            public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass) throws IOException, ServletException {
                return null;
            }
        }));
    }

    @Test
    void getEventById() {
    }

    @Test
    void updateEventAdmin() {
    }
}