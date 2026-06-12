package com.smartpizza.core.controller;

import com.smartpizza.core.dto.CategoryRequest;
import com.smartpizza.core.dto.CategoryResponse;
import com.smartpizza.core.dto.MenuItemRequest;
import com.smartpizza.core.dto.MenuItemResponse;
import com.smartpizza.core.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Core Service is running");
    }

    @PostMapping("/admin/categories")
    public ResponseEntity<CategoryResponse> addCategory(@RequestBody CategoryRequest request) {
        return ResponseEntity.ok(menuService.addCategory(request));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(menuService.getAllActiveCategories());
    }

    @PostMapping("/admin/menu-items")
    public ResponseEntity<MenuItemResponse> addMenuItem(@RequestBody MenuItemRequest request) {
        return ResponseEntity.ok(menuService.addMenuItem(request));
    }

    @GetMapping("/menu-items")
    public ResponseEntity<List<MenuItemResponse>> getAllMenuItems() {
        return ResponseEntity.ok(menuService.getAllAvailableMenuItems());
    }

    @GetMapping("/menu-items/{id}")
    public ResponseEntity<MenuItemResponse> getMenuItemById(@PathVariable Long id) {
        return ResponseEntity.ok(menuService.getMenuItemById(id));
    }

    @GetMapping("/menu-items/category/{categoryId}")
    public ResponseEntity<List<MenuItemResponse>> getMenuItemsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(menuService.getMenuItemsByCategory(categoryId));
    }

    @GetMapping("/menu-items/filter")
    public ResponseEntity<List<MenuItemResponse>> getMenuItemsByVegType(@RequestParam Boolean veg) {
        return ResponseEntity.ok(menuService.getMenuItemsByVegType(veg));
    }
}