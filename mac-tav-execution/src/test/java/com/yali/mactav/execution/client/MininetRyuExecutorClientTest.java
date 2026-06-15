package com.yali.mactav.execution.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.execution.config.ExecutionProperties;
import com.yali.mactav.model.execution.ExecutionAction;
import com.yali.mactav.model.execution.ExecutionActionType;
import com.yali.mactav.model.execution.ExecutionEnvironmentType;
import com.yali.mactav.model.execution.ExecutionMode;
import com.yali.mactav.model.execution.ExecutionPlan;
import com.yali.mactav.model.execution.TestCommand;
import com.yali.mactav.model.execution.TestResultType;
import com.yali.mactav.model.plan.Topology;
import com.yali.mactav.model.plan.TopologyLink;
import com.yali.mactav.model.plan.TopologyNode;
import com.yali.mactav.model.workspace.TraceRefs;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Tests the Java client without connecting to a real Python executor.
 */
class MininetRyuExecutorClientTest {

    @Test
    void sendsStructuredRunRequestToRunEndpoint() throws IOException {
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        try (MockExecutorServer server = MockExecutorServer.ok(path, body)) {
            MininetRyuExecutorClient client = client(server.baseUrl());

            var response = client.run("execution-1", mininetPlan(), 7);

            assertEquals("/api/v1/executions/run", path.get());
            assertEquals("execution-1", response.executionId());
            assertTrue(body.get().contains("\"topology\""));
            assertTrue(body.get().contains("\"actions\""));
            assertTrue(body.get().contains("\"testCommands\""));
            assertTrue(body.get().contains("\"timeoutSeconds\""));
            assertFalse(containsForbiddenShellField(body.get()));
        }
    }

    @Test
    void sendsStructuredCleanupRequestWithoutShellSemantics() throws IOException {
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        try (MockExecutorServer server = MockExecutorServer.ok(path, body)) {
            MininetRyuExecutorClient client = client(server.baseUrl());

            client.cleanup("execution-1", "task-1", mininetPlan().getCleanupActions(), traceRefs());

            assertEquals("/api/v1/executions/cleanup", path.get());
            assertTrue(body.get().contains("\"executionId\""));
            assertTrue(body.get().contains("\"cleanupActions\""));
            assertFalse(containsForbiddenShellField(body.get()));
        }
    }

    @Test
    void handlesHealthAndStatus2xxResponses() throws IOException {
        try (MockExecutorServer server = MockExecutorServer.fixed(
                "/health",
                200,
                """
                        {"status":"ok","pythonVersion":"3.10","configuredPort":18091,"ryuExpectedApps":["simple_switch_13","ofctl_rest"],"mininetInstalled":"true"}
                        """)) {
            MininetRyuExecutorClient client = client(server.baseUrl());

            var response = client.health();

            assertEquals("ok", response.status());
            assertEquals(18091, response.configuredPort());
        }
    }

    @Test
    void mapsHttp500ToBusinessException() throws IOException {
        try (MockExecutorServer server = MockExecutorServer.fixed("/api/v1/ryu/status", 500, "{\"error\":\"boom\"}")) {
            MininetRyuExecutorClient client = client(server.baseUrl());

            BusinessException exception = assertThrows(BusinessException.class, client::ryuStatus);

            assertEquals("EXECUTION_ADAPTER_FAILED", exception.getErrorCode());
        }
    }

    @Test
    void mapsInvalidJsonToBusinessException() throws IOException {
        try (MockExecutorServer server = MockExecutorServer.fixed("/api/v1/mininet/status", 200, "{not-json")) {
            MininetRyuExecutorClient client = client(server.baseUrl());

            BusinessException exception = assertThrows(BusinessException.class, client::mininetStatus);

            assertEquals("EXECUTION_ADAPTER_FAILED", exception.getErrorCode());
        }
    }

    @Test
    void mapsConnectionFailureToBusinessException() {
        ExecutionProperties properties = new ExecutionProperties();
        properties.getMininetRyu().setBaseUrl("http://127.0.0.1:9");
        properties.getMininetRyu().setConnectTimeoutMs(50);
        properties.getMininetRyu().setReadTimeoutMs(50);
        MininetRyuExecutorClient client = new MininetRyuExecutorClient(properties);

        BusinessException exception = assertThrows(BusinessException.class, client::health);

        assertEquals("EXECUTOR_UNAVAILABLE", exception.getErrorCode());
    }

