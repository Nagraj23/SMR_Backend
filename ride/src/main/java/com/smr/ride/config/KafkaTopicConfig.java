package com.smr.ride.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${smr.kafka.topics.ride-lifecycle}")
    private String rideLifecycleTopic;

    @Bean
    public NewTopic rideLifecycleTopic() {
        return TopicBuilder.name(rideLifecycleTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}