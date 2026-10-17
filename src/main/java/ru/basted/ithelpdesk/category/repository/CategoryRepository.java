package ru.basted.ithelpdesk.category.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.basted.ithelpdesk.category.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByOrderByIdAsc();

    boolean existsByName(String name);
}