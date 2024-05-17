package ru.practicum.ewm.stats.statsserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.stats.statsdto.EndpointHit;
import ru.practicum.ewm.stats.statsserver.service.StatService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StatController.class)
class StatControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    StatService statService;

    @SneakyThrows
    @Test
    void create_incorrectBody_exception() {
        final EndpointHit endpointHit = EndpointHit.builder()
                .app("some-app")
                .uri("some-uri")
                .ip("some-ip")
                .build();

        mockMvc.perform(post("/hit")
                        .content(objectMapper.writeValueAsString(endpointHit))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(statService, never()).create(any(EndpointHit.class));
    }

    @SneakyThrows
    @Test
    void getStats_withoutRequiredParameters_exception() {
        mockMvc.perform(get("/stats"))
                .andExpect(status().isBadRequest());

        verify(statService, never()).getStats(any(LocalDateTime.class), any(LocalDateTime.class),
                any(), any(Boolean.class));
    }

    @SneakyThrows
    @Test
    void getStats_incorrectFormatRequiredParameters_exception() {
        mockMvc.perform(get("/stats")
                        .param("start", "2023-10-10 15-15-15")
                        .param("end", "2023-10-20 15-15-15"))
                .andExpect(status().isBadRequest());

        verify(statService, never()).getStats(any(LocalDateTime.class), any(LocalDateTime.class),
                any(), any(Boolean.class));
    }
}