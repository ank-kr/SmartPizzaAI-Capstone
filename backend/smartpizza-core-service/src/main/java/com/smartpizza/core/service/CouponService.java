package com.smartpizza.core.service;

import com.smartpizza.core.dto.ApplyCouponRequest;
import com.smartpizza.core.dto.ApplyCouponResponse;
import com.smartpizza.core.dto.CouponRequest;
import com.smartpizza.core.dto.CouponResponse;
import com.smartpizza.core.entity.Coupon;
import com.smartpizza.core.enums.DiscountType;
import com.smartpizza.core.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponResponse createCoupon(CouponRequest request) {
        validateCouponRequest(request);

        String normalizedCode = request.getCode().trim().toUpperCase();

        if (couponRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new RuntimeException("Coupon already exists with code: " + normalizedCode);
        }

        Coupon coupon = Coupon.builder()
                .code(normalizedCode)
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minOrderAmount(request.getMinOrderAmount())
                .maxDiscount(request.getMaxDiscount())
                .startDate(request.getStartDate())
                .expiryDate(request.getExpiryDate())
                .active(request.getActive() == null ? true : request.getActive())
                .build();

        Coupon savedCoupon = couponRepository.save(coupon);

        return mapToCouponResponse(savedCoupon);
    }

    public List<CouponResponse> getAllActiveCoupons() {
        return couponRepository.findByActiveTrue()
                .stream()
                .map(this::mapToCouponResponse)
                .toList();
    }

    public ApplyCouponResponse applyCoupon(ApplyCouponRequest request) {
        validateApplyCouponRequest(request);

        String normalizedCode = request.getCouponCode().trim().toUpperCase();

        Coupon coupon = couponRepository.findByCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new RuntimeException("Invalid coupon code: " + normalizedCode));

        validateCouponForApply(coupon, request.getCartTotal());

        BigDecimal discountAmount = calculateDiscount(coupon, request.getCartTotal());
        BigDecimal amountAfterDiscount = request.getCartTotal().subtract(discountAmount);

        return ApplyCouponResponse.builder()
                .couponCode(coupon.getCode())
                .cartTotal(request.getCartTotal())
                .discountAmount(discountAmount)
                .amountAfterDiscount(amountAfterDiscount)
                .valid(true)
                .message("Coupon applied successfully")
                .build();
    }

    private void validateCouponRequest(CouponRequest request) {
        if (request == null) {
            throw new RuntimeException("Request body cannot be null");
        }

        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw new RuntimeException("Coupon code is required");
        }

        if (request.getDiscountType() == null) {
            throw new RuntimeException("Discount type is required");
        }

        if (request.getDiscountValue() == null || request.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Discount value must be greater than zero");
        }

        if (request.getMinOrderAmount() == null || request.getMinOrderAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Minimum order amount cannot be negative");
        }

        if (request.getDiscountType() == DiscountType.PERCENTAGE
                && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new RuntimeException("Percentage discount cannot be greater than 100");
        }
    }

    private void validateApplyCouponRequest(ApplyCouponRequest request) {
        if (request == null) {
            throw new RuntimeException("Request body cannot be null");
        }

        if (request.getCouponCode() == null || request.getCouponCode().trim().isEmpty()) {
            throw new RuntimeException("Coupon code is required");
        }

        if (request.getCartTotal() == null || request.getCartTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Cart total must be greater than zero");
        }
    }

    private void validateCouponForApply(Coupon coupon, BigDecimal cartTotal) {
        if (!Boolean.TRUE.equals(coupon.getActive())) {
            throw new RuntimeException("Coupon is not active");
        }

        LocalDateTime now = LocalDateTime.now();

        if (coupon.getStartDate() != null && now.isBefore(coupon.getStartDate())) {
            throw new RuntimeException("Coupon is not active yet");
        }

        if (coupon.getExpiryDate() != null && now.isAfter(coupon.getExpiryDate())) {
            throw new RuntimeException("Coupon has expired");
        }

        if (cartTotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new RuntimeException("Minimum order amount required is " + coupon.getMinOrderAmount());
        }
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal cartTotal) {
        BigDecimal discountAmount;

        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discountAmount = cartTotal
                    .multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100));
        } else {
            discountAmount = coupon.getDiscountValue();
        }

        if (coupon.getMaxDiscount() != null && discountAmount.compareTo(coupon.getMaxDiscount()) > 0) {
            discountAmount = coupon.getMaxDiscount();
        }

        if (discountAmount.compareTo(cartTotal) > 0) {
            discountAmount = cartTotal;
        }

        return discountAmount;
    }

    private CouponResponse mapToCouponResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minOrderAmount(coupon.getMinOrderAmount())
                .maxDiscount(coupon.getMaxDiscount())
                .startDate(coupon.getStartDate())
                .expiryDate(coupon.getExpiryDate())
                .active(coupon.getActive())
                .build();
    }
}