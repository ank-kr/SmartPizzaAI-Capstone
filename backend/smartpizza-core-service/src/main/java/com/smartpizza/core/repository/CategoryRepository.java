package com.smartpizza.core.repository;

import com.smartpizza.core.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByActiveTrue();

    boolean existsByNameIgnoreCase(String name);
}