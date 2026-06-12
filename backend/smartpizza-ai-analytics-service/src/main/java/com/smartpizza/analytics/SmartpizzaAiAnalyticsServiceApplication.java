package com.smartpizza.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class SmartpizzaAiAnalyticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartpizzaAiAnalyticsServiceApplication.class, args);
    }
}	