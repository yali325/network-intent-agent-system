package com.yali.mactav.orchestrator.remote.card;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.agent.AgentCard;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Nacos Config backed AgentCard registry client for Orchestrator discovery.
 *
 * <p>The implementation reads AgentCard JSON through the Nacos OpenAPI. It is a
 * registry adapter only and must not perform workflow orchestration or call
 * concrete agent modules directly.</p>
 */
public class NacosAgentCardRegistryClient implements AgentCardRegistryClient {

    public static final String DEFAULT_GROUP = "MAC_TAV_AGENT_CARDS";

    private static final String DATA_ID_PREFIX = "mactav.agent-card.";

    private static final String DATA_ID_SUFFIX = ".json";

    private final String serverAddr;

    private final String group;

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;

    private final Duration requestTimeout;

    public NacosAgentCardRegistryClient(String serverAddr, String group, ObjectMapper objectMapper) {
        this(serverAddr, group, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build(), Duration.ofSeconds(5));
    }

    public NacosAgentCardRegistryClient(String serverAddr,
                                        String group,
                                        ObjectMapper objectMapper,
                                        HttpClient httpClient,
                                        Duration requestTimeout) {
        this.serverAddr = normalizeServerAddr(serverAddr);
        this.group = group == null || group.isBlank() ? DEFAULT_GROUP : group;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.requestTimeout = requestTimeout == null ? Duration.ofSeconds(5) : requestTimeout;
    }

    @Override
    public Optional<AgentCard> findByAgentName(String agentName) {
        if (agentName == null || agentName.isBlank()) {
            return Optional.empty();
        }
        String dataId = DATA_ID_PREFIX + agentName + DATA_ID_SUFFIX;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverAddr + "/nacos/v1/cs/configs?dataId="
                        + encode(dataId) + "&group=" + encode(group)))
                .timeout(requestTimeout)
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404 || response.body() == null || response.body().isBlank()) {
                return Optional.empty();
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(
                        ErrorCode.AGENT_DISCOVERY_FAILED,
                        "Nacos AgentCard lookup failed with HTTP " + response.statusCode());
            }
            return Optional.of(objectMapper.readValue(response.body(), AgentCard.class));
        }
        catch (HttpTimeoutException ex) {
            throw new BusinessException(ErrorCode.AGENT_DISCOVERY_FAILED, "Nacos AgentCard lookup timed out", ex);
        }
        catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.AGENT_DISCOVERY_FAILED, "Nacos AgentCard JSON is invalid", ex);
        }
        catch (IOException ex) {
            throw new BusinessException(ErrorCode.AGENT_DISCOVERY_FAILED, "Nacos AgentCard lookup failed", ex);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.AGENT_DISCOVERY_FAILED, "Nacos AgentCard lookup interrupted", ex);
        }
    }

    @Override
    public List<AgentCard> listAvailableAgents() {
        return findByAgentName("IntentAgent").stream().toList();
    }

    private String normalizeServerAddr(String value) {
        String normalized = value == null || value.isBlank() ? "http://127.0.0.1:8848" : value;
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "http://" + normalized;
        }
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