    private MininetRyuExecutorClient client(String baseUrl) {
        ExecutionProperties properties = new ExecutionProperties();
        properties.getMininetRyu().setBaseUrl(baseUrl);
        properties.getMininetRyu().setConnectTimeoutMs(500);
        properties.getMininetRyu().setReadTimeoutMs(1000);
        return new MininetRyuExecutorClient(properties);
    }

    private boolean containsForbiddenShellField(String json) {
        return json.contains("\"rawCommand\"")
                || json.contains("\"shell\"")
                || json.contains("\"script\"")
                || json.contains("\"cmd\"")
                || json.contains("\"command\"");
    }

    private ExecutionPlan mininetPlan() {
        return ExecutionPlan.builder()
                .executionPlanId("execution-plan-1")
                .taskId("task-1")
                .planId("plan-1")
                .configSetId("config-set-1")
                .targetEnvironment(ExecutionEnvironmentType.MININET_RYU)
                .executionMode(ExecutionMode.MININET_RYU)
                .topology(Topology.builder()
                        .nodes(List.of(
                                TopologyNode.builder().id("h1").nodeType("host").ipAddress("10.0.0.10/24").traceRefs(traceRefs()).build(),
                                TopologyNode.builder().id("s1").nodeType("switch").traceRefs(traceRefs()).build(),
                                TopologyNode.builder().id("h2").nodeType("host").ipAddress("10.0.0.11/24").traceRefs(traceRefs()).build()))
                        .links(List.of(
                                TopologyLink.builder().id("l1").sourceNode("h1").targetNode("s1").traceRefs(traceRefs()).build(),
                                TopologyLink.builder().id("l2").sourceNode("s1").targetNode("h2").traceRefs(traceRefs()).build()))
                        .build())
                .actions(List.of(ExecutionAction.builder()
                        .actionId("action-ryu-check")
                        .actionType(ExecutionActionType.RYU_CONTROLLER_CHECK)
                        .traceRefs(traceRefs())
                        .build()))
                .cleanupActions(List.of(ExecutionAction.builder()
                        .actionId("cleanup-1")
                        .actionType(ExecutionActionType.MININET_CLEANUP)
                        .traceRefs(traceRefs())
                        .build()))
                .testCommands(List.of(TestCommand.builder()
                        .testId("test-ping")
                        .testType(TestResultType.PING)
                        .sourceNode("h1")
                        .targetNode("h2")
                        .parameters(Map.of("count", 1))
                        .traceRefs(traceRefs())
                        .build()))
                .traceRefs(traceRefs())
                .build();
    }

    private TraceRefs traceRefs() {
        return TraceRefs.builder().planElementIds(List.of("plan-node-1")).testIds(List.of("test-ping")).build();
    }

    private static final class MockExecutorServer implements AutoCloseable {
        private final HttpServer server;

        private MockExecutorServer(HttpServer server) {
            this.server = server;
            this.server.start();
        }

        static MockExecutorServer ok(AtomicReference<String> path, AtomicReference<String> body) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                path.set(exchange.getRequestURI().getPath());
                body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                respond(exchange, 200, successResponse());
            });
            return new MockExecutorServer(server);
        }

        static MockExecutorServer fixed(String path, int status, String responseBody) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext(path, exchange -> respond(exchange, status, responseBody));
            return new MockExecutorServer(server);
        }

        String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        @Override
        public void close() {
            server.stop(0);
        }

        private static void respond(HttpExchange exchange, int status, String responseBody) throws IOException {
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }

        private static String successResponse() {
            return """
                    {
                      "status":"SUCCESS",
                      "executionId":"execution-1",
                      "runtimeState":{
                        "executorId":"python-mininet-ryu-executor",
                        "executorEndpoint":"127.0.0.1:18091",
                        "ryuControllerStatus":"available",
                        "mininetStatus":"cleaned",
                        "environmentStatus":"EXECUTION_COMPLETED",
                        "logsSummary":"ok",
                        "startedAt":"2026-06-01T10:00:00Z",
                        "endedAt":"2026-06-01T10:00:01Z"
                      },
                      "testResults":[],
                      "flowStats":{"status":"collected"},
                      "errors":[],
                      "logsSummary":"Execution completed.",
                      "startedAt":"2026-06-01T10:00:00Z",
                      "endedAt":"2026-06-01T10:00:01Z"
                    }
                    """;
        }
    }
}
