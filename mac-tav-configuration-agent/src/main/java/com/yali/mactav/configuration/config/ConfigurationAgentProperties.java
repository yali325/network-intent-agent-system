package com.yali.mactav.configuration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the real MAC-TAV ConfigurationAgent wrapper.
 */
@ConfigurationProperties(prefix = "mactav.agents.configuration")
public class ConfigurationAgentProperties {

    private boolean enabled = true;

    private int runLimit = 8;

    private String promptPath = "prompts/configuration-agent-prompt.md";

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
        return runLimit > 0 ? runLimit : 8;
    }

    public String effectivePromptPath() {
        return promptPath == null || promptPath.isBlank()
                ? "prompts/configuration-agent-prompt.md"
                : promptPath;
    }
}
