package ru.practicum.ewm.mainservice.service.implementation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.mainservice.dto.category.CategoryDto;
import ru.practicum.ewm.mainservice.dto.category.NewCategoryDto;
import ru.practicum.ewm.mainservice.exception.custom.ObjectNotFoundExceptionCust;
import ru.practicum.ewm.mainservice.mapper.CategoryMapper;
import ru.practicum.ewm.mainservice.model.Category;
import ru.practicum.ewm.mainservice.repository.JpaCategoryRepository;
import ru.practicum.ewm.mainservice.service.CategoryService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {
    private CategoryService categoryService;

    @Mock
    private JpaCategoryRepository jpaCategoryRepository;

    @Captor
    private ArgumentCaptor<Category> categoryArgumentCaptor;

    @BeforeEach
    public void setUp() {
        categoryService = new CategoryServiceImpl(jpaCategoryRepository);
    }

    @Test
    void createCategory_duplicationOfCategories_DataIntegrityViolationException() {
        NewCategoryDto newCategoryDto = new NewCategoryDto("category");

        when(jpaCategoryRepository.save(CategoryMapper.newCategoryDtoToCategory(newCategoryDto)))
                .thenThrow(new DataIntegrityViolationException(""));

        assertThrows(DataIntegrityViolationException.class, () -> categoryService.createCategory(newCategoryDto));
    }

    @Test
    void createCategory_correct_CategoryDto() {
        NewCategoryDto newCategoryDto = new NewCategoryDto("category");

        when(jpaCategoryRepository.save(any(Category.class)))
                .thenAnswer(invocationOnMock -> {
                            Category category = invocationOnMock.getArgument(0, Category.class);
                            category.setId(1L);
                            return category;
                        }
                );

        CategoryDto categoryDto = categoryService.createCategory(newCategoryDto);

        assertEquals(1L, categoryDto.getId());
        assertEquals(newCategoryDto.getName(), categoryDto.getName());
    }

    @Test
    void updateCategory_whenCategoryAbsent_ObjectNotFoundExceptionCust() {
        when(jpaCategoryRepository.findById(anyLong()))
                .thenReturn(Optional.empty());

        assertThrows(ObjectNotFoundExceptionCust.class,
                () -> categoryService.updateCategory(1L, new CategoryDto()));

        verify(jpaCategoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategory_correct_CategoryDto() {
        Category oldName = Category.builder().id(1L).name("old name").build();
        Category newName = Category.builder().id(1L).name("new name").build();
        CategoryDto categoryDto = CategoryDto.builder().name("new name").build();

        when(jpaCategoryRepository.findById(1L))
                .thenReturn(Optional.of(oldName));
        when(jpaCategoryRepository.save(any()))
                .thenReturn(newName);

        CategoryDto categoryDtoNew = categoryService.updateCategory(1L, categoryDto);

        verify(jpaCategoryRepository).save(categoryArgumentCaptor.capture());
        Category value = categoryArgumentCaptor.getValue();

        assertEquals(newName, value);
        assertEquals("new name", categoryDtoNew.getName());
    }

    @Test
    void deleteCategory_whenCategoryAbsent_ObjectNotFoundExceptionCust() {
        when(jpaCategoryRepository.findById(anyLong()))
                .thenReturn(Optional.empty());

        assertThrows(ObjectNotFoundExceptionCust.class,
                () -> categoryService.deleteCategory(1L));

        verify(jpaCategoryRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteCategory_correct() {
        when(jpaCategoryRepository.findById(anyLong()))
                .thenReturn(Optional.of(new Category()));

        categoryService.deleteCategory(1L);

        verify(jpaCategoryRepository).deleteById(1L);
    }

    @Test
    void getCategories_categoriesNotFound_emptyList() {
        when(jpaCategoryRepository.findAll(any(Pageable.class)))
                .thenReturn(Page.empty());

        List<CategoryDto> list = categoryService.getCategories(0, 10);

        assertTrue(list.isEmpty());
    }

    @Test
    void getCategoryById() {
        when(jpaCategoryRepository.findById(anyLong()))
                .thenReturn(Optional.empty());

        assertThrows(ObjectNotFoundExceptionCust.class, () -> categoryService.getCategoryById(1L));
    }

    @Test
    void checkCategoryById_whenCategoryAbsent_ObjectNotFoundExceptionCust() {
        when(jpaCategoryRepository.findById(anyLong()))
                .thenReturn(Optional.empty());

        assertThrows(ObjectNotFoundExceptionCust.class, () -> categoryService.checkCategoryById(anyLong()));
    }

    @Test
    void checkCategoryById_whenCategoryAvailable_Category() {
        Category category = Category.builder().name("category").id(1L).build();
        when(jpaCategoryRepository.findById(anyLong()))
                .thenReturn(Optional.of(category));

        Category categoryResult = categoryService.checkCategoryById(anyLong());

        assertEquals(category, categoryResult);
    }
}