package ru.practicum.ewm.mainservice.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.mainservice.dto.event.EventRequestParamsAdmin;
import ru.practicum.ewm.mainservice.dto.event.UpdateEventAdminRequest;
import ru.practicum.ewm.mainservice.exception.custom.ObjectNotFoundExceptionCust;
import ru.practicum.ewm.mainservice.service.EventService;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventControllerAdmin.class)
class EventControllerAdminTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    @SneakyThrows
    @Test
    void getEventsAdmin_correct() {
        mockMvc.perform(get("/admin/events")
                        .content(objectMapper.writeValueAsString(new EventRequestParamsAdmin()))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(eventService).getEventsAdmin(any(EventRequestParamsAdmin.class), anyInt(), anyInt());
    }

    @SneakyThrows
    @Test
    void getEventsAdmin_incorrectRequestParam_httpStatusBadRequest() {
        mockMvc.perform(get("/admin/events")
                        .content(objectMapper.writeValueAsString(new EventRequestParamsAdmin()))
                        .param("from", "-1")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).getEventsAdmin(any(EventRequestParamsAdmin.class), anyInt(), anyInt());
    }

    @SneakyThrows
    @Test
    void updateEventAdmin_correct() {
        mockMvc.perform(patch("/admin/events/{eventId}", 1L)
                        .content(objectMapper.writeValueAsString(new UpdateEventAdminRequest()))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(eventService).updateEventAdmin(anyLong(), any(UpdateEventAdminRequest.class));
    }

    @SneakyThrows
    @Test
    void updateEventAdmin_eventNotFound_httpStatusNotFound() {
        when(eventService.updateEventAdmin(anyLong(), any(UpdateEventAdminRequest.class)))
                .thenThrow(new ObjectNotFoundExceptionCust(""));

        mockMvc.perform(patch("/admin/events/{eventId}", 1L)
                        .content(objectMapper.writeValueAsString(new UpdateEventAdminRequest()))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}