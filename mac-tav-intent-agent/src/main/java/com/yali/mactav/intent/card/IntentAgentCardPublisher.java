package com.yali.mactav.intent.card;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.intent.config.IntentAgentCardProperties;
import com.yali.mactav.model.agent.AgentCard;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * Publishes the IntentAgent AgentCard to Nacos Config through the Nacos OpenAPI.
 *
 * <p>This component is best-effort on startup: failed publication is logged as
 * metadata unavailability, while the service process remains alive for local
 * diagnostics. It never logs credentials or model request details.</p>
 */
public class IntentAgentCardPublisher {

    private static final Logger log = LoggerFactory.getLogger(IntentAgentCardPublisher.class);

    private final IntentAgentCardFactory cardFactory;

    private final IntentAgentCardProperties properties;

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;

    public IntentAgentCardPublisher(IntentAgentCardFactory cardFactory,
                                    IntentAgentCardProperties properties,
                                    ObjectMapper objectMapper) {
        this(cardFactory, properties, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build());
    }

    IntentAgentCardPublisher(IntentAgentCardFactory cardFactory,
                             IntentAgentCardProperties properties,
                             ObjectMapper objectMapper,
                             HttpClient httpClient) {
        this.cardFactory = cardFactory;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void publishOnReady() {
        if (!properties.isPublishEnabled()) {
            return;
        }
        try {
            publish();
        }
        catch (RuntimeException ex) {
            log.warn("IntentAgent AgentCard publication failed: {}", ex.getMessage());
        }
    }

    public void publish() {
        AgentCard agentCard = cardFactory.create(properties);
        String body = formBody(agentCard);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.effectiveNacosServerAddr() + "/nacos/v1/cs/configs"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Nacos returned HTTP " + response.statusCode());
            }
            log.info("IntentAgent AgentCard published to Nacos dataId={}, group={}",
                    properties.getNacosDataId(), properties.getNacosGroup());
        }
        catch (IOException ex) {
            throw new IllegalStateException("Nacos publish I/O failed", ex);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Nacos publish interrupted", ex);
        }
    }

    private String formBody(AgentCard agentCard) {
        try {
            return "dataId=" + encode(properties.getNacosDataId())
                    + "&group=" + encode(properties.getNacosGroup())
                    + "&content=" + encode(objectMapper.writeValueAsString(agentCard));
        }
        catch (JsonProcessingException ex) {
            throw new IllegalStateException("AgentCard JSON serialization failed", ex);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
