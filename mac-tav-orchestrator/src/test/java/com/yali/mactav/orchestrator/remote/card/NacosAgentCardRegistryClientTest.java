package com.yali.mactav.orchestrator.remote.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpServer;
import com.yali.mactav.model.agent.AgentCard;
import com.yali.mactav.model.agent.AgentHealthStatus;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Offline tests for Nacos AgentCard JSON lookup.
 *
 * <p>The test uses a local fake HTTP server instead of starting Nacos.</p>
 */
class NacosAgentCardRegistryClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void findByAgentNameShouldParseAgentCardFromNacosConfigResponse() throws Exception {
        ObjectMapper objectMapper = objectMapper();
        AgentCard card = AgentCard.builder()
                .agentName("IntentAgent")
                .serviceEndpoint("http://127.0.0.1:18081/internal/a2a/intent/invoke")
                .protocol("HTTP_JSON_A2A")
                .version("test")
                .healthStatus(AgentHealthStatus.UP)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/nacos/v1/cs/configs", exchange -> {
            byte[] body = objectMapper.writeValueAsBytes(card);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        NacosAgentCardRegistryClient client = new NacosAgentCardRegistryClient(
                "127.0.0.1:" + server.getAddress().getPort(),
                "MAC_TAV_AGENT_CARDS",
                objectMapper,
                HttpClient.newHttpClient(),
                Duration.ofSeconds(2));

        AgentCard found = client.findByAgentName("IntentAgent").orElseThrow();

        assertEquals("IntentAgent", found.getAgentName());
        assertEquals("HTTP_JSON_A2A", found.getProtocol());
    }

    @Test
    void findByAgentNameShouldReturnEmptyWhenNacosConfigMissing() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/nacos/v1/cs/configs", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        NacosAgentCardRegistryClient client = new NacosAgentCardRegistryClient(
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "MAC_TAV_AGENT_CARDS",
                objectMapper(),
                HttpClient.newHttpClient(),
                Duration.ofSeconds(2));

        assertTrue(client.findByAgentName("IntentAgent").isEmpty());
    }

    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
