package ru.practicum.ewm.mainservice.controller.pub;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.mainservice.exception.custom.ObjectNotFoundExceptionCust;
import ru.practicum.ewm.mainservice.service.CompilationService;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CompilationControllerPublic.class)
class CompilationControllerPublicTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompilationService compilationService;

    @SneakyThrows
    @Test
    void getCompilations_incorrectRequest_httpStatusBadRequest() {
        mockMvc.perform(get("/compilations")
                        .param("from", "-1")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(compilationService, never()).getCompilations(anyBoolean(), anyInt(), anyInt());
    }

    @SneakyThrows
    @Test
    void getCompilationsById_incorrectRequest_httpStatusBadRequest() {
        mockMvc.perform(get("/compilations/{compId}", 0L)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(compilationService, never()).getCompilationsById(anyLong());
    }

    @SneakyThrows
    @Test
    void getCompilationsById_incorrectRequestId_httpStatusNotFound() {
        when(compilationService.getCompilationsById(anyLong()))
                .thenThrow(new ObjectNotFoundExceptionCust(""));

        mockMvc.perform(get("/compilations/{compId}", 1L)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}