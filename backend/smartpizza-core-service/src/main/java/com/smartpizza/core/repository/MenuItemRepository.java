package com.smartpizza.core.repository;

import com.smartpizza.core.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByAvailableTrue();

    List<MenuItem> findByCategoryIdAndAvailableTrue(Long categoryId);

    List<MenuItem> findByVegAndAvailableTrue(Boolean veg);
}