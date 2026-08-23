package com.smr.ride.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl("http://10.143.177.135:8000") // 🎯 Your FastAPI container's local network gate
                .build();
    }
}