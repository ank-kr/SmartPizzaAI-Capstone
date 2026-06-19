package com.smartpizza.analytics.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.smartpizza.analytics.dto.AiChatRequest;
import com.smartpizza.analytics.dto.AiChatResponse;
import com.smartpizza.analytics.service.AiChatService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping
    public ResponseEntity<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        AiChatResponse response = aiChatService.chat(request);
        return ResponseEntity.ok(response);
    }
}