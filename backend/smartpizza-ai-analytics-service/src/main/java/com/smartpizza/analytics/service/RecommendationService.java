package com.smartpizza.analytics.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.smartpizza.analytics.client.CoreServiceClient;
import com.smartpizza.analytics.dto.ComboSuggestionResponse;
import com.smartpizza.analytics.dto.CoreMenuItemResponse;
import com.smartpizza.analytics.dto.CoreOrderItemResponse;
import com.smartpizza.analytics.dto.CoreOrderResponse;
import com.smartpizza.analytics.dto.RecommendationResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecommendationService {

	private final CoreServiceClient coreServiceClient;

	public List<RecommendationResponse> getPersonalizedRecommendations(Long userId) {
		validateUserId(userId);

		List<CoreMenuItemResponse> menuItems = coreServiceClient.getAllMenuItems();
		List<CoreOrderResponse> userOrders = coreServiceClient.getOrdersByUserId(userId);

		if (isEmpty(userOrders)) {
			return getTrendingRecommendations();
		}

		Map<Long, CoreMenuItemResponse> menuItemMap = buildMenuItemMap(menuItems);
		Map<String, Integer> categoryFrequencyMap = buildCategoryFrequencyMap(userOrders, menuItemMap);

		String favoriteCategory = findFavoriteCategory(categoryFrequencyMap);

		if (favoriteCategory == null) {
			return getTrendingRecommendations();
		}

		return menuItems.stream().filter(this::isAvailable)
				.filter(item -> favoriteCategory.equalsIgnoreCase(item.getCategoryName()))
				.sorted(Comparator.comparingDouble(this::getSafeRating).reversed()).limit(5)
				.map(item -> RecommendationResponse.builder().recommendationType("PERSONALIZED").itemId(item.getId())
						.itemName(item.getName()).categoryName(item.getCategoryName()).price(item.getPrice())
						.reason("Recommended because you frequently order from " + favoriteCategory + " category")
						.score(92.0).build())
				.toList();
	}

	public List<RecommendationResponse> getTrendingRecommendations() {
		List<CoreOrderResponse> allOrders = coreServiceClient.getAllOrders();
		List<CoreMenuItemResponse> menuItems = coreServiceClient.getAllMenuItems();

		Map<Long, Long> itemQuantityMap = buildItemQuantityMap(allOrders);

		if (itemQuantityMap.isEmpty()) {
			return getHighestRatedRecommendations(menuItems);
		}

		Map<Long, CoreMenuItemResponse> menuItemMap = buildMenuItemMap(menuItems);

		return itemQuantityMap.entrySet().stream().sorted(Map.Entry.<Long, Long>comparingByValue().reversed()).limit(5)
				.map(entry -> buildTrendingRecommendation(entry, menuItemMap)).toList();
	}

	public List<ComboSuggestionResponse> getComboSuggestions(Long userId) {
		validateUserId(userId);

		List<CoreOrderResponse> userOrders = coreServiceClient.getOrdersByUserId(userId);

		if (!isEmpty(userOrders)) {
			return List.of(
					ComboSuggestionResponse.builder().comboName("Smart Repeat Combo")
							.items(List.of("Favorite Pizza", "Garlic Bread", "Cold Beverage"))
							.estimatedPrice(BigDecimal.valueOf(599))
							.reason("Suggested based on your previous pizza ordering pattern").score(91.0).build(),

					ComboSuggestionResponse.builder().comboName("Family Feast Combo")
							.items(List.of("Medium Pizza", "Garlic Bread", "Dessert", "Beverage"))
							.estimatedPrice(BigDecimal.valueOf(799))
							.reason("Recommended because your order history shows complete meal preference").score(88.0)
							.build());
		}

		return List.of(
				ComboSuggestionResponse.builder().comboName("Starter Combo")
						.items(List.of("Margherita Pizza", "Garlic Bread", "Cold Beverage"))
						.estimatedPrice(BigDecimal.valueOf(499)).reason("Best combo for first-time customers")
						.score(84.0).build(),

				ComboSuggestionResponse.builder().comboName("Cheese Lover Combo")
						.items(List.of("Cheese Burst Pizza", "Cheesy Dip", "Cold Beverage"))
						.estimatedPrice(BigDecimal.valueOf(599)).reason("Popular beginner-friendly combo").score(82.0)
						.build());
	}

	public List<RecommendationResponse> getWeatherRecommendations(Long userId, String weather) {
		validateUserId(userId);

		List<CoreMenuItemResponse> menuItems = coreServiceClient.getAllMenuItems();
		String normalizedWeather = normalizeWeather(weather);

		return menuItems.stream().filter(this::isAvailable).filter(item -> isWeatherMatch(item, normalizedWeather))
				.limit(5)
				.map(item -> RecommendationResponse.builder().recommendationType("WEATHER_BASED").itemId(item.getId())
						.itemName(item.getName()).categoryName(item.getCategoryName()).price(item.getPrice())
						.reason(buildWeatherReason(normalizedWeather)).score(89.0).build())
				.toList();
	}

	private Map<String, Integer> buildCategoryFrequencyMap(List<CoreOrderResponse> userOrders,
			Map<Long, CoreMenuItemResponse> menuItemMap) {
		Map<String, Integer> categoryFrequencyMap = new HashMap<>();

		for (CoreOrderResponse order : userOrders) {
			if (order.getItems() == null) {
				continue;
			}

			for (CoreOrderItemResponse orderItem : order.getItems()) {
				CoreMenuItemResponse menuItem = menuItemMap.get(orderItem.getMenuItemId());

				if (menuItem == null || menuItem.getCategoryName() == null) {
					continue;
				}

				int quantity = orderItem.getQuantity() == null ? 0 : orderItem.getQuantity();

				categoryFrequencyMap.merge(menuItem.getCategoryName(), quantity, Integer::sum);
			}
		}

		return categoryFrequencyMap;
	}

	private String findFavoriteCategory(Map<String, Integer> categoryFrequencyMap) {
		return categoryFrequencyMap.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey)
				.orElse(null);
	}

	private Map<Long, Long> buildItemQuantityMap(List<CoreOrderResponse> orders) {
		Map<Long, Long> itemQuantityMap = new HashMap<>();

		if (isEmpty(orders)) {
			return itemQuantityMap;
		}

		for (CoreOrderResponse order : orders) {
			if (order.getItems() == null) {
				continue;
			}

			for (CoreOrderItemResponse orderItem : order.getItems()) {
				Long menuItemId = orderItem.getMenuItemId();

				if (menuItemId == null) {
					continue;
				}

				Long quantity = orderItem.getQuantity() == null ? 0L : orderItem.getQuantity().longValue();

				itemQuantityMap.merge(menuItemId, quantity, Long::sum);
			}
		}

		return itemQuantityMap;
	}

	private List<RecommendationResponse> getHighestRatedRecommendations(List<CoreMenuItemResponse> menuItems) {
		return menuItems.stream().filter(this::isAvailable)
				.sorted(Comparator.comparingDouble(this::getSafeRating).reversed()).limit(5)
				.map(item -> RecommendationResponse.builder().recommendationType("TRENDING").itemId(item.getId())
						.itemName(item.getName()).categoryName(item.getCategoryName()).price(item.getPrice())
						.reason("Recommended because this is one of the highest rated available menu items").score(85.0)
						.build())
				.toList();
	}

	private RecommendationResponse buildTrendingRecommendation(Map.Entry<Long, Long> entry,
			Map<Long, CoreMenuItemResponse> menuItemMap) {
		CoreMenuItemResponse item = menuItemMap.get(entry.getKey());

		String itemName = item == null ? "Menu Item " + entry.getKey() : item.getName();
		String categoryName = item == null ? null : item.getCategoryName();
		BigDecimal price = item == null ? BigDecimal.ZERO : item.getPrice();

		return RecommendationResponse.builder().recommendationType("TRENDING").itemId(entry.getKey()).itemName(itemName)
				.categoryName(categoryName).price(price)
				.reason("Trending because customers ordered this item " + entry.getValue() + " times").score(90.0)
				.build();
	}

	private boolean isWeatherMatch(CoreMenuItemResponse item, String weather) {
		if ("RAINY".equals(weather)) {
			return containsIgnoreCase(item.getCrustType(), "CHEESE")
					|| containsIgnoreCase(item.getSpiceLevel(), "MEDIUM")
					|| containsIgnoreCase(item.getSpiceLevel(), "HIGH");
		}

		if ("HOT".equals(weather)) {
			return containsIgnoreCase(item.getCategoryName(), "BEVERAGE") || containsIgnoreCase(item.getName(), "COKE")
					|| containsIgnoreCase(item.getName(), "DRINK");
		}

		if ("COLD".equals(weather)) {
			return containsIgnoreCase(item.getCrustType(), "CHEESE")
					|| containsIgnoreCase(item.getSpiceLevel(), "HIGH");
		}

		return getSafeRating(item) >= 4.5;
	}

	private String buildWeatherReason(String weather) {
		if ("RAINY".equals(weather)) {
			return "Rainy weather detected, so hot cheesy and spicy items are recommended";
		}

		if ("HOT".equals(weather)) {
			return "Hot weather detected, so refreshing beverage-based combos are recommended";
		}

		if ("COLD".equals(weather)) {
			return "Cold weather detected, so cheese burst and spicy pizzas are recommended";
		}

		return "Recommended based on normal weather and high item rating";
	}

	private Map<Long, CoreMenuItemResponse> buildMenuItemMap(List<CoreMenuItemResponse> menuItems) {
		Map<Long, CoreMenuItemResponse> menuItemMap = new HashMap<>();

		if (menuItems == null) {
			return menuItemMap;
		}

		for (CoreMenuItemResponse item : menuItems) {
			if (item != null && item.getId() != null) {
				menuItemMap.put(item.getId(), item);
			}
		}

		return menuItemMap;
	}

	private boolean isAvailable(CoreMenuItemResponse item) {
		return item != null && Boolean.TRUE.equals(item.getAvailable());
	}

	private double getSafeRating(CoreMenuItemResponse item) {
		if (item == null || item.getRating() == null) {
			return 0.0;
		}

		return item.getRating();
	}

	private boolean containsIgnoreCase(String value, String keyword) {
		return value != null && keyword != null && value.toUpperCase().contains(keyword.toUpperCase());
	}

	private String normalizeWeather(String weather) {
		if (weather == null || weather.trim().isEmpty()) {
			return "NORMAL";
		}

		return weather.trim().toUpperCase();
	}

	private boolean isEmpty(List<?> list) {
		return list == null || list.isEmpty();
	}

	private void validateUserId(Long userId) {
		if (userId == null) {
			throw new RuntimeException("User id is required");
		}
	}
}