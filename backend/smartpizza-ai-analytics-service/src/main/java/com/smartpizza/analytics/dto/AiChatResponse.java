package com.smartpizza.analytics.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {

    private Long userId;

    private String userMessage;

    private String reply;

    private String responseType;

    private List<String> suggestions;

    private LocalDateTime respondedAt;
}