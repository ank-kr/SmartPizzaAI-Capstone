package com.smartpizza.core.controller;

import com.smartpizza.core.dto.ApplyCouponRequest;
import com.smartpizza.core.dto.ApplyCouponResponse;
import com.smartpizza.core.dto.CouponRequest;
import com.smartpizza.core.dto.CouponResponse;
import com.smartpizza.core.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/admin/coupons")
    public ResponseEntity<CouponResponse> createCoupon(@RequestBody CouponRequest request) {
        CouponResponse response = couponService.createCoupon(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/coupons")
    public ResponseEntity<List<CouponResponse>> getAllActiveCoupons() {
        List<CouponResponse> response = couponService.getAllActiveCoupons();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/coupons/apply")
    public ResponseEntity<ApplyCouponResponse> applyCoupon(@RequestBody ApplyCouponRequest request) {
        ApplyCouponResponse response = couponService.applyCoupon(request);
        return ResponseEntity.ok(response);
    }
}