package ru.practicum.ewm.mainservice.controller.pub;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.mainservice.dto.event.EventRequestParams;
import ru.practicum.ewm.mainservice.exception.custom.ObjectNotFoundExceptionCust;
import ru.practicum.ewm.mainservice.service.EventService;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventControllerPublic.class)
class EventControllerPublicTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    private final EasyRandom generator = new EasyRandom();

    @SneakyThrows
    @Test
    void getEvents_incorrectRequest_httpStatusBadRequest() {
        EventRequestParams eventRequestParams = generator.nextObject(EventRequestParams.class);

        mockMvc.perform(get("/events")
                        .param("from", "-1")
                        .content(objectMapper.writeValueAsString(eventRequestParams))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(eventService, never())
                .getFilteredEvents(any(EventRequestParams.class), anyInt(), anyInt(), any(HttpServletRequest.class));
    }

    @SneakyThrows
    @Test
    void getEventById_incorrectRequest_httpStatusBadRequest() {
        mockMvc.perform(get("/events/{eventId}", 0L)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).getEventById(anyLong(), any(HttpServletRequest.class));
    }

    @SneakyThrows
    @Test
    void getEventById_incorrectEventId_httpStatusNotFound() {
        when(eventService.getEventById(anyLong(), any(HttpServletRequest.class)))
                .thenThrow(new ObjectNotFoundExceptionCust(""));

        mockMvc.perform(get("/events/{eventId}", 0L)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}