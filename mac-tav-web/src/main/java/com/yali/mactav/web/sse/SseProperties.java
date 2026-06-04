package com.yali.mactav.web.sse;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Runtime configuration for Web SSE emitters and Redis event subscription.
 */
@Data
@ConfigurationProperties(prefix = "mactav.sse")
public class SseProperties {

    private boolean enabled = true;

    private long timeoutMs = Duration.ofMinutes(30).toMillis();

    private Duration emitterTimeout = Duration.ofMinutes(30);

    private String redisChannelPrefix = "mactav:events:";

    private String redisTopicPrefix;

    public long resolvedTimeoutMs() {
        return timeoutMs > 0 ? timeoutMs : emitterTimeout.toMillis();
    }

    public String resolvedRedisChannelPrefix() {
        return redisChannelPrefix == null || redisChannelPrefix.isBlank()
                ? redisTopicPrefix
                : redisChannelPrefix;
    }
}
