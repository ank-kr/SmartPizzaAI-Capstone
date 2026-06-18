package com.smartpizza.core.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.smartpizza.core.dto.CategoryRequest;
import com.smartpizza.core.dto.CategoryResponse;
import com.smartpizza.core.dto.MenuItemRequest;
import com.smartpizza.core.dto.MenuItemResponse;
import com.smartpizza.core.entity.Category;
import com.smartpizza.core.entity.MenuItem;
import com.smartpizza.core.repository.CategoryRepository;
import com.smartpizza.core.repository.MenuItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuService {

	// repository responsible for category CRUD operation
	private final CategoryRepository categoryRepository;

	// repository responsible for menuitem crud operation
	private final MenuItemRepository menuItemRepository;

	public CategoryResponse addCategory(CategoryRequest request) {

		log.info("Creating category. name={}", request.getName());

		// Prevent duplicate category names (case-insensitive).
		if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
			log.warn("Category creation failed because category already exists. name={}", request.getName());
			throw new RuntimeException("Category already exists with name: " + request.getName());
		}

		// Create new category with active status enabled by default.
		Category category = Category.builder().name(request.getName()).description(request.getDescription())
				.active(true).build();

		Category savedCategory = categoryRepository.save(category);

		log.info("Category created successfully. categoryId={}, name={}, active={}", savedCategory.getId(),
				savedCategory.getName(), savedCategory.getActive());

		return mapCategoryToResponse(savedCategory);
	}

	public List<CategoryResponse> getAllActiveCategories() {

		log.info("Fetching all active categories");

		// Fetch only active categories for menu display.
		List<CategoryResponse> activeCategories = categoryRepository.findByActiveTrue().stream()
				.map(this::mapCategoryToResponse).toList();

		log.info("Active categories fetched successfully. count={}", activeCategories.size());

		return activeCategories;
	}

	public MenuItemResponse addMenuItem(MenuItemRequest request) {

		log.info("Creating menu item. name={}, categoryId={}", request.getName(), request.getCategoryId());

		// Category must exist before creating a menu item.
		Category category = categoryRepository.findById(request.getCategoryId()).orElseThrow(() -> {
			log.warn("Menu item creation failed because category was not found. categoryId={}",
					request.getCategoryId());
			return new RuntimeException("Category not found with id: " + request.getCategoryId());
		});

		// Map request -> entity with default values for optional fields.
		MenuItem menuItem = MenuItem.builder().name(request.getName()).description(request.getDescription())
				.price(request.getPrice()).imageUrl(request.getImageUrl()).size(request.getSize())
				.crustType(request.getCrustType()).spiceLevel(request.getSpiceLevel()).veg(request.getVeg())

				// Default availability = true if not provided.
				.available(request.getAvailable() == null ? true : request.getAvailable())

				// Default rating used for newly added items.
				.rating(request.getRating() == null ? 4.5 : request.getRating())

				// Link menu item to category.
				.category(category).build();

		MenuItem savedMenuItem = menuItemRepository.save(menuItem);

		log.info("Menu item created successfully. menuItemId={}, name={}, categoryId={}, available={}",
				savedMenuItem.getId(), savedMenuItem.getName(), category.getId(), savedMenuItem.getAvailable());

		return mapMenuItemToResponse(savedMenuItem);
	}

	public List<MenuItemResponse> getAllAvailableMenuItems() {

		log.info("Fetching all available menu items");

		// Fetch only menu items available for ordering.
		List<MenuItemResponse> availableMenuItems = menuItemRepository.findByAvailableTrue().stream()
				.map(this::mapMenuItemToResponse).toList();

		log.info("Available menu items fetched successfully. count={}", availableMenuItems.size());

		return availableMenuItems;
	}

	public MenuItemResponse getMenuItemById(Long id) {

		log.info("Fetching menu item by id={}", id);

		// Fetch specific menu item or throw if not found.
		MenuItem menuItem = menuItemRepository.findById(id).orElseThrow(() -> {
			log.warn("Menu item not found. menuItemId={}", id);
			return new RuntimeException("Menu item not found with id: " + id);
		});

		log.info("Menu item fetched successfully. menuItemId={}, name={}", menuItem.getId(), menuItem.getName());

		return mapMenuItemToResponse(menuItem);
	}

	public List<MenuItemResponse> getMenuItemsByCategory(Long categoryId) {

		log.info("Fetching available menu items by categoryId={}", categoryId);

		// Filter menu by category and availability.
		List<MenuItemResponse> menuItems = menuItemRepository.findByCategoryIdAndAvailableTrue(categoryId).stream()
				.map(this::mapMenuItemToResponse).toList();

		log.info("Available menu items fetched by category successfully. categoryId={}, count={}", categoryId,
				menuItems.size());

		return menuItems;
	}

	public List<MenuItemResponse> getMenuItemsByVegType(Boolean veg) {

		log.info("Fetching available menu items by vegType={}", veg);

		// Filter menu based on veg/non-veg type and availability.
		List<MenuItemResponse> menuItems = menuItemRepository.findByVegAndAvailableTrue(veg).stream()
				.map(this::mapMenuItemToResponse).toList();

		log.info("Available menu items fetched by veg type successfully. vegType={}, count={}", veg, menuItems.size());

		return menuItems;
	}

	private CategoryResponse mapCategoryToResponse(Category category) {

		// Entity -> DTO mapping for category response.
		return CategoryResponse.builder().id(category.getId()).name(category.getName())
				.description(category.getDescription()).active(category.getActive()).build();
	}

	private MenuItemResponse mapMenuItemToResponse(MenuItem menuItem) {

		// Entity -> DTO mapping for menu item including category details for frontend
		// display.
		return MenuItemResponse.builder().id(menuItem.getId()).name(menuItem.getName())
				.description(menuItem.getDescription()).price(menuItem.getPrice()).imageUrl(menuItem.getImageUrl())
				.size(menuItem.getSize()).crustType(menuItem.getCrustType()).spiceLevel(menuItem.getSpiceLevel())
				.veg(menuItem.getVeg()).available(menuItem.getAvailable()).rating(menuItem.getRating())

				// Include category details to avoid extra API call from frontend.
				.categoryId(menuItem.getCategory().getId()).categoryName(menuItem.getCategory().getName()).build();
	}
}