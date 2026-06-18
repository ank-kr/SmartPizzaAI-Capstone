package com.smartpizza.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartpizza.core.dto.ApplyCouponRequest;
import com.smartpizza.core.dto.ApplyCouponResponse;
import com.smartpizza.core.dto.CouponRequest;
import com.smartpizza.core.dto.CouponResponse;
import com.smartpizza.core.entity.Coupon;
import com.smartpizza.core.enums.DiscountType;
import com.smartpizza.core.repository.CouponRepository;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

	@Mock
	private CouponRepository couponRepository;

	@InjectMocks
	private CouponService couponService;

	@Test
	void createCoupon_ShouldCreateCouponSuccessfully() {
		CouponRequest request = CouponRequest.builder().code("pizza20").description("20 percent discount")
				.discountType(DiscountType.PERCENTAGE).discountValue(BigDecimal.valueOf(20))
				.minOrderAmount(BigDecimal.valueOf(200)).maxDiscount(BigDecimal.valueOf(100))
				.startDate(LocalDateTime.now().minusDays(1)).expiryDate(LocalDateTime.now().plusDays(5)).active(true)
				.build();

		Coupon savedCoupon = Coupon.builder().id(1L).code("PIZZA20").description("20 percent discount")
				.discountType(DiscountType.PERCENTAGE).discountValue(BigDecimal.valueOf(20))
				.minOrderAmount(BigDecimal.valueOf(200)).maxDiscount(BigDecimal.valueOf(100))
				.startDate(request.getStartDate()).expiryDate(request.getExpiryDate()).active(true).build();

		when(couponRepository.existsByCodeIgnoreCase("PIZZA20")).thenReturn(false);
		when(couponRepository.save(any(Coupon.class))).thenReturn(savedCoupon);

		CouponResponse response = couponService.createCoupon(request);

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals("PIZZA20", response.getCode());
		assertEquals("20 percent discount", response.getDescription());
		assertEquals(DiscountType.PERCENTAGE, response.getDiscountType());
		assertEquals(0, BigDecimal.valueOf(20).compareTo(response.getDiscountValue()));
		assertEquals(true, response.getActive());

		verify(couponRepository, times(1)).existsByCodeIgnoreCase("PIZZA20");
		verify(couponRepository, times(1)).save(any(Coupon.class));
	}

	@Test
	void createCoupon_ShouldThrowException_WhenCouponAlreadyExists() {
		CouponRequest request = CouponRequest.builder().code("pizza20").description("20 percent discount")
				.discountType(DiscountType.PERCENTAGE).discountValue(BigDecimal.valueOf(20))
				.minOrderAmount(BigDecimal.valueOf(200)).maxDiscount(BigDecimal.valueOf(100)).active(true).build();

		when(couponRepository.existsByCodeIgnoreCase("PIZZA20")).thenReturn(true);

		RuntimeException exception = assertThrows(RuntimeException.class, () -> couponService.createCoupon(request));

		assertEquals("Coupon already exists with code: PIZZA20", exception.getMessage());

		verify(couponRepository, times(1)).existsByCodeIgnoreCase("PIZZA20");
		verify(couponRepository, never()).save(any(Coupon.class));
	}

	@Test
	void createCoupon_ShouldThrowException_WhenRequestIsNull() {
		RuntimeException exception = assertThrows(RuntimeException.class, () -> couponService.createCoupon(null));

		assertEquals("Request body cannot be null", exception.getMessage());

		verifyNoInteractions(couponRepository);
	}

	@Test
	void createCoupon_ShouldThrowException_WhenCouponCodeIsBlank() {
		CouponRequest request = CouponRequest.builder().code(" ").discountType(DiscountType.PERCENTAGE)
				.discountValue(BigDecimal.valueOf(20)).minOrderAmount(BigDecimal.valueOf(200)).build();

		RuntimeException exception = assertThrows(RuntimeException.class, () -> couponService.createCoupon(request));

		assertEquals("Coupon code is required", exception.getMessage());

		verifyNoInteractions(couponRepository);
	}

	@Test
	void createCoupon_ShouldThrowException_WhenDiscountTypeIsNull() {
		CouponRequest request = CouponRequest.builder().code("PIZZA20").discountValue(BigDecimal.valueOf(20))
				.minOrderAmount(BigDecimal.valueOf(200)).build();

		RuntimeException exception = assertThrows(RuntimeException.class, () -> couponService.createCoupon(request));

		assertEquals("Discount type is required", exception.getMessage());

		verifyNoInteractions(couponRepository);
	}

	@Test
	void createCoupon_ShouldThrowException_WhenDiscountValueIsZero() {
		CouponRequest request = CouponRequest.builder().code("PIZZA20").discountType(DiscountType.PERCENTAGE)
				.discountValue(BigDecimal.ZERO).minOrderAmount(BigDecimal.valueOf(200)).build();

		RuntimeException exception = assertThrows(RuntimeException.class, () -> couponService.createCoupon(request));

		assertEquals("Discount value must be greater than zero", exception.getMessage());

		verifyNoInteractions(couponRepository);
	}

	@Test
	void createCoupon_ShouldThrowException_WhenPercentageDiscountGreaterThan100() {
		CouponRequest request = CouponRequest.builder().code("PIZZA150").discountType(DiscountType.PERCENTAGE)
				.discountValue(BigDecimal.valueOf(150)).minOrderAmount(BigDecimal.valueOf(200)).build();

		RuntimeException exception = assertThrows(RuntimeException.class, () -> couponService.createCoupon(request));

		assertEquals("Percentage discount cannot be greater than 100", exception.getMessage());

		verifyNoInteractions(couponRepository);
	}

	@Test
	void getAllActiveCoupons_ShouldReturnActiveCoupons() {
		Coupon coupon1 = Coupon.builder().id(1L).code("PIZZA20").description("20 percent discount")
				.discountType(DiscountType.PERCENTAGE).discountValue(BigDecimal.valueOf(20))
				.minOrderAmount(BigDecimal.valueOf(200)).maxDiscount(BigDecimal.valueOf(100)).active(true).build();

		Coupon coupon2 = Coupon.builder().id(2L).code("FLAT50").description("Flat 50 discount")
				.discountType(DiscountType.FIXED).discountValue(BigDecimal.valueOf(50))
				.minOrderAmount(BigDecimal.valueOf(300)).maxDiscount(null).active(true).build();

		when(couponRepository.findByActiveTrue()).thenReturn(List.of(coupon1, coupon2));

		List<CouponResponse> responses = couponService.getAllActiveCoupons();

		assertNotNull(responses);
		assertEquals(2, responses.size());
		assertEquals("PIZZA20", responses.get(0).getCode());
		assertEquals("FLAT50", responses.get(1).getCode());

		verify(couponRepository, times(1)).findByActiveTrue();
	}

	@Test
	void applyCoupon_ShouldApplyPercentageCouponSuccessfully() {
		ApplyCouponRequest request = ApplyCouponRequest.builder().couponCode("pizza20")
				.cartTotal(BigDecimal.valueOf(500)).build();

		Coupon coupon = Coupon.builder().id(1L).code("PIZZA20").description("20 percent discount")
				.discountType(DiscountType.PERCENTAGE).discountValue(BigDecimal.valueOf(20))
				.minOrderAmount(BigDecimal.valueOf(200)).maxDiscount(BigDecimal.valueOf(150))
				.startDate(LocalDateTime.now().minusDays(1)).expiryDate(LocalDateTime.now().plusDays(5)).active(true)
				.build();

		when(couponRepository.findByCodeIgnoreCase("PIZZA20")).thenReturn(Optional.of(coupon));

		ApplyCouponResponse response = couponService.applyCoupon(request);

		assertNotNull(response);
		assertEquals("PIZZA20", response.getCouponCode());
		assertTrue(response.getValid());
		assertEquals("Coupon applied successfully", response.getMessage());
		assertEquals(0, BigDecimal.valueOf(500).compareTo(response.getCartTotal()));
		assertEquals(0, BigDecimal.valueOf(100).compareTo(response.getDiscountAmount()));
		assertEquals(0, BigDecimal.valueOf(400).compareTo(response.getAmountAfterDiscount()));

		verify(couponRepository, times(1)).findByCodeIgnoreCase("PIZZA20");
	}

	@Test
	void applyCoupon_ShouldApplyMaxDiscount_WhenPercentageDiscountExceedsMaxDiscount() {
		ApplyCouponRequest request = ApplyCouponRequest.builder().couponCode("PIZZA50")
				.cartTotal(BigDecimal.valueOf(1000)).build();

		Coupon coupon = Coupon.builder().id(1L).code("PIZZA50").description("50 percent discount")
				.discountType(DiscountType.PERCENTAGE).discountValue(BigDecimal.valueOf(50))
				.minOrderAmount(BigDecimal.valueOf(200)).maxDiscount(BigDecimal.valueOf(200))
				.startDate(LocalDateTime.now().minusDays(1)).expiryDate(LocalDateTime.now().plusDays(5)).active(true)
				.build();

		when(couponRepository.findByCodeIgnoreCase("PIZZA50")).thenReturn(Optional.of(coupon));

		ApplyCouponResponse response = couponService.applyCoupon(request);

		assertNotNull(response);
		assertEquals(0, BigDecimal.valueOf(200).compareTo(response.getDiscountAmount()));
		assertEquals(0, BigDecimal.valueOf(800).compareTo(response.getAmountAfterDiscount()));

		verify(couponRepository, times(1)).findByCodeIgnoreCase("PIZZA50");
	}

	@Test
	void applyCoupon_ShouldApplyFlatCouponSuccessfully() {
		ApplyCouponRequest request = ApplyCouponRequest.builder().couponCode("FLAT50")
				.cartTotal(BigDecimal.valueOf(500)).build();

		Coupon coupon = Coupon.builder().id(2L).code("FLAT50").description("Flat 50 discount")
				.discountType(DiscountType.FIXED).discountValue(BigDecimal.valueOf(50))
				.minOrderAmount(BigDecimal.valueOf(200)).maxDiscount(null).startDate(LocalDateTime.now().minusDays(1))
				.expiryDate(LocalDateTime.now().plusDays(5)).active(true).build();

		when(couponRepository.findByCodeIgnoreCase("FLAT50")).thenReturn(Optional.of(coupon));

		ApplyCouponResponse response = couponService.applyCoupon(request);

		assertNotNull(response);
		assertEquals("FLAT50", response.getCouponCode());
		assertEquals(0, BigDecimal.valueOf(50).compareTo(response.getDiscountAmount()));
		assertEquals(0, BigDecimal.valueOf(450).compareTo(response.getAmountAfterDiscount()));

		verify(couponRepository, times(1)).findByCodeIgnoreCase("FLAT50");
	}

	@Test
	void applyCoupon_ShouldNotDiscountMoreThanCartTotal() {
		ApplyCouponRequest request = ApplyCouponRequest.builder().couponCode("FLAT1000")
				.cartTotal(BigDecimal.valueOf(300)).build();

		Coupon coupon = Coupon.builder().id(3L).code("FLAT1000").description("Flat 1000 discount")
				.discountType(DiscountType.FIXED).discountValue(BigDecimal.valueOf(1000))
				.minOrderAmount(BigDecimal.valueOf(100)).maxDiscount(null).startDate(LocalDateTime.now().minusDays(1))
				.expiryDate(LocalDateTime.now().plusDays(5)).active(true).build();

		when(couponRepository.findByCodeIgnoreCase("FLAT1000")).thenReturn(Optional.of(coupon));

		ApplyCouponResponse response = couponService.applyCoupon(request);

		assertNotNull(response);
		assertEquals(0, BigDecimal.valueOf(300).compareTo(response.getDiscountAmount()));
		assertEquals(0, BigDecimal.ZERO.compareTo(response.getAmountAfterDiscount()));

		verify(couponRepository, times(1)).findByCodeIgnoreCase("FLAT1000");
	}

	@Test
	void applyCoupon_ShouldThrowException_WhenCouponCodeIsInvalid() {
		ApplyCouponRequest request = ApplyCouponRequest.builder().couponCode("INVALID")
				.cartTotal(BigDecimal.valueOf(500)).build();

		when(couponRepository.findByCodeIgnoreCase("INVALID")).thenReturn(Optional.empty());

		RuntimeException exception = assertThrows(RuntimeException.class, () -> couponService.applyCoupon(request));

		assertEquals("Invalid coupon code: INVALID", exception.getMessage());

		verify(couponRepository, times(1)).findByCodeIgnoreCase("INVALID");
	}

	@Test
	void applyCoupon_ShouldThrowException_WhenCouponIsInactive() {
		ApplyCouponRequest request = ApplyCouponRequest.builder().couponCode("PIZZA20")
				.cartTotal(BigDecimal.valueOf(500)).build();

		Coupon coupon = Coupon.builder().id(1L).code("PIZZA20").discountType(DiscountType.PERCENTAGE)
				.discountValue(BigDecimal.valueOf(20)).minOrderAmount(BigDecimal.valueOf(200)).active(false).build();

		when(couponRepository.findByCodeIgnoreCase("PIZZA20")).thenReturn(Optional.of(coupon));

		RuntimeException exception = assertThrows(RuntimeException.class, () -> couponService.applyCoupon(request));

		assertEquals("Coupon is not active", exception.getMessage());

		verify(couponRepository, times(1)).findByCodeIgnoreCase("PIZZA20");
	}

	@Test
	void applyCoupon_ShouldThrowException_WhenCouponIsNotActiveYet() {
		ApplyCouponRequest request = ApplyCouponRequest.builder().couponCode("FUTURE20")
				.cartTotal(BigDecimal.valueOf(500)).build();

		Coupon coupon = Coupon.builder().id(1L).code("FUTURE20").discountType(DiscountType.PERCENTAGE)
				.discountValue(BigDecimal.valueOf(20)).minOrderAmount(BigDecimal.valueOf(200))
				.startDate(LocalDateTime.now().plusDays(1)).expiryDate(LocalDateTime.now().plusDays(10)).active(true)
				.build();

		when(couponRepository.findByCodeIgnoreCase("FUTURE20")).thenReturn(Optional.of(coupon));

		RuntimeException exception = assertThrows(RuntimeException.class, () -> couponService.applyCoupon(request));

		assertEquals("Coupon is not active yet", exception.getMessage());

		verify(couponRepository, times(1)).findByCodeIgnoreCase("FUTURE20");
	}

	@Test
	void applyCoupon_ShouldThrowException_WhenCouponIsExpired() {
		ApplyCouponRequest request = ApplyCouponRequest.builder().couponCode("OLD20").cartTotal(BigDecimal.valueOf(500))
				.build();

		Coupon coupon = Coupon.builder().id(1L).code("OLD20").discountType(DiscountType.PERCENTAGE)
				.discountValue(BigDecimal.valueOf(20)).minOrderAmount(BigDecimal.valueOf(200))
				.startDate(LocalDateTime.now().minusDays(10)).expiryDate(LocalDateTime.now().minusDays(1)).active(true)
				.build();

		when(couponRepository.findByCodeIgnoreCase("OLD20")).thenReturn(Optional.of(coupon));

		RuntimeException exception = assertThrows(RuntimeException.class, () -> couponService.applyCoupon(request));

		assertEquals("Coupon has expired", exception.getMessage());

		verify(couponRepository, times(1)).findByCodeIgnoreCase("OLD20");
	}

	@Test
	void applyCoupon_ShouldThrowException_WhenCartTotalIsLessThanMinOrderAmount() {
		ApplyCouponRequest request = ApplyCouponRequest.builder().couponCode("PIZZA20")
				.cartTotal(BigDecimal.valueOf(100)).build();

		Coupon coupon = Coupon.builder().id(1L).code("PIZZA20").discountType(DiscountType.PERCENTAGE)
				.discountValue(BigDecimal.valueOf(20)).minOrderAmount(BigDecimal.valueOf(500))
				.startDate(LocalDateTime.now().minusDays(1)).expiryDate(LocalDateTime.now().plusDays(5)).active(true)
				.build();

		when(couponRepository.findByCodeIgnoreCase("PIZZA20")).thenReturn(Optional.of(coupon));

		RuntimeException exception = assertThrows(RuntimeException.class, () -> couponService.applyCoupon(request));

		assertEquals("Minimum order amount required is 500", exception.getMessage());

		verify(couponRepository, times(1)).findByCodeIgnoreCase("PIZZA20");
	}
}