package ru.practicum.ewm.mainservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.practicum.ewm.mainservice.model.Category;

public interface JpaCategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {
}
