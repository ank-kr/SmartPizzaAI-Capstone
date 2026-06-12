package com.smartpizza.analytics.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartpizza.analytics.dto.ComboSuggestionResponse;
import com.smartpizza.analytics.dto.RecommendationResponse;
import com.smartpizza.analytics.service.RecommendationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
@CrossOrigin(origins = "*")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI Analytics Service is running");
    }

    @GetMapping("/recommendations/{userId}")
    public ResponseEntity<List<RecommendationResponse>> getPersonalizedRecommendations(@PathVariable Long userId) {
        List<RecommendationResponse> response = recommendationService.getPersonalizedRecommendations(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/trending")
    public ResponseEntity<List<RecommendationResponse>> getTrendingRecommendations() {
        List<RecommendationResponse> response = recommendationService.getTrendingRecommendations();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/combo-suggestions/{userId}")
    public ResponseEntity<List<ComboSuggestionResponse>> getComboSuggestions(@PathVariable Long userId) {
        List<ComboSuggestionResponse> response = recommendationService.getComboSuggestions(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/weather-recommendations/{userId}")
    public ResponseEntity<List<RecommendationResponse>> getWeatherRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "NORMAL") String weather
    ) {
        List<RecommendationResponse> response = recommendationService.getWeatherRecommendations(userId, weather);
        return ResponseEntity.ok(response);
    }
}