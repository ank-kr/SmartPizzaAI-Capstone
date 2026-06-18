package com.smartpizza.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartpizza.analytics.client.CoreServiceClient;
import com.smartpizza.analytics.dto.ComboSuggestionResponse;
import com.smartpizza.analytics.dto.CoreMenuItemResponse;
import com.smartpizza.analytics.dto.CoreOrderItemResponse;
import com.smartpizza.analytics.dto.CoreOrderResponse;
import com.smartpizza.analytics.dto.RecommendationResponse;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

	@Mock
	private CoreServiceClient coreServiceClient;

	@InjectMocks
	private RecommendationService recommendationService;

	@Test
	void getPersonalizedRecommendations_whenUserHasOrderHistory_shouldReturnFavoriteCategoryItems() {
		Long userId = 1L;

		CoreMenuItemResponse vegPizza = CoreMenuItemResponse.builder().id(101L).name("Farmhouse Pizza")
				.categoryName("Veg Pizza").price(BigDecimal.valueOf(399)).available(true).rating(4.8).build();

		CoreMenuItemResponse margherita = CoreMenuItemResponse.builder().id(102L).name("Margherita Pizza")
				.categoryName("Veg Pizza").price(BigDecimal.valueOf(299)).available(true).rating(4.5).build();

		CoreMenuItemResponse coke = CoreMenuItemResponse.builder().id(201L).name("Coke").categoryName("Beverage")
				.price(BigDecimal.valueOf(80)).available(true).rating(4.2).build();

		CoreOrderItemResponse orderedPizza = CoreOrderItemResponse.builder().menuItemId(101L).quantity(5).build();

		CoreOrderResponse order = CoreOrderResponse.builder().orderId(1L).items(List.of(orderedPizza)).build();

		when(coreServiceClient.getAllMenuItems()).thenReturn(List.of(vegPizza, margherita, coke));
		when(coreServiceClient.getOrdersByUserId(userId)).thenReturn(List.of(order));

		List<RecommendationResponse> response = recommendationService.getPersonalizedRecommendations(userId);

		assertNotNull(response);
		assertEquals(2, response.size());

		assertEquals("PERSONALIZED", response.get(0).getRecommendationType());
		assertEquals("Farmhouse Pizza", response.get(0).getItemName());
		assertEquals("Veg Pizza", response.get(0).getCategoryName());
		assertEquals(92.0, response.get(0).getScore());

		assertEquals("Margherita Pizza", response.get(1).getItemName());

		verify(coreServiceClient).getAllMenuItems();
		verify(coreServiceClient).getOrdersByUserId(userId);
		verify(coreServiceClient, never()).getAllOrders();
	}

	@Test
	void getPersonalizedRecommendations_whenUserHasNoOrderHistory_shouldReturnTrendingRecommendations() {
		Long userId = 1L;

		CoreMenuItemResponse falooda = CoreMenuItemResponse.builder().id(301L).name("Falooda").categoryName("Dessert")
				.price(BigDecimal.valueOf(120)).available(true).rating(4.7).build();

		CoreOrderItemResponse orderItem = CoreOrderItemResponse.builder().menuItemId(301L).quantity(10).build();

		CoreOrderResponse order = CoreOrderResponse.builder().orderId(1L).items(List.of(orderItem)).build();

		when(coreServiceClient.getAllMenuItems()).thenReturn(List.of(falooda));
		when(coreServiceClient.getOrdersByUserId(userId)).thenReturn(List.of());
		when(coreServiceClient.getAllOrders()).thenReturn(List.of(order));

		List<RecommendationResponse> response = recommendationService.getPersonalizedRecommendations(userId);

		assertNotNull(response);
		assertEquals(1, response.size());
		assertEquals("TRENDING", response.get(0).getRecommendationType());
		assertEquals("Falooda", response.get(0).getItemName());
		assertTrue(response.get(0).getReason().contains("10 times"));

		verify(coreServiceClient, times(2)).getAllMenuItems();
		verify(coreServiceClient).getOrdersByUserId(userId);
		verify(coreServiceClient).getAllOrders();
	}

	@Test
	void getTrendingRecommendations_whenOrdersExist_shouldReturnItemsSortedByQuantity() {
		CoreMenuItemResponse falooda = CoreMenuItemResponse.builder().id(1L).name("Falooda").categoryName("Dessert")
				.price(BigDecimal.valueOf(120)).available(true).rating(4.8).build();

		CoreMenuItemResponse coke = CoreMenuItemResponse.builder().id(2L).name("Coke").categoryName("Beverage")
				.price(BigDecimal.valueOf(80)).available(true).rating(4.2).build();

		CoreOrderItemResponse faloodaItem = CoreOrderItemResponse.builder().menuItemId(1L).quantity(8).build();

		CoreOrderItemResponse cokeItem = CoreOrderItemResponse.builder().menuItemId(2L).quantity(3).build();

		CoreOrderResponse order = CoreOrderResponse.builder().orderId(1L).items(List.of(faloodaItem, cokeItem)).build();

		when(coreServiceClient.getAllOrders()).thenReturn(List.of(order));
		when(coreServiceClient.getAllMenuItems()).thenReturn(List.of(falooda, coke));

		List<RecommendationResponse> response = recommendationService.getTrendingRecommendations();

		assertNotNull(response);
		assertEquals(2, response.size());

		assertEquals(1L, response.get(0).getItemId());
		assertEquals("Falooda", response.get(0).getItemName());
		assertEquals("TRENDING", response.get(0).getRecommendationType());
		assertTrue(response.get(0).getReason().contains("8 times"));

		assertEquals(2L, response.get(1).getItemId());
		assertEquals("Coke", response.get(1).getItemName());

		verify(coreServiceClient).getAllOrders();
		verify(coreServiceClient).getAllMenuItems();
	}

	@Test
	void getTrendingRecommendations_whenNoOrders_shouldReturnHighestRatedAvailableItems() {
		CoreMenuItemResponse highRatedPizza = CoreMenuItemResponse.builder().id(1L).name("Cheese Burst Pizza")
				.categoryName("Pizza").price(BigDecimal.valueOf(450)).available(true).rating(4.9).build();

		CoreMenuItemResponse lowRatedPizza = CoreMenuItemResponse.builder().id(2L).name("Simple Pizza")
				.categoryName("Pizza").price(BigDecimal.valueOf(250)).available(true).rating(4.1).build();

		CoreMenuItemResponse unavailableItem = CoreMenuItemResponse.builder().id(3L).name("Unavailable Pizza")
				.categoryName("Pizza").price(BigDecimal.valueOf(300)).available(false).rating(5.0).build();

		when(coreServiceClient.getAllOrders()).thenReturn(List.of());
		when(coreServiceClient.getAllMenuItems()).thenReturn(List.of(lowRatedPizza, highRatedPizza, unavailableItem));

		List<RecommendationResponse> response = recommendationService.getTrendingRecommendations();

		assertNotNull(response);
		assertEquals(2, response.size());

		assertEquals("Cheese Burst Pizza", response.get(0).getItemName());
		assertEquals("Simple Pizza", response.get(1).getItemName());
		assertEquals("TRENDING", response.get(0).getRecommendationType());

		verify(coreServiceClient).getAllOrders();
		verify(coreServiceClient).getAllMenuItems();
	}

	@Test
	void getComboSuggestions_whenUserHasOrderHistory_shouldReturnRepeatAndFamilyCombos() {
		Long userId = 1L;

		CoreOrderResponse order = CoreOrderResponse.builder().orderId(1L).items(List.of()).build();

		when(coreServiceClient.getOrdersByUserId(userId)).thenReturn(List.of(order));

		List<ComboSuggestionResponse> response = recommendationService.getComboSuggestions(userId);

		assertNotNull(response);
		assertEquals(2, response.size());

		assertEquals("Smart Repeat Combo", response.get(0).getComboName());
		assertEquals("Family Feast Combo", response.get(1).getComboName());
		assertEquals(91.0, response.get(0).getScore());
		assertEquals(0, BigDecimal.valueOf(599).compareTo(response.get(0).getEstimatedPrice()));

		verify(coreServiceClient).getOrdersByUserId(userId);
	}

	@Test
	void getComboSuggestions_whenUserHasNoOrderHistory_shouldReturnStarterCombos() {
		Long userId = 1L;

		when(coreServiceClient.getOrdersByUserId(userId)).thenReturn(List.of());

		List<ComboSuggestionResponse> response = recommendationService.getComboSuggestions(userId);

		assertNotNull(response);
		assertEquals(2, response.size());

		assertEquals("Starter Combo", response.get(0).getComboName());
		assertEquals("Cheese Lover Combo", response.get(1).getComboName());
		assertEquals(84.0, response.get(0).getScore());
		assertEquals(0, BigDecimal.valueOf(499).compareTo(response.get(0).getEstimatedPrice()));

		verify(coreServiceClient).getOrdersByUserId(userId);
	}

	@Test
	void getWeatherRecommendations_whenWeatherIsHot_shouldReturnBeverageItems() {
		Long userId = 1L;

		CoreMenuItemResponse coke = CoreMenuItemResponse.builder().id(1L).name("Coke").categoryName("Beverage")
				.price(BigDecimal.valueOf(80)).available(true).rating(4.3).build();

		CoreMenuItemResponse pizza = CoreMenuItemResponse.builder().id(2L).name("Farmhouse Pizza")
				.categoryName("Veg Pizza").price(BigDecimal.valueOf(399)).available(true).rating(4.8).build();

		when(coreServiceClient.getAllMenuItems()).thenReturn(List.of(coke, pizza));

		List<RecommendationResponse> response = recommendationService.getWeatherRecommendations(userId, "hot");

		assertNotNull(response);
		assertEquals(1, response.size());
		assertEquals("WEATHER_BASED", response.get(0).getRecommendationType());
		assertEquals("Coke", response.get(0).getItemName());
		assertTrue(response.get(0).getReason().contains("Hot weather"));

		verify(coreServiceClient).getAllMenuItems();
	}

	@Test
	void getWeatherRecommendations_whenWeatherIsRainy_shouldReturnCheeseOrSpicyItems() {
		Long userId = 1L;

		CoreMenuItemResponse cheesePizza = CoreMenuItemResponse.builder().id(1L).name("Cheese Burst Pizza")
				.categoryName("Pizza").price(BigDecimal.valueOf(450)).available(true).rating(4.8)
				.crustType("CHEESE BURST").spiceLevel("MEDIUM").build();

		CoreMenuItemResponse coke = CoreMenuItemResponse.builder().id(2L).name("Coke").categoryName("Beverage")
				.price(BigDecimal.valueOf(80)).available(true).rating(4.0).crustType("NONE").spiceLevel("LOW").build();

		when(coreServiceClient.getAllMenuItems()).thenReturn(List.of(cheesePizza, coke));

		List<RecommendationResponse> response = recommendationService.getWeatherRecommendations(userId, "RAINY");

		assertNotNull(response);
		assertEquals(1, response.size());
		assertEquals("Cheese Burst Pizza", response.get(0).getItemName());
		assertTrue(response.get(0).getReason().contains("Rainy weather"));

		verify(coreServiceClient).getAllMenuItems();
	}

	@Test
	void getWeatherRecommendations_whenWeatherIsNormal_shouldReturnHighRatedAvailableItems() {
		Long userId = 1L;

		CoreMenuItemResponse highRatedItem = CoreMenuItemResponse.builder().id(1L).name("Margherita Pizza")
				.categoryName("Pizza").price(BigDecimal.valueOf(299)).available(true).rating(4.6).build();

		CoreMenuItemResponse lowRatedItem = CoreMenuItemResponse.builder().id(2L).name("Low Rated Item")
				.categoryName("Pizza").price(BigDecimal.valueOf(199)).available(true).rating(4.0).build();

		when(coreServiceClient.getAllMenuItems()).thenReturn(List.of(highRatedItem, lowRatedItem));

		List<RecommendationResponse> response = recommendationService.getWeatherRecommendations(userId, null);

		assertNotNull(response);
		assertEquals(1, response.size());
		assertEquals("Margherita Pizza", response.get(0).getItemName());
		assertEquals("WEATHER_BASED", response.get(0).getRecommendationType());
		assertTrue(response.get(0).getReason().contains("normal weather"));

		verify(coreServiceClient).getAllMenuItems();
	}

	@Test
	void getPersonalizedRecommendations_whenUserIdIsNull_shouldThrowException() {
		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> recommendationService.getPersonalizedRecommendations(null));

		assertEquals("User id is required", exception.getMessage());
		verifyNoInteractions(coreServiceClient);
	}

	@Test
	void getComboSuggestions_whenUserIdIsNull_shouldThrowException() {
		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> recommendationService.getComboSuggestions(null));

		assertEquals("User id is required", exception.getMessage());
		verifyNoInteractions(coreServiceClient);
	}

	@Test
	void getWeatherRecommendations_whenUserIdIsNull_shouldThrowException() {
		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> recommendationService.getWeatherRecommendations(null, "HOT"));

		assertEquals("User id is required", exception.getMessage());
		verifyNoInteractions(coreServiceClient);
	}
}