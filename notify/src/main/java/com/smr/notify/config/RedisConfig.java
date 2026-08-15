package com.smr.notify.config;

import com.smr.notify.listener.RedisEventListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;


@Configuration
public class RedisConfig {

    @Bean
    public ChannelTopic rideTopic() {
        return new ChannelTopic("smr:notifications");
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory ,  RedisEventListener redisEventListener,
            ChannelTopic rideTopic) {

        RedisMessageListenerContainer container =
                new RedisMessageListenerContainer();

        container.setConnectionFactory(connectionFactory);

        container.addMessageListener(redisEventListener, rideTopic);

        return container;
    }
}