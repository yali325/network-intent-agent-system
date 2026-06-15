package com.yali.mactav.execution.config;

import com.yali.mactav.model.execution.ExecutionMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration contract for Phase 6 execution settings.
 *
 * <p>This class declares execution properties only. It does not create an
 * adapter, HTTP client, executor connection, Orchestrator hook, or Controller.</p>
 */
@ConfigurationProperties(prefix = "mactav.execution")
public class ExecutionProperties {

    private ExecutionMode mode = ExecutionMode.STRUCTURE_VALIDATION;

    private boolean dryRunEnabled = true;

    private MininetRyuProperties mininetRyu = new MininetRyuProperties();

    public ExecutionMode getMode() {
        return mode;
    }

    public void setMode(ExecutionMode mode) {
        this.mode = mode;
    }

    public boolean isDryRunEnabled() {
        return dryRunEnabled;
    }

    public void setDryRunEnabled(boolean dryRunEnabled) {
        this.dryRunEnabled = dryRunEnabled;
    }

    public MininetRyuProperties getMininetRyu() {
        return mininetRyu;
    }

    public void setMininetRyu(MininetRyuProperties mininetRyu) {
        this.mininetRyu = mininetRyu;
    }

    public ExecutionMode effectiveMode() {
        return mode == null ? ExecutionMode.STRUCTURE_VALIDATION : mode;
    }

    /**
     * Mininet/Ryu executor configuration contract.
     */
    public static class MininetRyuProperties {

        private boolean enabled = false;

        private String baseUrl = "http://127.0.0.1:18091";

        private int connectTimeoutMs = 3000;

        private int readTimeoutMs = 60000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }
    }
}
