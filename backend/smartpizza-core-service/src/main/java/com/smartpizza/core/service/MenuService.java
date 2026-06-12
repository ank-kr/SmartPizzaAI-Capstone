package com.smartpizza.core.service;

import com.smartpizza.core.dto.CategoryRequest;
import com.smartpizza.core.dto.CategoryResponse;
import com.smartpizza.core.dto.MenuItemRequest;
import com.smartpizza.core.dto.MenuItemResponse;
import com.smartpizza.core.entity.Category;
import com.smartpizza.core.entity.MenuItem;
import com.smartpizza.core.repository.CategoryRepository;
import com.smartpizza.core.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final CategoryRepository categoryRepository;

    private final MenuItemRepository menuItemRepository;

    public CategoryResponse addCategory(CategoryRequest request) {

        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new RuntimeException("Category already exists with name: " + request.getName());
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .active(true)
                .build();

        Category savedCategory = categoryRepository.save(category);

        return mapCategoryToResponse(savedCategory);
    }

    public List<CategoryResponse> getAllActiveCategories() {
        return categoryRepository.findByActiveTrue()
                .stream()
                .map(this::mapCategoryToResponse)
                .toList();
    }

    public MenuItemResponse addMenuItem(MenuItemRequest request) {

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));

        MenuItem menuItem = MenuItem.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .size(request.getSize())
                .crustType(request.getCrustType())
                .spiceLevel(request.getSpiceLevel())
                .veg(request.getVeg())
                .available(request.getAvailable() == null ? true : request.getAvailable())
                .rating(request.getRating() == null ? 4.5 : request.getRating())
                .category(category)
                .build();

        MenuItem savedMenuItem = menuItemRepository.save(menuItem);

        return mapMenuItemToResponse(savedMenuItem);
    }

    public List<MenuItemResponse> getAllAvailableMenuItems() {
        return menuItemRepository.findByAvailableTrue()
                .stream()
                .map(this::mapMenuItemToResponse)
                .toList();
    }

    public MenuItemResponse getMenuItemById(Long id) {
        MenuItem menuItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu item not found with id: " + id));

        return mapMenuItemToResponse(menuItem);
    }

    public List<MenuItemResponse> getMenuItemsByCategory(Long categoryId) {
        return menuItemRepository.findByCategoryIdAndAvailableTrue(categoryId)
                .stream()
                .map(this::mapMenuItemToResponse)
                .toList();
    }

    public List<MenuItemResponse> getMenuItemsByVegType(Boolean veg) {
        return menuItemRepository.findByVegAndAvailableTrue(veg)
                .stream()
                .map(this::mapMenuItemToResponse)
                .toList();
    }

    private CategoryResponse mapCategoryToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .active(category.getActive())
                .build();
    }

    private MenuItemResponse mapMenuItemToResponse(MenuItem menuItem) {
        return MenuItemResponse.builder()
                .id(menuItem.getId())
                .name(menuItem.getName())
                .description(menuItem.getDescription())
                .price(menuItem.getPrice())
                .imageUrl(menuItem.getImageUrl())
                .size(menuItem.getSize())
                .crustType(menuItem.getCrustType())
                .spiceLevel(menuItem.getSpiceLevel())
                .veg(menuItem.getVeg())
                .available(menuItem.getAvailable())
                .rating(menuItem.getRating())
                .categoryId(menuItem.getCategory().getId())
                .categoryName(menuItem.getCategory().getName())
                .build();
    }
}