package com.yali.mactav.intent.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Bridges the required aliApi-key environment variable into Spring AI DashScope configuration.
 *
 * <p>The bridge only copies the value into the in-memory Environment and never
 * logs or stores the key. It deliberately does not support legacy key names.</p>
 */
public class IntentAgentApiKeyEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "mactavIntentAgentApiKeyBridge";

    private static final String DASHSCOPE_API_KEY_PROPERTY = "spring.ai.dashscope.api-key";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String existingSpringValue = environment.getProperty(DASHSCOPE_API_KEY_PROPERTY);
        if (existingSpringValue != null && !existingSpringValue.isBlank()) {
            return;
        }
        String apiKey = System.getenv(IntentAgentApiKeyResolver.API_KEY_ENV_NAME);
        if (apiKey == null || apiKey.isBlank()) {
            return;
        }
        Map<String, Object> properties = new HashMap<>();
        properties.put(DASHSCOPE_API_KEY_PROPERTY, apiKey);
        properties.put(IntentAgentApiKeyResolver.API_KEY_ENV_NAME, apiKey);
        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
