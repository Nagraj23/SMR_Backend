package com.smr.ride.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync // 🚀 Turns on background thread spawning capabilities across the JVM instance
public class AsyncConfig {

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);        // Idle worker baseline
        executor.setMaxPoolSize(25);       // Upper limit scale constraint during traffic spikes
        executor.setQueueCapacity(200);    // Backlog capacity before blocking
        executor.setThreadNamePrefix("NotifyWorker-");
        executor.initialize();
        return executor;
    }
}