package ru.practicum.ewm.mainservice.service.implementation;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.mainservice.dto.category.CategoryDto;
import ru.practicum.ewm.mainservice.dto.category.NewCategoryDto;
import ru.practicum.ewm.mainservice.exception.custom.ObjectNotFoundExceptionCust;
import ru.practicum.ewm.mainservice.mapper.CategoryMapper;
import ru.practicum.ewm.mainservice.model.Category;
import ru.practicum.ewm.mainservice.repository.JpaCategoryRepository;
import ru.practicum.ewm.mainservice.service.CategoryService;

@Service
@AllArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final JpaCategoryRepository jpaCategoryRepository;

    @Override
    public CategoryDto createCategory(NewCategoryDto newCategoryDto) {
        Category category = jpaCategoryRepository.save(CategoryMapper.newCategoryDtoToCategory(newCategoryDto));
        return CategoryMapper.categoryToCategoryDto(category);
    }

    @Override
    public CategoryDto updateCategory(Long catId, CategoryDto categoryDto) {
        Category category = checkCategoryById(catId);
        category.setName(categoryDto.getName());
        return CategoryMapper.categoryToCategoryDto(jpaCategoryRepository.save(category));
    }

    @Override
    public void deleteCategory(Long catId) {
        checkCategoryById(catId);
        try {
            jpaCategoryRepository.deleteById(catId);
        } catch (Exception e) {
            System.out.println("Exception e !!!!!!!!!!!!!!!!!!!! = " + e);
        }
    }

    private Category checkCategoryById(Long catId) {
        return jpaCategoryRepository.findById(catId).orElseThrow(() ->
                new ObjectNotFoundExceptionCust("Категории с id = " + catId + " не существует"));
    }
}
