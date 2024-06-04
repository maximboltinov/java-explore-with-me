package ru.practicum.ewm.mainservice.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.mainservice.dto.category.CategoryDto;
import ru.practicum.ewm.mainservice.dto.category.NewCategoryDto;
import ru.practicum.ewm.mainservice.service.CategoryService;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CategoryControllerAdmin.class)
class CategoryControllerAdminTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @SneakyThrows
    @Test
    void createCategory_correct() {
        NewCategoryDto newCategoryDto = new NewCategoryDto("categoryName");

        when(categoryService.createCategory(newCategoryDto))
                .thenReturn(new CategoryDto(1L, "categoryName"));

        mockMvc.perform(post("/admin/categories")
                        .content(objectMapper.writeValueAsString(newCategoryDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1L), Long.class))
                .andExpect(jsonPath("$.name", is("categoryName"), String.class));

        verify(categoryService).createCategory((newCategoryDto));
    }

    @SneakyThrows
    @Test
    void createCategory_categoryNameIsNull_httpStatusBadRequest() {
        NewCategoryDto newCategoryDto = new NewCategoryDto(null);

        mockMvc.perform(post("/admin/categories")
                        .content(objectMapper.writeValueAsString(newCategoryDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).createCategory((newCategoryDto));
    }

    @SneakyThrows
    @Test
    void createCategory_categoryWithBusyName_httpStatusConflict() {
        NewCategoryDto newCategoryDto = new NewCategoryDto("categoryName");
        when(categoryService.createCategory(any(NewCategoryDto.class)))
                .thenThrow(new DataIntegrityViolationException(""));

        mockMvc.perform(post("/admin/categories")
                        .content(objectMapper.writeValueAsString(newCategoryDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @SneakyThrows
    @Test
    void updateCategory_correct() {
        CategoryDto categoryDto = new CategoryDto(1L, "categoryName");
        when(categoryService.updateCategory(1L, categoryDto))
                .thenReturn(categoryDto);

        mockMvc.perform(patch("/admin/categories/{catId}", 1)
                        .content(objectMapper.writeValueAsString(categoryDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1L), Long.class))
                .andExpect(jsonPath("$.name", is(categoryDto.getName())));

        verify(categoryService).updateCategory(1L, categoryDto);
    }

    @SneakyThrows
    @Test
    void updateCategory_incorrectCategoryName_httpStatusBadRequest() {
        CategoryDto categoryDto = new CategoryDto(null, "  ");
        when(categoryService.updateCategory(1L, categoryDto))
                .thenThrow(new DataIntegrityViolationException(""));

        mockMvc.perform(patch("/admin/categories/{catId}", 1)
                        .content(objectMapper.writeValueAsString(categoryDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).updateCategory(anyLong(), any(CategoryDto.class));
    }

    @SneakyThrows
    @Test
    void updateCategory_categoryWithBusyName_httpStatusConflict() {
        CategoryDto categoryDto = new CategoryDto(null, "categoryName");
        when(categoryService.updateCategory(1L, categoryDto))
                .thenThrow(new DataIntegrityViolationException(""));

        mockMvc.perform(patch("/admin/categories/{catId}", 1)
                        .content(objectMapper.writeValueAsString(categoryDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @SneakyThrows
    @Test
    void deleteCategory_correct() {
        mockMvc.perform(delete("/admin/categories/{catId}", 1)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(categoryService).deleteCategory(1L);
    }
}