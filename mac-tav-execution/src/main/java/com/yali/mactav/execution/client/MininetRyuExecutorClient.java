package com.yali.mactav.execution.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.execution.client.dto.MininetRyuCleanupRequest;
import com.yali.mactav.execution.client.dto.MininetRyuHealthResponse;
import com.yali.mactav.execution.client.dto.MininetRyuRunRequest;
import com.yali.mactav.execution.client.dto.MininetRyuRunResponse;
import com.yali.mactav.execution.client.dto.MininetRyuStatusResponse;
import com.yali.mactav.execution.config.ExecutionProperties;
import com.yali.mactav.model.execution.ExecutionPlan;
import com.yali.mactav.model.workspace.TraceRefs;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * JDK HttpClient wrapper for the Python Mininet/Ryu executor API.
 */
public class MininetRyuExecutorClient {

    private static final String JSON = "application/json";

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    private final String baseUrl;

    private final Duration readTimeout;

    public MininetRyuExecutorClient(ExecutionProperties properties) {
        this(properties, defaultObjectMapper());
    }

    public MininetRyuExecutorClient(ExecutionProperties properties, ObjectMapper objectMapper) {
        ExecutionProperties.MininetRyuProperties mininetRyu = properties == null
                ? new ExecutionProperties.MininetRyuProperties()
                : properties.getMininetRyu();
        this.baseUrl = normalizeBaseUrl(mininetRyu.getBaseUrl());
        this.readTimeout = Duration.ofMillis(Math.max(1, mininetRyu.getReadTimeoutMs()));
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(1, mininetRyu.getConnectTimeoutMs())))
                .build();
        this.objectMapper = objectMapper == null ? defaultObjectMapper() : objectMapper;
    }

    public MininetRyuHealthResponse health() {
        return get("/health", MininetRyuHealthResponse.class);
    }

    public MininetRyuStatusResponse ryuStatus() {
        return get("/api/v1/ryu/status", MininetRyuStatusResponse.class);
    }

    public MininetRyuStatusResponse mininetStatus() {
        return get("/api/v1/mininet/status", MininetRyuStatusResponse.class);
    }

    public MininetRyuRunResponse run(String executionId, ExecutionPlan executionPlan, Integer executionVersion) {
        return post(
                "/api/v1/executions/run",
                buildRunRequest(executionId, executionPlan, executionVersion),
                MininetRyuRunResponse.class);
    }

    public MininetRyuRunResponse cleanup(
            String executionId,
            String taskId,
            List<com.yali.mactav.model.execution.ExecutionAction> cleanupActions,
            TraceRefs traceRefs) {
        return post(
                "/api/v1/executions/cleanup",
                new MininetRyuCleanupRequest(executionId, taskId, safeList(cleanupActions), traceRefs),
                MininetRyuRunResponse.class);
    }

    public MininetRyuRunRequest buildRunRequest(
            String executionId,
            ExecutionPlan executionPlan,
            Integer executionVersion) {
        if (executionPlan == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "ExecutionPlan must not be null");
        }
        return new MininetRyuRunRequest(
                requireText(executionId, "executionId"),
                executionPlan.getTaskId(),
                executionPlan.getPlanId(),
                executionPlan.getConfigSetId(),
                executionVersion == null ? 1 : executionVersion,
                executionPlan.getTopology(),
                safeList(executionPlan.getActions()),
                safeList(executionPlan.getCleanupActions()),
                safeList(executionPlan.getTestCommands()),
                null,
                executionPlan.getTraceRefs());
    }

    public String runRequestJson(String executionId, ExecutionPlan executionPlan, Integer executionVersion) {
        return writeJson(buildRunRequest(executionId, executionPlan, executionVersion));
    }

    public String cleanupRequestJson(
            String executionId,
            String taskId,
            List<com.yali.mactav.model.execution.ExecutionAction> cleanupActions,
            TraceRefs traceRefs) {
        return writeJson(new MininetRyuCleanupRequest(executionId, taskId, safeList(cleanupActions), traceRefs));
    }

    private <T> T get(String path, Class<T> responseType) {
        HttpRequest request = HttpRequest.newBuilder(endpoint(path))
                .timeout(readTimeout)
                .GET()
                .header("Accept", JSON)
                .build();
        return send(request, responseType);
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        HttpRequest request = HttpRequest.newBuilder(endpoint(path))
                .timeout(readTimeout)
                .POST(HttpRequest.BodyPublishers.ofString(writeJson(body)))
                .header("Accept", JSON)
                .header("Content-Type", JSON)
                .build();
        return send(request, responseType);
    }

    private <T> T send(HttpRequest request, Class<T> responseType) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(
                        ErrorCode.EXECUTION_ADAPTER_FAILED,
                        "Mininet/Ryu executor HTTP call failed with status=" + response.statusCode());
            }
            try {
                return objectMapper.readValue(response.body(), responseType);
            } catch (JsonProcessingException exception) {
                throw new BusinessException(
                        ErrorCode.EXECUTION_ADAPTER_FAILED,
                        "Mininet/Ryu executor returned invalid JSON");
            }
        } catch (IOException exception) {
            throw new BusinessException(
                    ErrorCode.EXECUTION_ADAPTER_FAILED,
                    "Mininet/Ryu executor connection failed: " + exception.getClass().getSimpleName());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.EXECUTION_TIMEOUT, "Mininet/Ryu executor call was interrupted");
        }
    }

    private String writeJson(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.EXECUTION_ADAPTER_FAILED, "Failed to serialize executor request");
        }
    }

    private URI endpoint(String path) {
        return URI.create(baseUrl + path);
    }

    private static String normalizeBaseUrl(String value) {
        String resolved = value == null || value.isBlank() ? "http://localhost:18091" : value.trim();
        return resolved.endsWith("/") ? resolved.substring(0, resolved.length() - 1) : resolved;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, fieldName + " must not be blank");
        }
        return value;
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static ObjectMapper defaultObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .findAndRegisterModules();
    }

    @Override
    public String toString() {
        return "MininetRyuExecutorClient{baseUrl='" + Objects.toString(baseUrl, "") + "'}";
    }
}
