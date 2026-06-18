package com.smartpizza.core.service;

import com.smartpizza.core.dto.CategoryRequest;
import com.smartpizza.core.dto.CategoryResponse;
import com.smartpizza.core.dto.MenuItemRequest;
import com.smartpizza.core.dto.MenuItemResponse;
import com.smartpizza.core.entity.Category;
import com.smartpizza.core.entity.MenuItem;
import com.smartpizza.core.repository.CategoryRepository;
import com.smartpizza.core.repository.MenuItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @InjectMocks
    private MenuService menuService;

    @Test
    void addCategory_ShouldCreateCategorySuccessfully() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Veg Pizza")
                .description("Fresh vegetarian pizzas")
                .build();

        Category savedCategory = Category.builder()
                .id(1L)
                .name("Veg Pizza")
                .description("Fresh vegetarian pizzas")
                .active(true)
                .build();

        when(categoryRepository.existsByNameIgnoreCase("Veg Pizza")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        CategoryResponse response = menuService.addCategory(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Veg Pizza", response.getName());
        assertEquals("Fresh vegetarian pizzas", response.getDescription());
        assertTrue(response.getActive());

        verify(categoryRepository, times(1)).existsByNameIgnoreCase("Veg Pizza");
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    void addCategory_ShouldThrowException_WhenCategoryAlreadyExists() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Veg Pizza")
                .description("Fresh vegetarian pizzas")
                .build();

        when(categoryRepository.existsByNameIgnoreCase("Veg Pizza")).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> menuService.addCategory(request)
        );

        assertEquals("Category already exists with name: Veg Pizza", exception.getMessage());

        verify(categoryRepository, times(1)).existsByNameIgnoreCase("Veg Pizza");
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void getAllActiveCategories_ShouldReturnActiveCategories() {
        Category category1 = Category.builder()
                .id(1L)
                .name("Veg Pizza")
                .description("Fresh vegetarian pizzas")
                .active(true)
                .build();

        Category category2 = Category.builder()
                .id(2L)
                .name("Beverages")
                .description("Cold drinks and beverages")
                .active(true)
                .build();

        when(categoryRepository.findByActiveTrue()).thenReturn(List.of(category1, category2));

        List<CategoryResponse> responses = menuService.getAllActiveCategories();

        assertNotNull(responses);
        assertEquals(2, responses.size());

        assertEquals(1L, responses.get(0).getId());
        assertEquals("Veg Pizza", responses.get(0).getName());
        assertTrue(responses.get(0).getActive());

        assertEquals(2L, responses.get(1).getId());
        assertEquals("Beverages", responses.get(1).getName());
        assertTrue(responses.get(1).getActive());

        verify(categoryRepository, times(1)).findByActiveTrue();
    }

    @Test
    void addMenuItem_ShouldCreateMenuItemSuccessfully() {
        Category category = createCategory();

        MenuItemRequest request = MenuItemRequest.builder()
                .name("Farmhouse Pizza")
                .description("Loaded with onion, capsicum and mushrooms")
                .price(BigDecimal.valueOf(399))
                .imageUrl("/images/farmhouse.jpg")
                .veg(true)
                .available(true)
                .rating(4.7)
                .categoryId(1L)
                .build();

        MenuItem savedMenuItem = MenuItem.builder()
                .id(10L)
                .name("Farmhouse Pizza")
                .description("Loaded with onion, capsicum and mushrooms")
                .price(BigDecimal.valueOf(399))
                .imageUrl("/images/farmhouse.jpg")
                .veg(true)
                .available(true)
                .rating(4.7)
                .category(category)
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(menuItemRepository.save(any(MenuItem.class))).thenReturn(savedMenuItem);

        MenuItemResponse response = menuService.addMenuItem(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Farmhouse Pizza", response.getName());
        assertEquals("Loaded with onion, capsicum and mushrooms", response.getDescription());
        assertEquals(0, BigDecimal.valueOf(399).compareTo(response.getPrice()));
        assertEquals("/images/farmhouse.jpg", response.getImageUrl());
        assertTrue(response.getVeg());
        assertTrue(response.getAvailable());
        assertEquals(4.7, response.getRating());
        assertEquals(1L, response.getCategoryId());
        assertEquals("Veg Pizza", response.getCategoryName());

        verify(categoryRepository, times(1)).findById(1L);
        verify(menuItemRepository, times(1)).save(any(MenuItem.class));
    }

    @Test
    void addMenuItem_ShouldSetDefaultAvailableAndRating_WhenValuesAreNull() {
        Category category = createCategory();

        MenuItemRequest request = MenuItemRequest.builder()
                .name("Margherita Pizza")
                .description("Classic cheese pizza")
                .price(BigDecimal.valueOf(299))
                .imageUrl("/images/margherita.jpg")
                .veg(true)
                .available(null)
                .rating(null)
                .categoryId(1L)
                .build();

        MenuItem savedMenuItem = MenuItem.builder()
                .id(11L)
                .name("Margherita Pizza")
                .description("Classic cheese pizza")
                .price(BigDecimal.valueOf(299))
                .imageUrl("/images/margherita.jpg")
                .veg(true)
                .available(true)
                .rating(4.5)
                .category(category)
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(menuItemRepository.save(any(MenuItem.class))).thenReturn(savedMenuItem);

        MenuItemResponse response = menuService.addMenuItem(request);

        assertNotNull(response);
        assertEquals(11L, response.getId());
        assertEquals("Margherita Pizza", response.getName());
        assertTrue(response.getAvailable());
        assertEquals(4.5, response.getRating());

        verify(categoryRepository, times(1)).findById(1L);
        verify(menuItemRepository, times(1)).save(any(MenuItem.class));
    }

    @Test
    void addMenuItem_ShouldThrowException_WhenCategoryNotFound() {
        MenuItemRequest request = MenuItemRequest.builder()
                .name("Farmhouse Pizza")
                .description("Loaded with vegetables")
                .price(BigDecimal.valueOf(399))
                .veg(true)
                .categoryId(99L)
                .build();

        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> menuService.addMenuItem(request)
        );

        assertEquals("Category not found with id: 99", exception.getMessage());

        verify(categoryRepository, times(1)).findById(99L);
        verify(menuItemRepository, never()).save(any(MenuItem.class));
    }

    @Test
    void getAllAvailableMenuItems_ShouldReturnAvailableMenuItems() {
        Category category = createCategory();

        MenuItem item1 = MenuItem.builder()
                .id(10L)
                .name("Farmhouse Pizza")
                .description("Loaded with vegetables")
                .price(BigDecimal.valueOf(399))
                .imageUrl("/images/farmhouse.jpg")
                .veg(true)
                .available(true)
                .rating(4.7)
                .category(category)
                .build();

        MenuItem item2 = MenuItem.builder()
                .id(11L)
                .name("Margherita Pizza")
                .description("Classic cheese pizza")
                .price(BigDecimal.valueOf(299))
                .imageUrl("/images/margherita.jpg")
                .veg(true)
                .available(true)
                .rating(4.5)
                .category(category)
                .build();

        when(menuItemRepository.findByAvailableTrue()).thenReturn(List.of(item1, item2));

        List<MenuItemResponse> responses = menuService.getAllAvailableMenuItems();

        assertNotNull(responses);
        assertEquals(2, responses.size());

        assertEquals("Farmhouse Pizza", responses.get(0).getName());
        assertEquals("Margherita Pizza", responses.get(1).getName());

        verify(menuItemRepository, times(1)).findByAvailableTrue();
    }

    @Test
    void getMenuItemById_ShouldReturnMenuItemSuccessfully() {
        Category category = createCategory();

        MenuItem menuItem = MenuItem.builder()
                .id(10L)
                .name("Farmhouse Pizza")
                .description("Loaded with vegetables")
                .price(BigDecimal.valueOf(399))
                .imageUrl("/images/farmhouse.jpg")
                .veg(true)
                .available(true)
                .rating(4.7)
                .category(category)
                .build();

        when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem));

        MenuItemResponse response = menuService.getMenuItemById(10L);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Farmhouse Pizza", response.getName());
        assertEquals(1L, response.getCategoryId());
        assertEquals("Veg Pizza", response.getCategoryName());

        verify(menuItemRepository, times(1)).findById(10L);
    }

    @Test
    void getMenuItemById_ShouldThrowException_WhenMenuItemNotFound() {
        when(menuItemRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> menuService.getMenuItemById(99L)
        );

        assertEquals("Menu item not found with id: 99", exception.getMessage());

        verify(menuItemRepository, times(1)).findById(99L);
    }

    @Test
    void getMenuItemsByCategory_ShouldReturnMenuItemsForCategory() {
        Category category = createCategory();

        MenuItem item = MenuItem.builder()
                .id(10L)
                .name("Farmhouse Pizza")
                .description("Loaded with vegetables")
                .price(BigDecimal.valueOf(399))
                .imageUrl("/images/farmhouse.jpg")
                .veg(true)
                .available(true)
                .rating(4.7)
                .category(category)
                .build();

        when(menuItemRepository.findByCategoryIdAndAvailableTrue(1L)).thenReturn(List.of(item));

        List<MenuItemResponse> responses = menuService.getMenuItemsByCategory(1L);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("Farmhouse Pizza", responses.get(0).getName());
        assertEquals(1L, responses.get(0).getCategoryId());

        verify(menuItemRepository, times(1)).findByCategoryIdAndAvailableTrue(1L);
    }

    @Test
    void getMenuItemsByVegType_ShouldReturnVegMenuItems() {
        Category category = createCategory();

        MenuItem item = MenuItem.builder()
                .id(10L)
                .name("Farmhouse Pizza")
                .description("Loaded with vegetables")
                .price(BigDecimal.valueOf(399))
                .imageUrl("/images/farmhouse.jpg")
                .veg(true)
                .available(true)
                .rating(4.7)
                .category(category)
                .build();

        when(menuItemRepository.findByVegAndAvailableTrue(true)).thenReturn(List.of(item));

        List<MenuItemResponse> responses = menuService.getMenuItemsByVegType(true);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertTrue(responses.get(0).getVeg());
        assertEquals("Farmhouse Pizza", responses.get(0).getName());

        verify(menuItemRepository, times(1)).findByVegAndAvailableTrue(true);
    }

    @Test
    void getMenuItemsByVegType_ShouldReturnNonVegMenuItems() {
        Category category = Category.builder()
                .id(2L)
                .name("Non Veg Pizza")
                .description("Chicken and meat based pizzas")
                .active(true)
                .build();

        MenuItem item = MenuItem.builder()
                .id(20L)
                .name("Chicken Pizza")
                .description("Loaded with chicken toppings")
                .price(BigDecimal.valueOf(499))
                .imageUrl("/images/non-veg-pizza.jpg")
                .veg(false)
                .available(true)
                .rating(4.6)
                .category(category)
                .build();

        when(menuItemRepository.findByVegAndAvailableTrue(false)).thenReturn(List.of(item));

        List<MenuItemResponse> responses = menuService.getMenuItemsByVegType(false);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertFalse(responses.get(0).getVeg());
        assertEquals("Chicken Pizza", responses.get(0).getName());
        assertEquals("Non Veg Pizza", responses.get(0).getCategoryName());

        verify(menuItemRepository, times(1)).findByVegAndAvailableTrue(false);
    }

    private Category createCategory() {
        return Category.builder()
                .id(1L)
                .name("Veg Pizza")
                .description("Fresh vegetarian pizzas")
                .active(true)
                .build();
    }
}
