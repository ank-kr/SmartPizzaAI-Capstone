package com.smartpizza.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartpizza.analytics.client.CoreServiceClient;
import com.smartpizza.analytics.dto.AnalyticsSummaryResponse;
import com.smartpizza.analytics.dto.CoreOrderItemResponse;
import com.smartpizza.analytics.dto.CoreOrderResponse;
import com.smartpizza.analytics.dto.DeliveryPartnerResponse;
import com.smartpizza.analytics.dto.DeliveryPerformanceResponse;
import com.smartpizza.analytics.dto.TopItemResponse;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

	@Mock
	private CoreServiceClient coreServiceClient;

	@InjectMocks
	private AnalyticsService analyticsService;

	@Test
	void getSummary_shouldReturnCorrectAnalyticsSummary() {
		CoreOrderResponse paidDeliveredOrder = CoreOrderResponse.builder().orderId(1L).userId(101L)
				.paymentStatus("PAID").orderStatus("DELIVERED").finalAmount(BigDecimal.valueOf(500)).build();

		CoreOrderResponse paidPendingOrder = CoreOrderResponse.builder().orderId(2L).userId(102L).paymentStatus("PAID")
				.orderStatus("CONFIRMED").finalAmount(BigDecimal.valueOf(300)).build();

		CoreOrderResponse unpaidCancelledOrder = CoreOrderResponse.builder().orderId(3L).userId(103L)
				.paymentStatus("PENDING").orderStatus("CANCELLED").finalAmount(BigDecimal.valueOf(200)).build();

		when(coreServiceClient.getAllOrders())
				.thenReturn(List.of(paidDeliveredOrder, paidPendingOrder, unpaidCancelledOrder));

		AnalyticsSummaryResponse response = analyticsService.getSummary();

		assertNotNull(response);
		assertEquals(3L, response.getTotalOrders());
		assertEquals(2L, response.getPaidOrders());
		assertEquals(1L, response.getDeliveredOrders());
		assertEquals(1L, response.getPendingOrders());
		assertEquals(0, BigDecimal.valueOf(800).compareTo(response.getTotalRevenue()));
		assertEquals(0, BigDecimal.valueOf(400).setScale(2).compareTo(response.getAverageOrderValue()));

		verify(coreServiceClient).getAllOrders();
	}

	@Test
	void getSummary_whenNoPaidOrders_shouldReturnZeroAverageOrderValue() {
		CoreOrderResponse pendingOrder = CoreOrderResponse.builder().orderId(1L).userId(101L).paymentStatus("PENDING")
				.orderStatus("PAYMENT_PENDING").finalAmount(BigDecimal.valueOf(250)).build();

		when(coreServiceClient.getAllOrders()).thenReturn(List.of(pendingOrder));

		AnalyticsSummaryResponse response = analyticsService.getSummary();

		assertNotNull(response);
		assertEquals(1L, response.getTotalOrders());
		assertEquals(0L, response.getPaidOrders());
		assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotalRevenue()));
		assertEquals(0, BigDecimal.ZERO.compareTo(response.getAverageOrderValue()));

		verify(coreServiceClient).getAllOrders();
	}

	@Test
	void getTopItems_shouldReturnTopFiveItemsSortedByQuantitySold() {
		CoreOrderItemResponse faloodaItem = CoreOrderItemResponse.builder().menuItemId(1L).itemName("Falooda")
				.quantity(5).subtotal(BigDecimal.valueOf(500)).build();

		CoreOrderItemResponse pizzaItem = CoreOrderItemResponse.builder().menuItemId(2L).itemName("Farmhouse Pizza")
				.quantity(3).subtotal(BigDecimal.valueOf(900)).build();

		CoreOrderItemResponse repeatedFaloodaItem = CoreOrderItemResponse.builder().menuItemId(1L).itemName("Falooda")
				.quantity(4).subtotal(BigDecimal.valueOf(400)).build();

		CoreOrderResponse firstOrder = CoreOrderResponse.builder().orderId(1L).items(List.of(faloodaItem, pizzaItem))
				.build();

		CoreOrderResponse secondOrder = CoreOrderResponse.builder().orderId(2L).items(List.of(repeatedFaloodaItem))
				.build();

		when(coreServiceClient.getAllOrders()).thenReturn(List.of(firstOrder, secondOrder));

		List<TopItemResponse> response = analyticsService.getTopItems();

		assertNotNull(response);
		assertEquals(2, response.size());

		assertEquals(1L, response.get(0).getMenuItemId());
		assertEquals("Falooda", response.get(0).getItemName());
		assertEquals(9L, response.get(0).getTotalQuantitySold());
		assertEquals(0, BigDecimal.valueOf(900).compareTo(response.get(0).getTotalRevenue()));

		assertEquals(2L, response.get(1).getMenuItemId());
		assertEquals("Farmhouse Pizza", response.get(1).getItemName());
		assertEquals(3L, response.get(1).getTotalQuantitySold());
		assertEquals(0, BigDecimal.valueOf(900).compareTo(response.get(1).getTotalRevenue()));

		verify(coreServiceClient).getAllOrders();
	}

	@Test
	void getTopItems_whenOrderItemsAreNull_shouldSkipThatOrder() {
		CoreOrderItemResponse validItem = CoreOrderItemResponse.builder().menuItemId(10L).itemName("Garlic Bread")
				.quantity(2).subtotal(BigDecimal.valueOf(200)).build();

		CoreOrderResponse orderWithoutItems = CoreOrderResponse.builder().orderId(1L).items(null).build();

		CoreOrderResponse orderWithItems = CoreOrderResponse.builder().orderId(2L).items(List.of(validItem)).build();

		when(coreServiceClient.getAllOrders()).thenReturn(List.of(orderWithoutItems, orderWithItems));

		List<TopItemResponse> response = analyticsService.getTopItems();

		assertNotNull(response);
		assertEquals(1, response.size());
		assertEquals(10L, response.get(0).getMenuItemId());
		assertEquals("Garlic Bread", response.get(0).getItemName());
		assertEquals(2L, response.get(0).getTotalQuantitySold());

		verify(coreServiceClient).getAllOrders();
	}

	@Test
	void getDeliveryPerformance_shouldReturnCorrectPartnerStats() {
		DeliveryPartnerResponse availablePartner = DeliveryPartnerResponse.builder().deliveryPartnerId(1L)
				.partnerName("Partner One").partnerStatus("AVAILABLE").activeDeliveryCount(0).rating(4.5).build();

		DeliveryPartnerResponse busyPartner = DeliveryPartnerResponse.builder().deliveryPartnerId(2L)
				.partnerName("Partner Two").partnerStatus("BUSY").activeDeliveryCount(2).rating(4.0).build();

		DeliveryPartnerResponse offlinePartner = DeliveryPartnerResponse.builder().deliveryPartnerId(3L)
				.partnerName("Partner Three").partnerStatus("OFFLINE").activeDeliveryCount(0).rating(5.0).build();

		when(coreServiceClient.getAllDeliveryPartners())
				.thenReturn(List.of(availablePartner, busyPartner, offlinePartner));

		DeliveryPerformanceResponse response = analyticsService.getDeliveryPerformance();

		assertNotNull(response);
		assertEquals(3L, response.getTotalPartners());
		assertEquals(1L, response.getAvailablePartners());
		assertEquals(1L, response.getBusyPartners());
		assertEquals(1L, response.getOfflinePartners());
		assertEquals(2, response.getTotalActiveDeliveries());
		assertEquals(4.5, response.getAverageRating());

		verify(coreServiceClient).getAllDeliveryPartners();
	}

	@Test
	void getDeliveryPerformance_whenRatingAndActiveCountAreNull_shouldIgnoreNullValues() {
		DeliveryPartnerResponse partnerWithNulls = DeliveryPartnerResponse.builder().deliveryPartnerId(1L)
				.partnerName("Partner One").partnerStatus("AVAILABLE").activeDeliveryCount(null).rating(null).build();

		DeliveryPartnerResponse busyPartner = DeliveryPartnerResponse.builder().deliveryPartnerId(2L)
				.partnerName("Partner Two").partnerStatus("BUSY").activeDeliveryCount(3).rating(4.2).build();

		when(coreServiceClient.getAllDeliveryPartners()).thenReturn(List.of(partnerWithNulls, busyPartner));

		DeliveryPerformanceResponse response = analyticsService.getDeliveryPerformance();

		assertNotNull(response);
		assertEquals(2L, response.getTotalPartners());
		assertEquals(1L, response.getAvailablePartners());
		assertEquals(1L, response.getBusyPartners());
		assertEquals(0L, response.getOfflinePartners());
		assertEquals(3, response.getTotalActiveDeliveries());
		assertEquals(4.2, response.getAverageRating());

		verify(coreServiceClient).getAllDeliveryPartners();
	}
}