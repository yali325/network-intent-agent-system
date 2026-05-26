package com.yali.mactav.orchestrator.remote.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.a2a.A2aRequest;
import com.yali.mactav.model.a2a.A2aResponse;
import com.yali.mactav.model.agent.AgentCard;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

/**
 * HTTP JSON implementation of the A2aClient transport boundary.
 *
 * <p>The client posts the shared A2A envelope to an AgentCard service endpoint.
 * It never constructs prompts, calls model APIs, parses business DTOs, or writes
 * NetworkWorkspace state.</p>
 */
public class HttpA2aClient implements A2aClient {

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;

    private final Duration requestTimeout;

    public HttpA2aClient(ObjectMapper objectMapper) {
        this(objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build(), Duration.ofSeconds(10));
    }

    public HttpA2aClient(ObjectMapper objectMapper, HttpClient httpClient, Duration requestTimeout) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.requestTimeout = requestTimeout == null ? Duration.ofSeconds(10) : requestTimeout;
    }

    @Override
    public A2aResponse call(A2aRequest request, AgentCard agentCard) {
        validate(agentCard);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(agentCard.getServiceEndpoint()))
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(serialize(request)))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(
                        ErrorCode.A2A_CALL_FAILED,
                        "A2A endpoint returned HTTP " + response.statusCode());
            }
            return objectMapper.readValue(response.body(), A2aResponse.class);
        }
        catch (HttpTimeoutException ex) {
            throw new BusinessException(ErrorCode.REMOTE_AGENT_TIMEOUT, "A2A request timed out", ex);
        }
        catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.A2A_RESPONSE_INVALID, "A2A response JSON is invalid", ex);
        }
        catch (IOException ex) {
            throw new BusinessException(ErrorCode.A2A_CALL_FAILED, "A2A HTTP request failed", ex);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.A2A_CALL_FAILED, "A2A HTTP request interrupted", ex);
        }
    }

    private void validate(AgentCard agentCard) {
        if (agentCard == null || agentCard.getServiceEndpoint() == null || agentCard.getServiceEndpoint().isBlank()) {
            throw new BusinessException(ErrorCode.AGENT_SERVICE_UNAVAILABLE, "Agent service endpoint is missing");
        }
    }

    private String serialize(A2aRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        }
        catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.A2A_CALL_FAILED, "A2A request JSON serialization failed", ex);
        }
    }
}
