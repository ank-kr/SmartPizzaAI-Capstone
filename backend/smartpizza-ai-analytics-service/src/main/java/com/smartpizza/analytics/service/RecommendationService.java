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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

	private final CoreServiceClient coreServiceClient;

	public List<RecommendationResponse> getPersonalizedRecommendations(Long userId) {
		validateUserId(userId);

		log.info("Generating personalized recommendations for userId={}", userId);

		List<CoreMenuItemResponse> menuItems = coreServiceClient.getAllMenuItems();
		List<CoreOrderResponse> userOrders = coreServiceClient.getOrdersByUserId(userId);

		if (isEmpty(userOrders)) {
			log.warn("No order history found for userId={}. Falling back to trending recommendations", userId);
			return getTrendingRecommendations();
		}

		Map<Long, CoreMenuItemResponse> menuItemMap = buildMenuItemMap(menuItems);
		Map<String, Integer> categoryFrequencyMap = buildCategoryFrequencyMap(userOrders, menuItemMap);

		String favoriteCategory = findFavoriteCategory(categoryFrequencyMap);

		if (favoriteCategory == null) {
			log.warn("Favorite category could not be resolved for userId={}. Falling back to trending recommendations",
					userId);
			return getTrendingRecommendations();
		}

		List<RecommendationResponse> recommendations = menuItems.stream().filter(this::isAvailable)
				.filter(item -> favoriteCategory.equalsIgnoreCase(item.getCategoryName()))
				.sorted(Comparator.comparingDouble(this::getSafeRating).reversed()).limit(5)
				.map(item -> RecommendationResponse.builder().recommendationType("PERSONALIZED").itemId(item.getId())
						.itemName(item.getName()).categoryName(item.getCategoryName()).price(item.getPrice())
						.reason("Recommended because you frequently order from " + favoriteCategory + " category")
						.score(92.0).build())
				.toList();

		log.info("Personalized recommendations generated. userId={}, favoriteCategory={}, count={}", userId,
				favoriteCategory, recommendations.size());

		return recommendations;
	}

	public List<RecommendationResponse> getTrendingRecommendations() {
		log.info("Generating trending recommendations");

		List<CoreOrderResponse> allOrders = coreServiceClient.getAllOrders();
		List<CoreMenuItemResponse> menuItems = coreServiceClient.getAllMenuItems();

		Map<Long, Long> itemQuantityMap = buildItemQuantityMap(allOrders);

		if (itemQuantityMap.isEmpty()) {
			log.warn("No order quantity data found. Falling back to highest rated recommendations");
			return getHighestRatedRecommendations(menuItems);
		}

		Map<Long, CoreMenuItemResponse> menuItemMap = buildMenuItemMap(menuItems);

		List<RecommendationResponse> recommendations = itemQuantityMap.entrySet().stream()
				.sorted(Map.Entry.<Long, Long>comparingByValue().reversed()).limit(5)
				.map(entry -> buildTrendingRecommendation(entry, menuItemMap)).toList();

		log.info("Trending recommendations generated successfully. count={}", recommendations.size());

		return recommendations;
	}

	public List<ComboSuggestionResponse> getComboSuggestions(Long userId) {
		validateUserId(userId);

		log.info("Generating combo suggestions for userId={}", userId);

		List<CoreOrderResponse> userOrders = coreServiceClient.getOrdersByUserId(userId);

		if (!isEmpty(userOrders)) {
			List<ComboSuggestionResponse> combos = List.of(
					ComboSuggestionResponse.builder().comboName("Smart Repeat Combo")
							.items(List.of("Favorite Pizza", "Garlic Bread", "Cold Beverage"))
							.estimatedPrice(BigDecimal.valueOf(599))
							.reason("Suggested based on your previous pizza ordering pattern").score(91.0).build(),

					ComboSuggestionResponse.builder().comboName("Family Feast Combo")
							.items(List.of("Medium Pizza", "Garlic Bread", "Dessert", "Beverage"))
							.estimatedPrice(BigDecimal.valueOf(799))
							.reason("Recommended because your order history shows complete meal preference").score(88.0)
							.build());

			log.info("Personalized combo suggestions generated. userId={}, count={}", userId, combos.size());

			return combos;
		}

		log.warn("No order history found for combo suggestions. Returning starter combos. userId={}", userId);

		List<ComboSuggestionResponse> combos = List.of(
				ComboSuggestionResponse.builder().comboName("Starter Combo")
						.items(List.of("Margherita Pizza", "Garlic Bread", "Cold Beverage"))
						.estimatedPrice(BigDecimal.valueOf(499)).reason("Best combo for first-time customers")
						.score(84.0).build(),

				ComboSuggestionResponse.builder().comboName("Cheese Lover Combo")
						.items(List.of("Cheese Burst Pizza", "Cheesy Dip", "Cold Beverage"))
						.estimatedPrice(BigDecimal.valueOf(599)).reason("Popular beginner-friendly combo").score(82.0)
						.build());

		log.info("Starter combo suggestions generated. userId={}, count={}", userId, combos.size());

		return combos;
	}

	public List<RecommendationResponse> getWeatherRecommendations(Long userId, String weather) {
		validateUserId(userId);

		String normalizedWeather = normalizeWeather(weather);

		log.info("Generating weather-based recommendations. userId={}, weather={}", userId, normalizedWeather);

		List<CoreMenuItemResponse> menuItems = coreServiceClient.getAllMenuItems();

		List<RecommendationResponse> recommendations = menuItems.stream().filter(this::isAvailable)
				.filter(item -> isWeatherMatch(item, normalizedWeather)).limit(5)
				.map(item -> RecommendationResponse.builder().recommendationType("WEATHER_BASED").itemId(item.getId())
						.itemName(item.getName()).categoryName(item.getCategoryName()).price(item.getPrice())
						.reason(buildWeatherReason(normalizedWeather)).score(89.0).build())
				.toList();

		log.info("Weather-based recommendations generated. userId={}, weather={}, count={}", userId, normalizedWeather,
				recommendations.size());

		return recommendations;
	}

	private Map<String, Integer> buildCategoryFrequencyMap(List<CoreOrderResponse> userOrders,
			Map<Long, CoreMenuItemResponse> menuItemMap) {
		Map<String, Integer> categoryFrequencyMap = new HashMap<>();

		for (CoreOrderResponse order : userOrders) {
			if (order.getItems() == null) {
				log.debug("Skipping order while building category frequency because items are null. orderId={}",
						order.getOrderId());
				continue;
			}

			for (CoreOrderItemResponse orderItem : order.getItems()) {
				CoreMenuItemResponse menuItem = menuItemMap.get(orderItem.getMenuItemId());

				if (menuItem == null || menuItem.getCategoryName() == null) {
					log.debug("Skipping order item because menu item/category is missing. menuItemId={}",
							orderItem.getMenuItemId());
					continue;
				}

				int quantity = orderItem.getQuantity() == null ? 0 : orderItem.getQuantity();

				categoryFrequencyMap.merge(menuItem.getCategoryName(), quantity, Integer::sum);
			}
		}

		log.debug("Category frequency map built. categoryCount={}", categoryFrequencyMap.size());

		return categoryFrequencyMap;
	}

	private String findFavoriteCategory(Map<String, Integer> categoryFrequencyMap) {
		String favoriteCategory = categoryFrequencyMap.entrySet().stream().max(Map.Entry.comparingByValue())
				.map(Map.Entry::getKey).orElse(null);

		log.debug("Favorite category resolved. favoriteCategory={}", favoriteCategory);

		return favoriteCategory;
	}

	private Map<Long, Long> buildItemQuantityMap(List<CoreOrderResponse> orders) {
		Map<Long, Long> itemQuantityMap = new HashMap<>();

		if (isEmpty(orders)) {
			log.debug("No orders available for item quantity map generation");
			return itemQuantityMap;
		}

		for (CoreOrderResponse order : orders) {
			if (order.getItems() == null) {
				log.debug("Skipping order while building item quantity map because items are null. orderId={}",
						order.getOrderId());
				continue;
			}

			for (CoreOrderItemResponse orderItem : order.getItems()) {
				Long menuItemId = orderItem.getMenuItemId();

				if (menuItemId == null) {
					log.debug("Skipping order item because menuItemId is null");
					continue;
				}

				Long quantity = orderItem.getQuantity() == null ? 0L : orderItem.getQuantity().longValue();

				itemQuantityMap.merge(menuItemId, quantity, Long::sum);
			}
		}

		log.debug("Item quantity map built. itemCount={}", itemQuantityMap.size());

		return itemQuantityMap;
	}

	private List<RecommendationResponse> getHighestRatedRecommendations(List<CoreMenuItemResponse> menuItems) {
		List<RecommendationResponse> recommendations = menuItems.stream().filter(this::isAvailable)
				.sorted(Comparator.comparingDouble(this::getSafeRating).reversed()).limit(5)
				.map(item -> RecommendationResponse.builder().recommendationType("TRENDING").itemId(item.getId())
						.itemName(item.getName()).categoryName(item.getCategoryName()).price(item.getPrice())
						.reason("Recommended because this is one of the highest rated available menu items").score(85.0)
						.build())
				.toList();

		log.info("Highest rated recommendations generated successfully. count={}", recommendations.size());

		return recommendations;
	}

	private RecommendationResponse buildTrendingRecommendation(Map.Entry<Long, Long> entry,
			Map<Long, CoreMenuItemResponse> menuItemMap) {
		CoreMenuItemResponse item = menuItemMap.get(entry.getKey());

		String itemName = item == null ? "Menu Item " + entry.getKey() : item.getName();
		String categoryName = item == null ? null : item.getCategoryName();
		BigDecimal price = item == null ? BigDecimal.ZERO : item.getPrice();

		log.debug("Building trending recommendation. menuItemId={}, itemName={}, quantity={}", entry.getKey(), itemName,
				entry.getValue());

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
			log.debug("Menu item list is null while building menu item map");
			return menuItemMap;
		}

		for (CoreMenuItemResponse item : menuItems) {
			if (item != null && item.getId() != null) {
				menuItemMap.put(item.getId(), item);
			}
		}

		log.debug("Menu item map built. itemCount={}", menuItemMap.size());

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
			log.warn("Recommendation generation failed because userId is null");
			throw new RuntimeException("User id is required");
		}
	}
}