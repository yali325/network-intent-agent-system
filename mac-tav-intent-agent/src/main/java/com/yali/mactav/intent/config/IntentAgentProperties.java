package com.yali.mactav.intent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the real MAC-TAV IntentAgent wrapper.
 *
 * <p>The properties belong to mac-tav-intent-agent and configure only local
 * Agent behavior. They must not contain API keys or Orchestrator/Web wiring.</p>
 */
@ConfigurationProperties(prefix = "mactav.agents.intent")
public class IntentAgentProperties {

    private boolean enabled = true;

    private int runLimit = 6;

    private String promptPath = "prompts/intent-agent-prompt.md";

    private boolean manualModelValidationEnabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRunLimit() {
        return runLimit;
    }

    public void setRunLimit(int runLimit) {
        this.runLimit = runLimit;
    }

    public String getPromptPath() {
        return promptPath;
    }

    public void setPromptPath(String promptPath) {
        this.promptPath = promptPath;
    }

    public boolean isManualModelValidationEnabled() {
        return manualModelValidationEnabled;
    }

    public void setManualModelValidationEnabled(boolean manualModelValidationEnabled) {
        this.manualModelValidationEnabled = manualModelValidationEnabled;
    }

    public int effectiveRunLimit() {
        return runLimit > 0 ? runLimit : 6;
    }

    public String effectivePromptPath() {
        return promptPath == null || promptPath.isBlank() ? "prompts/intent-agent-prompt.md" : promptPath;
    }
}
