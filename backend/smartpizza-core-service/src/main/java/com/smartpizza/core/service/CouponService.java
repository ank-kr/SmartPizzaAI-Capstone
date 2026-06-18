package com.smartpizza.core.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.smartpizza.core.dto.ApplyCouponRequest;
import com.smartpizza.core.dto.ApplyCouponResponse;
import com.smartpizza.core.dto.CouponRequest;
import com.smartpizza.core.dto.CouponResponse;
import com.smartpizza.core.entity.Coupon;
import com.smartpizza.core.enums.DiscountType;
import com.smartpizza.core.repository.CouponRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

	// this repo interact with coupon table
	private final CouponRepository couponRepository;

	public CouponResponse createCoupon(CouponRequest request) {
		// Validate required coupon fields before creating coupon.
		validateCouponRequest(request);

		// It takes the coupon code entered by admin/customer and converts it into a
		// standard format.(user-pizza20, after normalization PIZZA20)
		String normalizedCode = request.getCode().trim().toUpperCase();

		log.info("Creating coupon with code={}", normalizedCode);

		// Prevent duplicate coupon creation, regardless of letter casing.
		if (couponRepository.existsByCodeIgnoreCase(normalizedCode)) {
			log.warn("Coupon creation failed because coupon already exists. code={}", normalizedCode);
			throw new RuntimeException("Coupon already exists with code: " + normalizedCode);
		}

		// Coupon creation by admin, continues after duplicate validation.
		Coupon coupon = Coupon.builder().code(normalizedCode).description(request.getDescription())
				.discountType(request.getDiscountType()).discountValue(request.getDiscountValue())
				.minOrderAmount(request.getMinOrderAmount()).maxDiscount(request.getMaxDiscount())
				.startDate(request.getStartDate()).expiryDate(request.getExpiryDate())
				.active(request.getActive() == null ? true : request.getActive()).build();

		Coupon savedCoupon = couponRepository.save(coupon);

		log.info("Coupon created successfully. couponId={}, code={}, discountType={}, active={}", savedCoupon.getId(),
				savedCoupon.getCode(), savedCoupon.getDiscountType(), savedCoupon.getActive());

		return mapToCouponResponse(savedCoupon); // save coupon
	}

	// get all active coupon
	public List<CouponResponse> getAllActiveCoupons() {
		log.info("Fetching all active coupons");

		List<CouponResponse> activeCoupons = couponRepository.findByActiveTrue().stream().map(this::mapToCouponResponse)
				.toList();

		log.info("Active coupons fetched successfully. count={}", activeCoupons.size());

		return activeCoupons;
	}

	// apply coupon
	public ApplyCouponResponse applyCoupon(ApplyCouponRequest request) {
		validateApplyCouponRequest(request);

		String normalizedCode = request.getCouponCode().trim().toUpperCase();

		log.info("Applying coupon. code={}, cartTotal={}", normalizedCode, request.getCartTotal());

		Coupon coupon = couponRepository.findByCodeIgnoreCase(normalizedCode).orElseThrow(() -> {
			log.warn("Coupon apply failed because coupon code is invalid. code={}", normalizedCode);
			return new RuntimeException("Invalid coupon code: " + normalizedCode);
		});

		validateCouponForApply(coupon, request.getCartTotal());

		BigDecimal discountAmount = calculateDiscount(coupon, request.getCartTotal());
		BigDecimal amountAfterDiscount = request.getCartTotal().subtract(discountAmount);

		log.info("Coupon applied successfully. code={}, cartTotal={}, discountAmount={}, amountAfterDiscount={}",
				coupon.getCode(), request.getCartTotal(), discountAmount, amountAfterDiscount);

		return ApplyCouponResponse.builder().couponCode(coupon.getCode()).cartTotal(request.getCartTotal())
				.discountAmount(discountAmount).amountAfterDiscount(amountAfterDiscount).valid(true)
				.message("Coupon applied successfully").build();
	}

	// validation checks for coupon
	private void validateCouponRequest(CouponRequest request) {
		if (request == null) {
			log.warn("Coupon creation failed because request body is null");
			throw new RuntimeException("Request body cannot be null");
		}

		if (request.getCode() == null || request.getCode().trim().isEmpty()) {
			log.warn("Coupon creation failed because coupon code is missing");
			throw new RuntimeException("Coupon code is required");
		}

		if (request.getDiscountType() == null) {
			log.warn("Coupon creation failed because discount type is missing. code={}", request.getCode());
			throw new RuntimeException("Discount type is required");
		}

		// Discount value must be positive.
		if (request.getDiscountValue() == null || request.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
			log.warn("Coupon creation failed because discount value is invalid. code={}, discountValue={}",
					request.getCode(), request.getDiscountValue());
			throw new RuntimeException("Discount value must be greater than zero");
		}

		// Minimum order amount can be zero, but not negative.
		if (request.getMinOrderAmount() == null || request.getMinOrderAmount().compareTo(BigDecimal.ZERO) < 0) {
			log.warn("Coupon creation failed because minimum order amount is invalid. code={}, minOrderAmount={}",
					request.getCode(), request.getMinOrderAmount());
			throw new RuntimeException("Minimum order amount cannot be negative");
		}

		// Percentage coupon cannot exceed 100%.
		if (request.getDiscountType() == DiscountType.PERCENTAGE
				&& request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
			log.warn(
					"Coupon creation failed because percentage discount is greater than 100. code={}, discountValue={}",
					request.getCode(), request.getDiscountValue());
			throw new RuntimeException("Percentage discount cannot be greater than 100");
		}
	}

	// when a customer enters coupon code in cart/checkout page and backend checks
	// whether the request is valid before applying discount.
	private void validateApplyCouponRequest(ApplyCouponRequest request) {
		if (request == null) {
			log.warn("Coupon apply failed because request body is null");
			throw new RuntimeException("Request body cannot be null");
		}

		if (request.getCouponCode() == null || request.getCouponCode().trim().isEmpty()) {
			log.warn("Coupon apply failed because coupon code is missing");
			throw new RuntimeException("Coupon code is required");
		}

		if (request.getCartTotal() == null || request.getCartTotal().compareTo(BigDecimal.ZERO) <= 0) {
			log.warn("Coupon apply failed because cart total is invalid. couponCode={}, cartTotal={}",
					request.getCouponCode(), request.getCartTotal());
			throw new RuntimeException("Cart total must be greater than zero");
		}
	}

	// checks coupon is active or expired
	private void validateCouponForApply(Coupon coupon, BigDecimal cartTotal) {
		if (!Boolean.TRUE.equals(coupon.getActive())) {
			log.warn("Coupon apply failed because coupon is inactive. code={}", coupon.getCode());
			throw new RuntimeException("Coupon is not active");
		}

		LocalDateTime now = LocalDateTime.now();

		if (coupon.getStartDate() != null && now.isBefore(coupon.getStartDate())) {
			log.warn("Coupon apply failed because coupon is not active yet. code={}, startDate={}", coupon.getCode(),
					coupon.getStartDate());
			throw new RuntimeException("Coupon is not active yet");
		}

		if (coupon.getExpiryDate() != null && now.isAfter(coupon.getExpiryDate())) {
			log.warn("Coupon apply failed because coupon has expired. code={}, expiryDate={}", coupon.getCode(),
					coupon.getExpiryDate());
			throw new RuntimeException("Coupon has expired");
		}

		if (cartTotal.compareTo(coupon.getMinOrderAmount()) < 0) {
			log.warn(
					"Coupon apply failed because cart total is below minimum order amount. code={}, cartTotal={}, minOrderAmount={}",
					coupon.getCode(), cartTotal, coupon.getMinOrderAmount());
			throw new RuntimeException("Minimum order amount required is " + coupon.getMinOrderAmount());
		}
	}

	// calculate applied coupon discount
	private BigDecimal calculateDiscount(Coupon coupon, BigDecimal cartTotal) {
		BigDecimal discountAmount;

		if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
			discountAmount = cartTotal.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100));
		} else {
			discountAmount = coupon.getDiscountValue();
		}

		log.debug("Coupon discount calculated before caps. code={}, discountType={}, cartTotal={}, discountAmount={}",
				coupon.getCode(), coupon.getDiscountType(), cartTotal, discountAmount);

		// If the coupon has a maximum discount limit, and the calculated discount is
		// greater than that limit, then reduce the discount to the maximum allowed
		// discount.
		if (coupon.getMaxDiscount() != null && discountAmount.compareTo(coupon.getMaxDiscount()) > 0) {
			log.debug("Coupon discount capped by maxDiscount. code={}, calculatedDiscount={}, maxDiscount={}",
					coupon.getCode(), discountAmount, coupon.getMaxDiscount());
			discountAmount = coupon.getMaxDiscount();
		}

		if (discountAmount.compareTo(cartTotal) > 0) {
			log.debug("Coupon discount capped by cart total. code={}, calculatedDiscount={}, cartTotal={}",
					coupon.getCode(), discountAmount, cartTotal);
			discountAmount = cartTotal;
		}

		return discountAmount;
	}

	// entity(database object) -> DTO mapping(object sent to the frontend)
	private CouponResponse mapToCouponResponse(Coupon coupon) {
		return CouponResponse.builder().id(coupon.getId()).code(coupon.getCode()).description(coupon.getDescription())
				.discountType(coupon.getDiscountType()).discountValue(coupon.getDiscountValue())
				.minOrderAmount(coupon.getMinOrderAmount()).maxDiscount(coupon.getMaxDiscount())
				.startDate(coupon.getStartDate()).expiryDate(coupon.getExpiryDate()).active(coupon.getActive()).build();
	}
}