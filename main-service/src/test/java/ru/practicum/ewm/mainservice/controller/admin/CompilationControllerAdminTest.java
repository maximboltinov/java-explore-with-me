package ru.practicum.ewm.mainservice.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.mainservice.dto.compilation.CompilationDto;
import ru.practicum.ewm.mainservice.dto.compilation.NewCompilationDto;
import ru.practicum.ewm.mainservice.dto.compilation.UpdateCompilationRequest;
import ru.practicum.ewm.mainservice.dto.event.EventShortDto;
import ru.practicum.ewm.mainservice.exception.custom.ObjectNotFoundExceptionCust;
import ru.practicum.ewm.mainservice.service.CompilationService;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CompilationControllerAdmin.class)
class CompilationControllerAdminTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompilationService compilationService;

    @SneakyThrows
    @Test
    void createCompilation_correct() {
        NewCompilationDto newCompilationDto =
                new NewCompilationDto(List.of(1L), false, "compilation title");

        CompilationDto compilationDto =
                new CompilationDto(1L, List.of(new EventShortDto()), false, "compilation title");

        when(compilationService.createCompilation(newCompilationDto))
                .thenReturn(compilationDto);

        mockMvc.perform(post("/admin/compilations")
                        .content(objectMapper.writeValueAsString(newCompilationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1L), Long.class))
                .andExpect(jsonPath("$.pinned", is(false), Boolean.class))
                .andExpect(jsonPath("$.title", is("compilation title"), String.class));

        verify(compilationService).createCompilation(newCompilationDto);
    }

    @SneakyThrows
    @Test
    void createCompilation_compilationTitleIsNull_httpStatusBadRequest() {
        NewCompilationDto newCompilationDto = new NewCompilationDto(List.of(), false, null);

        mockMvc.perform(post("/admin/compilations")
                        .content(objectMapper.writeValueAsString(newCompilationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(compilationService, never()).createCompilation(newCompilationDto);
    }

    @SneakyThrows
    @Test
    void updateCompilation_correct() {
        UpdateCompilationRequest update = new UpdateCompilationRequest(null, false, "update title");

        mockMvc.perform(patch("/admin/compilations/{compId}", 1L)
                        .content(objectMapper.writeValueAsString(update))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(compilationService).updateCompilation(1L, update);
    }

    @SneakyThrows
    @Test
    void updateCompilation_incorrectCompilationId_httpStatusBadRequest() {
        UpdateCompilationRequest update = new UpdateCompilationRequest(null, false, "update title");

        mockMvc.perform(patch("/admin/compilations/{compId}", 0L)
                        .content(objectMapper.writeValueAsString(update))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(compilationService, never()).updateCompilation(anyLong(), any(UpdateCompilationRequest.class));
    }

    @SneakyThrows
    @Test
    void updateCompilation_notFoundCompilation_httpStatusNotFound() {
        UpdateCompilationRequest update = new UpdateCompilationRequest(null, false, "update title");

        when(compilationService.updateCompilation(anyLong(), any(UpdateCompilationRequest.class)))
                .thenThrow(new ObjectNotFoundExceptionCust(""));

        mockMvc.perform(patch("/admin/compilations/{compId}", 1L)
                        .content(objectMapper.writeValueAsString(update))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @SneakyThrows
    @Test
    void deleteCompilation_correct() {
        mockMvc.perform(delete("/admin/compilations/{compId}", 1L)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(compilationService).deleteCompilation(1L);
    }

    @SneakyThrows
    @Test
    void deleteCompilation_incorrectCompilationId_httpStatusBadRequest() {
        mockMvc.perform(delete("/admin/compilations/{compId}", 0L)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(compilationService, never()).deleteCompilation(anyLong());
    }

    @SneakyThrows
    @Test
    void deleteCompilation_notFoundCompilation_httpStatusNotFound() {
        doThrow(new ObjectNotFoundExceptionCust("")).when(compilationService).deleteCompilation(anyLong());

        mockMvc.perform(delete("/admin/compilations/{compId}", 1L)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}