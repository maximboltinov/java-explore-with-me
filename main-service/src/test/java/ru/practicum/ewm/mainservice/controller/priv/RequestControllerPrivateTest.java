package ru.practicum.ewm.mainservice.controller.priv;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.mainservice.exception.custom.ConflictException;
import ru.practicum.ewm.mainservice.exception.custom.ObjectNotFoundExceptionCust;
import ru.practicum.ewm.mainservice.service.RequestService;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RequestControllerPrivate.class)
class RequestControllerPrivateTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RequestService requestService;

    @SneakyThrows
    @Test
    void create_whenIntegrityConditionViolated_httpStatusConflict() {
        when(requestService.create(1L, 1L))
                .thenThrow(new ConflictException(""));

        mockMvc.perform(post("/users/{userId}/requests", 1L)
                        .param("eventId", "1")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @SneakyThrows
    @Test
    void getRequests_incorrectRequest_httpStatusBadRequest() {
        mockMvc.perform(get("/users/{userId}/requests", 0L)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(requestService, never()).getRequests(anyLong());
    }

    @SneakyThrows
    @Test
    void cancelRequest_whenRequestNotFound_httpStatusNotFound() {
        when(requestService.cancelRequest(1L, 1L))
                .thenThrow(new ObjectNotFoundExceptionCust(""));

        mockMvc.perform(patch("/users/{userId}/requests/{requestId}/cancel", 1L, 1L)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}