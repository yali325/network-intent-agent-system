package com.yali.mactav.orchestrator.remote.client;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardWrapper;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.a2a.A2aRequest;
import com.yali.mactav.model.a2a.A2aResponse;
import com.yali.mactav.model.agent.AgentCard;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orchestrator A2A client backed by Spring AI Alibaba's official A2aRemoteAgent.
 *
 * <p>The adapter keeps MAC-TAV's local A2aClient boundary while delegating
 * discovery, JSON-RPC transport, and remote invocation to SAA. It never
 * constructs prompts, invokes ChatModel/ReactAgent directly, or writes
 * Workspace state.</p>
 */
public class OfficialA2aClient implements A2aClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OfficialA2aClient.class);

    private static final String OUTPUT_KEY = "output";

    private static final Duration DEFAULT_CALL_TIMEOUT = Duration.ofSeconds(60);

    private static final AtomicInteger THREAD_SEQUENCE = new AtomicInteger();

    private static final Pattern STRING_FIELD_PATTERN =
            Pattern.compile("\"(errorCode|errorMessage|code|message|reasonCode|reason)\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");

    private static final Pattern NUMERIC_CODE_PATTERN =
            Pattern.compile("\"(code)\"\\s*:\\s*(-?\\d+)");

    private final AgentCardProvider agentCardProvider;

    private final ObjectMapper objectMapper;

    private final Duration callTimeout;

    public OfficialA2aClient(AgentCardProvider agentCardProvider, ObjectMapper objectMapper) {
        this(agentCardProvider, objectMapper, DEFAULT_CALL_TIMEOUT);
    }

    public OfficialA2aClient(AgentCardProvider agentCardProvider, ObjectMapper objectMapper, Duration callTimeout) {
        this.agentCardProvider = agentCardProvider;
        this.objectMapper = objectMapper;
        this.callTimeout = callTimeout == null || callTimeout.isNegative() || callTimeout.isZero()
                ? DEFAULT_CALL_TIMEOUT
                : callTimeout;
    }

    @Override
    public A2aResponse call(A2aRequest request, AgentCard agentCard) {
        try {
            String payloadJson = invokeRemoteAgent(request, agentCard);
            return A2aResponse.builder()
                    .success(true)
                    .taskId(request.getTaskId())
                    .sourceAgent(request.getTargetAgent())
                    .targetAgent(request.getSourceAgent())
                    .stage(request.getStage())
                    .payloadJson(payloadJson)
                    .message("Spring AI Alibaba A2A call succeeded")
                    .traceId(request.getTraceId())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
        catch (BusinessException ex) {
            throw ex;
        }
        catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.A2A_CALL_FAILED, "Spring AI Alibaba A2A call failed", ex);
        }
    }

    private String invokeRemoteAgent(A2aRequest request, AgentCard agentCard) {
        if (request == null || request.getTargetAgent() == null || request.getTargetAgent().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "A2A targetAgent must not be blank");
        }
        io.a2a.spec.AgentCard officialCard = resolveOfficialCard(request.getTargetAgent());
        boolean streaming = officialCard.capabilities() != null && officialCard.capabilities().streaming();
        String input = serialize(request);
        String safeUrl = summarizeUrl(officialCard.url());
        LOGGER.info(
                "Invoking SAA A2A agent targetAgent={}, taskId={}, traceId={}, payloadLength={}, requestJsonLength={}, timeoutMs={}, agentCardStreaming={}, url={}",
                request.getTargetAgent(),
                request.getTaskId(),
                request.getTraceId(),
                request.getPayloadJson() == null ? 0 : request.getPayloadJson().length(),
                input.length(),
                callTimeout.toMillis(),
                streaming,
                safeUrl);
        A2aRemoteAgent remoteAgent = A2aRemoteAgent.builder()
                .name(request.getTargetAgent())
                .description(agentCard == null ? request.getTargetAgent() : agentCard.getDescription())
                .agentCard(officialCard)
                .instruction("{input}")
                .outputKey(OUTPUT_KEY)
                .build();
        try {
            Optional<OverAllState> result = invokeWithTimeout(
                    remoteAgent,
                    input,
                    request,
                    streaming,
                    safeUrl);
            return result
                    .flatMap(state -> state.value(OUTPUT_KEY))
                    .map(Object::toString)
                    .filter(value -> !value.isBlank())
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.A2A_RESPONSE_INVALID,
                            "Spring AI Alibaba A2A response did not contain output"));
        }
        catch (BusinessException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new BusinessException(ErrorCode.A2A_CALL_FAILED, "Spring AI Alibaba A2A invocation failed", ex);
        }
    }

    private io.a2a.spec.AgentCard resolveOfficialCard(String targetAgent) {
        try {
            AgentCardWrapper wrapper = agentCardProvider.supportGetAgentCardByName()
                    ? agentCardProvider.getAgentCard(targetAgent)
                    : agentCardProvider.getAgentCard();
            if (wrapper == null || wrapper.getAgentCard() == null) {
                throw new BusinessException(ErrorCode.AGENT_CARD_NOT_FOUND, "Agent card not found: " + targetAgent);
            }
            io.a2a.spec.AgentCard officialCard = wrapper.getAgentCard();
            if (officialCard.name() != null && !officialCard.name().equals(targetAgent)) {
                throw new BusinessException(ErrorCode.AGENT_CARD_NOT_FOUND, "Agent card not found: " + targetAgent);
            }
            return officialCard;
        }
        catch (BusinessException ex) {
            throw ex;
        }
        catch (RuntimeException ex) {
            throw new BusinessException(
                    ErrorCode.AGENT_DISCOVERY_FAILED,
                    "Spring AI Alibaba AgentCard discovery failed for agent: " + targetAgent,
                    ex);
        }
    }

    private Optional<OverAllState> invokeWithTimeout(A2aRemoteAgent remoteAgent,
                                                     String input,
                                                     A2aRequest request,
                                                     boolean agentCardStreaming,
                                                     String agentUrl)
            throws Exception {
        ExecutorService executorService = Executors.newSingleThreadExecutor(daemonThreadFactory(request));
        Future<Optional<OverAllState>> future = executorService.submit(
                () -> SafeA2aStdoutGuard.call(() -> remoteAgent.invoke(buildInvokeState(input))));
        try {
            return future.get(callTimeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException ex) {
            future.cancel(true);
            LOGGER.warn(
                    "SAA A2A invocation timeout targetAgent={}, taskId={}, traceId={}, timeoutMs={}, agentCardStreaming={}, url={}",
                    request.getTargetAgent(),
                    request.getTaskId(),
                    request.getTraceId(),
                    callTimeout.toMillis(),
                    agentCardStreaming,
                    agentUrl);
            throw new BusinessException(
                    ErrorCode.REMOTE_AGENT_TIMEOUT,
                    "Spring AI Alibaba A2A invocation timed out for targetAgent=" + request.getTargetAgent()
                            + ", taskId=" + request.getTaskId()
                            + ", traceId=" + request.getTraceId()
                            + ", timeoutMs=" + callTimeout.toMillis()
                            + ", agentCardStreaming=" + agentCardStreaming
                            + ", url=" + agentUrl,
                    ex);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.warn(
                    "SAA A2A invocation interrupted targetAgent={}, taskId={}, traceId={}, timeoutMs={}, agentCardStreaming={}, url={}",
                    request.getTargetAgent(),
                    request.getTaskId(),
                    request.getTraceId(),
                    callTimeout.toMillis(),
                    agentCardStreaming,
                    agentUrl);
            throw new BusinessException(
                    ErrorCode.REMOTE_AGENT_TIMEOUT,
                    "Spring AI Alibaba A2A invocation was interrupted for targetAgent=" + request.getTargetAgent()
                            + ", taskId=" + request.getTaskId()
                            + ", traceId=" + request.getTraceId()
                            + ", timeoutMs=" + callTimeout.toMillis()
                            + ", agentCardStreaming=" + agentCardStreaming
                            + ", url=" + agentUrl,
                    ex);
        }
        catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            LOGGER.warn(
                    "SAA A2A invocation failed targetAgent={}, taskId={}, traceId={}, timeoutMs={}, agentCardStreaming={}, url={}, errorClass={}, message={}",
                    request.getTargetAgent(),
                    request.getTaskId(),
                    request.getTraceId(),
                    callTimeout.toMillis(),
                    agentCardStreaming,
                    agentUrl,
                    cause == null ? ex.getClass().getSimpleName() : cause.getClass().getSimpleName(),
                    summarize(cause == null ? ex.getMessage() : cause.getMessage()));
            if (cause instanceof BusinessException businessException) {
                throw businessException;
            }
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw new BusinessException(
                    ErrorCode.A2A_CALL_FAILED,
                    "Spring AI Alibaba A2A invocation failed for targetAgent=" + request.getTargetAgent()
                            + ", taskId=" + request.getTaskId()
                            + ", traceId=" + request.getTraceId()
                            + ", agentCardStreaming=" + agentCardStreaming
                            + ", url=" + agentUrl,
                    ex);
        }
        finally {
            executorService.shutdownNow();
        }
    }

    Map<String, Object> buildInvokeState(String input) {
        if (input == null || input.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Serialized A2A request input must not be blank");
        }
        return Map.of("input", input);
    }

    private ThreadFactory daemonThreadFactory(A2aRequest request) {
        return runnable -> {
            Thread thread = new Thread(
                    runnable,
                    "mactav-a2a-call-" + request.getTargetAgent() + "-" + THREAD_SEQUENCE.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    private String serialize(A2aRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        }
        catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.A2A_CALL_FAILED, "A2A request JSON serialization failed", ex);
        }
    }

    private String summarize(String message) {
        if (message == null) {
            return "";
        }
        String normalized = message.replaceAll("\\s+", " ").trim();
        String safeRemoteError = extractSafeRemoteError(normalized);
        if (!safeRemoteError.isBlank()) {
            return safeRemoteError;
        }
        if (isSensitiveA2aContent(normalized) || containsUnsafeKey(normalized)) {
            return "[SAA_A2A_CONTENT_REDACTED length=" + normalized.length() + "]";
        }
        return normalized.length() <= 240 ? normalized : normalized.substring(0, 240) + "...";
    }

    String summarizeA2aFailureForTest(String message) {
        return summarize(message);
    }

    private String extractSafeRemoteError(String message) {
        Map<String, String> fields = new LinkedHashMap<>();
        collectSafeFields(message, fields);
        String decoded = decodeEscapedJson(message);
        if (!decoded.equals(message)) {
            collectSafeFields(decoded, fields);
        }
        if (fields.isEmpty()) {
            return extractKnownProjectError(decoded);
        }
        String code = firstNonBlank(
                fields.get("errorCode"),
                firstNonBlank(fields.get("reasonCode"), fields.get("code")));
        String remoteMessage = firstNonBlank(
                fields.get("errorMessage"),
                firstNonBlank(fields.get("message"), fields.get("reason")));
        if (code == null && remoteMessage == null) {
            return "";
        }
        StringBuilder summary = new StringBuilder("remote");
        if (code != null) {
            summary.append(" errorCode=").append(bound(code, 120));
        }
        if (remoteMessage != null) {
            summary.append(" message=").append(bound(remoteMessage, 240));
        }
        return summary.toString();
    }

    private String extractKnownProjectError(String message) {
        for (ErrorCode errorCode : ErrorCode.values()) {
            String token = errorCode.getErrorCode();
            int index = message.indexOf(token);
            if (token.isBlank() || index < 0) {
                continue;
            }
            String messagePart = safeMessageNear(message, index + token.length(), errorCode.getMessage());
            return "remote errorCode=" + token + " message=" + messagePart;
        }
        return "";
    }

    private String safeMessageNear(String message, int startIndex, String fallback) {
        int end = Math.min(message.length(), startIndex + 300);
        int sensitiveIndex = firstSensitiveIndex(message, startIndex, end);
        if (sensitiveIndex >= 0) {
            end = sensitiveIndex;
        }
        int metadataIndex = firstMetadataIndex(message, startIndex, end);
        if (metadataIndex >= 0) {
            end = metadataIndex;
        }
        String candidate = message.substring(startIndex, end)
                .replaceAll("^[\\s:：,;\\-\\]})\"']+", "")
                .replaceAll("[\"'}\\],]+$", "")
                .replaceAll("\\s+", " ")
                .trim();
        if (candidate.isBlank() || containsUnsafeKey(candidate) || isSensitiveA2aContent(candidate)) {
            return fallback;
        }
        return bound(candidate, 240);
    }

    private int firstSensitiveIndex(String message, int startIndex, int endIndex) {
        int first = -1;
        for (String marker : new String[]{
                "\"payloadJson\"",
                "\\\"payloadJson\\\"",
                "\"workspaceSnapshot\"",
                "\\\"workspaceSnapshot\\\"",
                "\"rawText\"",
                "\\\"rawText\\\"",
                "\"prompt\"",
                "\\\"prompt\\\""}) {
            int index = message.indexOf(marker, startIndex);
            if (index >= 0 && index < endIndex && (first < 0 || index < first)) {
                first = index;
            }
        }
        return first;
    }

    private int firstMetadataIndex(String message, int startIndex, int endIndex) {
        int first = -1;
        for (String marker : new String[]{
                "\"messageId\"",
                "\\\"messageId\\\"",
                "\"contextId\"",
                "\\\"contextId\\\"",
                "\"taskId\"",
                "\\\"taskId\\\"",
                "\"timestamp\"",
                "\\\"timestamp\\\""}) {
            int index = message.indexOf(marker, startIndex);
            if (index >= 0 && index < endIndex && (first < 0 || index < first)) {
                first = index;
            }
        }
        return first;
    }

    private void collectSafeFields(String text, Map<String, String> fields) {
        collectFieldsFromJson(text, fields);
        Matcher stringMatcher = STRING_FIELD_PATTERN.matcher(text);
        while (stringMatcher.find()) {
            putSafeField(fields, stringMatcher.group(1), stringMatcher.group(2));
        }
        Matcher numericMatcher = NUMERIC_CODE_PATTERN.matcher(text);
        while (numericMatcher.find()) {
            putSafeField(fields, numericMatcher.group(1), numericMatcher.group(2));
        }
    }

    private void collectFieldsFromJson(String text, Map<String, String> fields) {
        String candidate = text.trim();
        if (!(candidate.startsWith("{") || candidate.startsWith("["))) {
            return;
        }
        try {
            collectFieldsFromNode(objectMapper.readTree(candidate), fields);
        }
        catch (JsonProcessingException ignored) {
            // Fall back to regex extraction for exception messages containing JSON fragments.
        }
    }

    private void collectFieldsFromNode(JsonNode node, Map<String, String> fields) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                JsonNode value = entry.getValue();
                if (isSafeErrorField(fieldName) && value.isValueNode()) {
                    putSafeField(fields, fieldName, value.asText());
                }
                collectFieldsFromNode(value, fields);
            });
        }
        else if (node.isArray()) {
            node.forEach(child -> collectFieldsFromNode(child, fields));
        }
    }

    private void putSafeField(Map<String, String> fields, String key, String value) {
        if (!isSafeErrorField(key) || value == null || value.isBlank()) {
            return;
        }
        String unescaped = decodeEscapedJson(value).replaceAll("\\s+", " ").trim();
        if (containsUnsafeKey(unescaped) || isSensitiveA2aContent(unescaped)) {
            return;
        }
        fields.putIfAbsent(key, unescaped);
    }

    private boolean isSafeErrorField(String key) {
        return "errorCode".equals(key)
                || "errorMessage".equals(key)
                || "code".equals(key)
                || "message".equals(key)
                || "reasonCode".equals(key)
                || "reason".equals(key);
    }

    private String decodeEscapedJson(String value) {
        return value.replace("\\\"", "\"")
                .replace("\\\\n", " ")
                .replace("\\n", " ")
                .replace("\\\\r", " ")
                .replace("\\r", " ")
                .replace("\\\\t", " ")
                .replace("\\t", " ");
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private String bound(String value, int maxLength) {
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "...";
    }

    private boolean isSensitiveA2aContent(String message) {
        return message.contains("\"method\":\"message/send\"")
                || message.contains("\"method\":\"message/stream\"")
                || message.contains("\"payloadJson\"")
                || message.contains("\\\"payloadJson\\\"")
                || message.contains("\"workspaceSnapshot\"")
                || message.contains("\\\"workspaceSnapshot\\\"")
                || message.contains("\"rawText\"")
                || message.contains("\\\"rawText\\\"");
    }

    private boolean containsUnsafeKey(String message) {
        String lower = message.toLowerCase();
        return lower.contains("\"prompt\"")
                || lower.contains("\\\"prompt\\\"")
                || lower.contains("api_key")
                || lower.contains("apikey")
                || lower.contains("authorization:");
    }

    private String summarizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            URI uri = new URI(url);
            int port = uri.getPort();
            String portPart = port < 0 ? "" : ":" + port;
            return uri.getScheme() + "://" + uri.getHost() + portPart + uri.getPath();
        }
        catch (URISyntaxException ex) {
            return "[A2A_URL_INVALID]";
        }
    }
}
