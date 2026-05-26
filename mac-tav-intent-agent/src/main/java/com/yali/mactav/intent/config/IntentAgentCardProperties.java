package com.yali.mactav.intent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for publishing the IntentAgent AgentCard to Nacos.
 *
 * <p>The properties describe service discovery metadata only. They do not
 * control model prompts, parser behavior, or workflow state.</p>
 */
@Data
@ConfigurationProperties(prefix = "mactav.agent-card.intent")
public class IntentAgentCardProperties {

    private boolean publishEnabled = true;

    private String nacosServerAddr = "http://127.0.0.1:8848";

    private String nacosGroup = "MAC_TAV_AGENT_CARDS";

    private String nacosDataId = "mactav.agent-card.IntentAgent.json";

    private String serviceEndpoint;

    private String serviceHost = "127.0.0.1";

    private int servicePort = 18081;

    private String a2aPath = "/internal/a2a/intent/invoke";

    private String version = "0.0.1-SNAPSHOT";

    public String effectiveServiceEndpoint() {
        if (serviceEndpoint != null && !serviceEndpoint.isBlank()) {
            return serviceEndpoint;
        }
        String normalizedPath = a2aPath == null || a2aPath.isBlank() ? "/internal/a2a/intent/invoke" : a2aPath;
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        return "http://" + serviceHost + ":" + servicePort + normalizedPath;
    }

    public String effectiveNacosServerAddr() {
        if (nacosServerAddr == null || nacosServerAddr.isBlank()) {
            return "http://127.0.0.1:8848";
        }
        String normalized = nacosServerAddr.startsWith("http://") || nacosServerAddr.startsWith("https://")
                ? nacosServerAddr
                : "http://" + nacosServerAddr;
        return normalized.endsWith("/")
                ? normalized.substring(0, normalized.length() - 1)
                : normalized;
    }
}
