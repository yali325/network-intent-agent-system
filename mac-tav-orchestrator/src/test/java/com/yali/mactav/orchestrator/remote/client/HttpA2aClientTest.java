package com.yali.mactav.orchestrator.remote.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpServer;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.a2a.A2aRequest;
import com.yali.mactav.model.agent.AgentCard;
import com.yali.mactav.model.enums.WorkflowStage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Offline transport tests for HttpA2aClient.
 *
 * <p>The tests use a local JDK HTTP server fixture and do not call real Nacos,
 * IntentAgent, or model providers.</p>
 */
class HttpA2aClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void callShouldConvertNon2xxToBusinessException() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/a2a", exchange -> {
            byte[] body = "bad gateway".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(502, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        HttpA2aClient client = new HttpA2aClient(objectMapper(), HttpClient.newHttpClient(), Duration.ofSeconds(2));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> client.call(request(), card("/a2a")));

        assertEquals("A2A_CALL_FAILED", exception.getErrorCode());
    }

    @Test
    void callShouldConvertTimeoutToBusinessException() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/slow", exchange -> {
            try {
                Thread.sleep(300);
            }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        HttpA2aClient client = new HttpA2aClient(objectMapper(), HttpClient.newHttpClient(), Duration.ofMillis(50));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> client.call(request(), card("/slow")));

        assertEquals("REMOTE_AGENT_TIMEOUT", exception.getErrorCode());
    }

    private A2aRequest request() {
        return A2aRequest.builder()
                .taskId("task-http-a2a")
                .sourceAgent("Orchestrator")
                .targetAgent("IntentAgent")
                .stage(WorkflowStage.INTENT)
                .payloadJson("{}")
                .traceId("trace-http-a2a")
                .timestamp(LocalDateTime.now())
                .build();
    }

    private AgentCard card(String path) {
        return AgentCard.builder()
                .agentName("IntentAgent")
                .serviceEndpoint("http://127.0.0.1:" + server.getAddress().getPort() + path)
                .build();
    }

    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
