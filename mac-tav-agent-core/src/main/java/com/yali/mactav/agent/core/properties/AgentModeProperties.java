package com.yali.mactav.agent.core.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mactav.agent")
public class AgentModeProperties {

    public static final String INTENT_MODE_MOCK = "mock";
    public static final String INTENT_MODE_LLM = "llm";

    private String intentMode = INTENT_MODE_MOCK;

    public String getIntentMode() {
        return intentMode;
    }

    public void setIntentMode(String intentMode) {
        this.intentMode = normalize(intentMode);
    }

    public boolean isMockIntentMode() {
        return INTENT_MODE_MOCK.equals(intentMode);
    }

    public boolean isLlmIntentMode() {
        return INTENT_MODE_LLM.equals(intentMode);
    }

    private String normalize(String mode) {
        if (mode == null || mode.isBlank()) {
            return INTENT_MODE_MOCK;
        }
        return mode.trim().toLowerCase();
    }
}
