package ru.practicum.ewm.mainservice.controller.admin;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.mainservice.dto.category.CategoryDto;
import ru.practicum.ewm.mainservice.dto.category.NewCategoryDto;
import ru.practicum.ewm.mainservice.service.CategoryService;

import javax.validation.Valid;
import javax.validation.constraints.Positive;

@RestController
@RequestMapping("/admin/categories")
@AllArgsConstructor
@Slf4j
@Validated
public class CategoryControllerAdmin {
    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@RequestBody @Valid NewCategoryDto newCategoryDto) {
        log.info("Запрос POST /admin/categories {}", newCategoryDto);
        CategoryDto categoryDto = categoryService.createCategory(newCategoryDto);
        log.info("Ответ POST /admin/categories {}", categoryDto);
        return categoryDto;
    }

    @PatchMapping("/{catId}")
    @ResponseStatus(HttpStatus.OK)
    public CategoryDto updateCategory(@PathVariable @Positive Long catId,
                                      @RequestBody @Valid CategoryDto categoryDto) {
        log.info("Запрос PATCH /admin/categories {}", categoryDto);
        CategoryDto answercategoryDto = categoryService.updateCategory(catId, categoryDto);
        log.info("Ответ PATCH /admin/categories {}", answercategoryDto);
        return answercategoryDto;
    }

    @DeleteMapping("/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long catId) {
        log.info("Запрос DELETE /admin/categories/{}", catId);
        categoryService.deleteCategory(catId);
        log.info("Ответ DELETE /admin/categories/{} {}", catId, HttpStatus.NO_CONTENT);
    }
}
