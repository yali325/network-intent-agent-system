package com.yali.mactav.web.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Spring wiring for task event SSE delivery through Redis pub/sub.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SseProperties.class)
public class SseConfiguration {

    @Bean
    public SseEventMapper sseEventMapper(ObjectMapper objectMapper) {
        return new SseEventMapper(objectMapper);
    }

    @Bean
    public SseEmitterRegistry sseEmitterRegistry(SseEventMapper eventMapper) {
        return new SseEmitterRegistry(eventMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "mactav.sse.enabled", havingValue = "true", matchIfMissing = true)
    public RedisWorkspaceEventSubscriber redisWorkspaceEventSubscriber(SseEmitterRegistry emitterRegistry,
                                                                       SseEventMapper eventMapper) {
        return new RedisWorkspaceEventSubscriber(emitterRegistry, eventMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "mactav.sse.enabled", havingValue = "true", matchIfMissing = true)
    public RedisMessageListenerContainer redisWorkspaceEventListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisWorkspaceEventSubscriber subscriber,
            SseProperties properties) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                subscriber,
                new PatternTopic(properties.resolvedRedisChannelPrefix() + "*"));
        return container;
    }
}
