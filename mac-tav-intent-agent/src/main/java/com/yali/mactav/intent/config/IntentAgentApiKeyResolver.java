package com.yali.mactav.intent.config;

import java.util.Map;
import java.util.Optional;

/**
 * Resolves DashScope API keys for manual real-model validation.
 *
 * <p>The resolver reads environment variables and JVM system properties by
 * name, but never stores or logs the key. Runtime ChatModel configuration still
 * belongs to Spring AI Alibaba auto-configuration.</p>
 */
public final class IntentAgentApiKeyResolver {

    public static final String API_KEY_ENV_NAME = "aliApi-key";

    private IntentAgentApiKeyResolver() {
    }

    public static Optional<String> resolve() {
        return resolve(System.getenv());
    }

    static Optional<String> resolve(Map<String, String> environment) {
        String environmentValue = environment == null ? null : environment.get(API_KEY_ENV_NAME);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return Optional.of(environmentValue);
        }
        return Optional.empty();
    }
}
