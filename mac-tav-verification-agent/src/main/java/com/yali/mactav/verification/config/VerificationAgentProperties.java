package com.yali.mactav.verification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the real MAC-TAV VerificationAgent wrapper.
 */
@ConfigurationProperties(prefix = "mactav.agents.verification")
public class VerificationAgentProperties {

    private boolean enabled = true;

    private int runLimit = 6;

    private String promptPath = "prompts/verification-agent-prompt.md";

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

    public int effectiveRunLimit() {
        return runLimit > 0 ? runLimit : 6;
    }

    public String effectivePromptPath() {
        return promptPath == null || promptPath.isBlank()
                ? "prompts/verification-agent-prompt.md"
                : promptPath;
    }
}
