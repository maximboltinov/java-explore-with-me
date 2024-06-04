package ru.practicum.ewm.mainservice.controller.priv;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.mainservice.dto.event.NewEventDto;
import ru.practicum.ewm.mainservice.dto.event.UpdateEventUserRequest;
import ru.practicum.ewm.mainservice.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.mainservice.enums.RequestStatus;
import ru.practicum.ewm.mainservice.exception.custom.ConflictException;
import ru.practicum.ewm.mainservice.exception.custom.IncorrectParametersException;
import ru.practicum.ewm.mainservice.exception.custom.ObjectNotFoundExceptionCust;
import ru.practicum.ewm.mainservice.service.EventService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventControllerPrivate.class)
class EventControllerPrivateTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    private final EasyRandom generator = new EasyRandom();

    @SneakyThrows
    @Test
    void createEvent_correct() {
        NewEventDto eventDto = generator.nextObject(NewEventDto.class);
        eventDto.setCategory(1L);
        eventDto.setEventDate(LocalDateTime.now().plusDays(5));
        eventDto.setParticipantLimit(0);

        mockMvc.perform(post("/users/{userId}/events", 1L)
                        .content(objectMapper.writeValueAsString(eventDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        verify(eventService).createEvent(anyLong(), any(NewEventDto.class));
    }

    @SneakyThrows
    @Test
    void createEvent_incorrectBody_httpStatusBadRequest() {
        NewEventDto eventDto = new NewEventDto();

        mockMvc.perform(post("/users/{userId}/events", 1L)
                        .content(objectMapper.writeValueAsString(eventDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).createEvent(anyLong(), any(NewEventDto.class));
    }

    @SneakyThrows
    @Test
    void createEvent_incorrectPathVariable_httpStatusBadRequest() {
        NewEventDto eventDto = generator.nextObject(NewEventDto.class);
        eventDto.setCategory(1L);
        eventDto.setEventDate(LocalDateTime.now().plusDays(5));
        eventDto.setParticipantLimit(0);

        mockMvc.perform(post("/users/{userId}/events", 0L)
                        .content(objectMapper.writeValueAsString(eventDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).createEvent(anyLong(), any(NewEventDto.class));
    }

    @SneakyThrows
    @Test
    void updateEvent_correct() {
        UpdateEventUserRequest updateEventUserRequest = generator.nextObject(UpdateEventUserRequest.class);
        updateEventUserRequest.setCategory(1L);
        updateEventUserRequest.setEventDate(LocalDateTime.now().plusDays(5));
        updateEventUserRequest.setParticipantLimit(0);

        mockMvc.perform(patch("/users/{userId}/events/{eventId}", 1L, 1L)
                        .content(objectMapper.writeValueAsString(updateEventUserRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(eventService).updateEvent(anyLong(), anyLong(), any(UpdateEventUserRequest.class));
    }

    @SneakyThrows
    @Test
    void updateEvent_whenNotFoundEventByEventIdAndOwnerId_httpStatusNotFound() {
        UpdateEventUserRequest updateEventUserRequest = generator.nextObject(UpdateEventUserRequest.class);
        updateEventUserRequest.setCategory(1L);
        updateEventUserRequest.setEventDate(LocalDateTime.now().plusDays(5));
        updateEventUserRequest.setParticipantLimit(0);

        when(eventService.updateEvent(anyLong(), anyLong(), any(UpdateEventUserRequest.class)))
                .thenThrow(new ObjectNotFoundExceptionCust(""));

        mockMvc.perform(patch("/users/{userId}/events/{eventId}", 1L, 1L)
                        .content(objectMapper.writeValueAsString(updateEventUserRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @SneakyThrows
    @Test
    void updateEvent_whenStatePublished_httpStatusConflict() {
        UpdateEventUserRequest updateEventUserRequest = generator.nextObject(UpdateEventUserRequest.class);
        updateEventUserRequest.setCategory(1L);
        updateEventUserRequest.setEventDate(LocalDateTime.now().plusDays(5));
        updateEventUserRequest.setParticipantLimit(0);

        when(eventService.updateEvent(anyLong(), anyLong(), any(UpdateEventUserRequest.class)))
                .thenThrow(new ConflictException(""));

        mockMvc.perform(patch("/users/{userId}/events/{eventId}", 1L, 1L)
                        .content(objectMapper.writeValueAsString(updateEventUserRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @SneakyThrows
    @Test
    void updateEvent_incorrectBody_httpBadRequest() {
        UpdateEventUserRequest updateEventUserRequest = generator.nextObject(UpdateEventUserRequest.class);
        updateEventUserRequest.setCategory(0L);

        mockMvc.perform(patch("/users/{userId}/events/{eventId}", 1L, 1L)
                        .content(objectMapper.writeValueAsString(updateEventUserRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).updateEvent(anyLong(), anyLong(), any(UpdateEventUserRequest.class));
    }

    @SneakyThrows
    @Test
    void getEventsByUserId_correct() {
        mockMvc.perform(get("/users/{userId}/events", 1L)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(eventService).getEventsByUserId(anyLong(), anyInt(), anyInt());
    }

    @SneakyThrows
    @Test
    void getEventsByUserId_userNotFound_httpStatusNotFound() {
        when(eventService.getEventsByUserId(anyLong(), anyInt(), anyInt()))
                .thenThrow(new ObjectNotFoundExceptionCust(""));

        mockMvc.perform(get("/users/{userId}/events", 1L)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @SneakyThrows
    @Test
    void getEventsByUserId_incorrectRequestParam_httpStatusBadRequest() {
        mockMvc.perform(get("/users/{userId}/events", 1L)
                        .param("from", "-1")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).getEventsByUserId(anyLong(), anyInt(), anyInt());
    }

    @SneakyThrows
    @Test
    void getEvent_correct() {
        mockMvc.perform(get("/users/{userId}/events/{eventId}", 1L, 1L)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(eventService).getEvent(1L, 1L);
    }

    @SneakyThrows
    @Test
    void getEvent_incorrectPathVariable_httpStatusBadRequest() {
        mockMvc.perform(get("/users/{userId}/events/{eventId}", 0L, 1L)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).getEvent(0L, 1L);
    }

    @SneakyThrows
    @Test
    void getRequestByEventAndOwner_correct() {
        mockMvc.perform(get("/users/{userId}/events/{eventId}/requests", 1L, 1L)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(eventService).getRequestByEventAndOwner(1L, 1L);
    }

    @SneakyThrows
    @Test
    void getRequestByEventAndOwner_incorrectPathVariable_httpStatusBadRequest() {
        mockMvc.perform(get("/users/{userId}/events/{eventId}/requests", 0L, 1L)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).getRequestByEventAndOwner(1L, 1L);
    }

    @SneakyThrows
    @Test
    void updateStatusRequestFromOwner_whenIncorrectStatus_httpStatusBadRequest() {
        EventRequestStatusUpdateRequest eventRequestStatusUpdateRequest = new EventRequestStatusUpdateRequest();
        eventRequestStatusUpdateRequest.setStatus(RequestStatus.CANCELED);
        eventRequestStatusUpdateRequest.setRequestIds(Set.of(1L, 2L, 3L));

        when(eventService.updateStatusRequest(1L, 1L, eventRequestStatusUpdateRequest))
                .thenThrow(new IncorrectParametersException(""));

        mockMvc.perform(patch("/users/{userId}/events/{eventId}/requests", 1L, 1L)
                        .content(objectMapper.writeValueAsString(eventRequestStatusUpdateRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @SneakyThrows
    @Test
    void updateStatusRequestFromOwner_whenIntegrityConditionViolated_httpStatusConflict() {
        EventRequestStatusUpdateRequest eventRequestStatusUpdateRequest = new EventRequestStatusUpdateRequest();
        eventRequestStatusUpdateRequest.setStatus(RequestStatus.CONFIRMED);
        eventRequestStatusUpdateRequest.setRequestIds(Set.of(1L, 2L, 3L));

        when(eventService.updateStatusRequest(1L, 1L, eventRequestStatusUpdateRequest))
                .thenThrow(new ConflictException(""));

        mockMvc.perform(patch("/users/{userId}/events/{eventId}/requests", 1L, 1L)
                        .content(objectMapper.writeValueAsString(eventRequestStatusUpdateRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }
}