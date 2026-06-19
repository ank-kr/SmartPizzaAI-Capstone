package com.smartpizza.analytics.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.smartpizza.analytics.client.CoreServiceClient;
import com.smartpizza.analytics.dto.AiChatRequest;
import com.smartpizza.analytics.dto.AiChatResponse;
import com.smartpizza.analytics.dto.CoreMenuItemResponse;
import com.smartpizza.analytics.dto.CoreOrderResponse;
import com.smartpizza.analytics.dto.RecommendationResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

	private final CoreServiceClient coreServiceClient;

	private final RecommendationService recommendationService;

	public AiChatResponse chat(AiChatRequest request) {
		validateChatRequest(request);

		String normalizedMessage = request.getMessage().trim().toLowerCase();

		log.info("AI chat request received. userId={}, messageLength={}", request.getUserId(),
				request.getMessage().length());

		if (containsAny(normalizedMessage, "recommend", "suggest", "best", "popular", "trending")) {
			return buildRecommendationReply(request);
		}

		if (containsAny(normalizedMessage, "order", "status", "my order", "history")) {
			return buildOrderStatusReply(request);
		}

		if (containsAny(normalizedMessage, "cart", "add to cart", "basket")) {
			return buildCartHelpReply(request);
		}

		if (containsAny(normalizedMessage, "coupon", "discount", "offer", "promo")) {
			return buildCouponHelpReply(request);
		}

		if (containsAny(normalizedMessage, "payment", "pay", "paid", "refund")) {
			return buildPaymentHelpReply(request);
		}

		if (containsAny(normalizedMessage, "delivery", "track", "partner", "where is my order")) {
			return buildDeliveryHelpReply(request);
		}

		if (containsAny(normalizedMessage, "menu", "pizza", "veg", "non veg", "beverage")) {
			return buildMenuHelpReply(request);
		}

		return buildDefaultReply(request);
	}

	private AiChatResponse buildRecommendationReply(AiChatRequest request) {
		List<RecommendationResponse> recommendations = recommendationService
				.getPersonalizedRecommendations(request.getUserId());

		List<String> suggestions = recommendations.stream().limit(3).map(RecommendationResponse::getItemName).toList();

		String reply;

		if (suggestions.isEmpty()) {
			reply = "I could not find personalized recommendations right now. You can try browsing our available menu items or trending pizzas.";
			log.warn("AI chat recommendation fallback used because recommendation list is empty. userId={}",
					request.getUserId());
		} else {
			reply = "Based on your ordering pattern and current trends, I recommend: " + String.join(", ", suggestions)
					+ ".";
			log.info("AI chat recommendation reply generated. userId={}, suggestionCount={}", request.getUserId(),
					suggestions.size());
		}

		return buildResponse(request, reply, "RECOMMENDATION", suggestions);
	}

	private AiChatResponse buildOrderStatusReply(AiChatRequest request) {
		List<CoreOrderResponse> orders = coreServiceClient.getOrdersByUserId(request.getUserId());

		if (orders == null || orders.isEmpty()) {
			log.warn("AI chat order status requested but no orders found. userId={}", request.getUserId());

			return buildResponse(request,
					"I could not find any orders for your account. Once you place an order, I can help you track its status here.",
					"ORDER_STATUS", List.of("Browse menu", "Add items to cart", "Place your first order"));
		}

		CoreOrderResponse latestOrder = orders.get(0);

		String reply = "Your latest order #" + latestOrder.getOrderId() + " is currently in "
				+ latestOrder.getOrderStatus() + " status with payment status " + latestOrder.getPaymentStatus()
				+ ". Final amount is ₹" + latestOrder.getFinalAmount() + ".";

		log.info("AI chat order status reply generated. userId={}, orderId={}, orderStatus={}, paymentStatus={}",
				request.getUserId(), latestOrder.getOrderId(), latestOrder.getOrderStatus(),
				latestOrder.getPaymentStatus());

		return buildResponse(request, reply, "ORDER_STATUS",
				List.of("Track latest order", "View order history", "Contact support"));
	}

	private AiChatResponse buildCartHelpReply(AiChatRequest request) {
		log.info("AI chat cart help reply generated. userId={}", request.getUserId());

		return buildResponse(request,
				"You can add pizzas or beverages to your cart from the customer dashboard. After adding items, open the cart page to update quantity, apply coupon, and place your order.",
				"CART_HELP", List.of("Browse menu", "Add item to cart", "Open cart page"));
	}

	private AiChatResponse buildCouponHelpReply(AiChatRequest request) {
		log.info("AI chat coupon help reply generated. userId={}", request.getUserId());

		return buildResponse(request,
				"You can apply an active coupon during checkout from the cart page. The system validates coupon status, expiry date, minimum order amount, and discount limit before applying it.",
				"COUPON_HELP", List.of("Check active coupons", "Apply coupon in cart", "Proceed to checkout"));
	}

	private AiChatResponse buildPaymentHelpReply(AiChatRequest request) {
		log.info("AI chat payment help reply generated. userId={}", request.getUserId());

		return buildResponse(request,
				"After placing an order, the order status becomes PAYMENT_PENDING. Once payment is completed, the system marks it as PAID and CONFIRMED, then automatically assigns delivery.",
				"PAYMENT_HELP", List.of("Place order", "Complete payment", "Track delivery"));
	}

	private AiChatResponse buildDeliveryHelpReply(AiChatRequest request) {
		log.info("AI chat delivery help reply generated. userId={}", request.getUserId());

		return buildResponse(request,
				"Delivery tracking becomes available after successful payment and delivery partner assignment. You can track partner details, delivery status, distance, and ETA from the tracking page.",
				"DELIVERY_HELP", List.of("Complete payment", "Wait for delivery assignment", "Open delivery tracking"));
	}

	private AiChatResponse buildMenuHelpReply(AiChatRequest request) {
		List<CoreMenuItemResponse> menuItems = coreServiceClient.getAllMenuItems();

		List<String> suggestions = menuItems == null ? List.of()
				: menuItems.stream().filter(item -> item != null && Boolean.TRUE.equals(item.getAvailable())).limit(3)
						.map(CoreMenuItemResponse::getName).toList();

		String reply;

		if (suggestions.isEmpty()) {
			reply = "You can browse available pizzas, beverages, and combos from the customer dashboard.";
		} else {
			reply = "Some available menu items you can try are: " + String.join(", ", suggestions) + ".";
		}

		log.info("AI chat menu help reply generated. userId={}, suggestionCount={}", request.getUserId(),
				suggestions.size());

		return buildResponse(request, reply, "MENU_HELP", suggestions);
	}

	private AiChatResponse buildDefaultReply(AiChatRequest request) {
		log.info("AI chat default reply generated. userId={}", request.getUserId());

		return buildResponse(request,
				"I can help you with pizza recommendations, order status, cart, coupon, payment, and delivery tracking. Try asking: 'recommend me a pizza' or 'what is my order status?'",
				"GENERAL_HELP", List.of("Recommend me a pizza", "What is my order status?", "How can I apply coupon?"));
	}

	private AiChatResponse buildResponse(AiChatRequest request, String reply, String responseType,
			List<String> suggestions) {
		return AiChatResponse.builder().userId(request.getUserId()).userMessage(request.getMessage()).reply(reply)
				.responseType(responseType).suggestions(suggestions).respondedAt(LocalDateTime.now()).build();
	}

	private void validateChatRequest(AiChatRequest request) {
		if (request == null) {
			log.warn("AI chat request failed because request body is null");
			throw new RuntimeException("Request body cannot be null");
		}

		if (request.getUserId() == null) {
			log.warn("AI chat request failed because userId is null");
			throw new RuntimeException("User id is required");
		}

		if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
			log.warn("AI chat request failed because message is blank. userId={}", request.getUserId());
			throw new RuntimeException("Message is required");
		}
	}

	private boolean containsAny(String message, String... keywords) {
		for (String keyword : keywords) {
			if (message.contains(keyword)) {
				return true;
			}
		}

		return false;
	}
}