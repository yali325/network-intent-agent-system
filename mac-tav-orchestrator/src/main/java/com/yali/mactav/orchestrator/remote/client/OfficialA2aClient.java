package com.yali.mactav.orchestrator.remote.client;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardWrapper;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.a2a.A2aRequest;
import com.yali.mactav.model.a2a.A2aResponse;
import com.yali.mactav.model.agent.AgentCard;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
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
        LOGGER.info(
                "Invoking SAA A2A agent targetAgent={}, taskId={}, traceId={}, agentCardStreaming={}, url={}",
                request.getTargetAgent(),
                request.getTaskId(),
                request.getTraceId(),
                streaming,
                officialCard.url());
        A2aRemoteAgent remoteAgent = A2aRemoteAgent.builder()
                .name(request.getTargetAgent())
                .description(agentCard == null ? request.getTargetAgent() : agentCard.getDescription())
                .agentCard(officialCard)
                .instruction("{input}")
                .outputKey(OUTPUT_KEY)
                .build();
        try {
            Optional<OverAllState> result = invokeWithTimeout(remoteAgent, serialize(request), request);
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

    private Optional<OverAllState> invokeWithTimeout(A2aRemoteAgent remoteAgent, String input, A2aRequest request)
            throws Exception {
        ExecutorService executorService = Executors.newSingleThreadExecutor(daemonThreadFactory(request));
        Future<Optional<OverAllState>> future = executorService.submit(() -> remoteAgent.invoke(input));
        try {
            return future.get(callTimeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException ex) {
            future.cancel(true);
            throw new BusinessException(
                    ErrorCode.REMOTE_AGENT_TIMEOUT,
                    "Spring AI Alibaba A2A invocation timed out after " + callTimeout.toSeconds()
                            + "s for agent: " + request.getTargetAgent(),
                    ex);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(
                    ErrorCode.REMOTE_AGENT_TIMEOUT,
                    "Spring AI Alibaba A2A invocation was interrupted for agent: " + request.getTargetAgent(),
                    ex);
        }
        catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof BusinessException businessException) {
                throw businessException;
            }
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw new BusinessException(
                    ErrorCode.A2A_CALL_FAILED,
                    "Spring AI Alibaba A2A invocation failed for agent: " + request.getTargetAgent(),
                    ex);
        }
        finally {
            executorService.shutdownNow();
        }
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
}
