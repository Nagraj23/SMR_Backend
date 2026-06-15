package com.smr.ride.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl("http://127.0.0.1:8000") // 🎯 Your FastAPI container's local network gate
                .build();
    }
}