package ru.practicum.ewm.mainservice.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.mainservice.dto.category.CategoryDto;
import ru.practicum.ewm.mainservice.dto.category.NewCategoryDto;
import ru.practicum.ewm.mainservice.model.Category;

@UtilityClass
public class CategoryMapper {
    public Category categoryDtoToCategory(CategoryDto categoryDto) {
        return Category.builder()
                .name(categoryDto.getName())
                .build();
    }

    public Category newCategoryDtoToCategory(NewCategoryDto newCategoryDto) {
        return Category.builder()
                .name(newCategoryDto.getName())
                .build();
    }

    public CategoryDto categoryToCategoryDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}
