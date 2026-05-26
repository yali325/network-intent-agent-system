package com.yali.mactav.orchestrator.remote.client;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.a2a.A2aRequest;
import com.yali.mactav.model.a2a.A2aResponse;
import com.yali.mactav.model.agent.AgentCard;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Orchestrator A2A client backed by Spring AI Alibaba's official A2aRemoteAgent.
 *
 * <p>The adapter keeps MAC-TAV's local A2aClient boundary while delegating
 * discovery, JSON-RPC transport, and remote invocation to SAA. It never
 * constructs prompts, invokes ChatModel/ReactAgent directly, or writes
 * Workspace state.</p>
 */
public class OfficialA2aClient implements A2aClient {

    private static final String OUTPUT_KEY = "output";

    private final AgentCardProvider agentCardProvider;

    private final ObjectMapper objectMapper;

    public OfficialA2aClient(AgentCardProvider agentCardProvider, ObjectMapper objectMapper) {
        this.agentCardProvider = agentCardProvider;
        this.objectMapper = objectMapper;
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
        A2aRemoteAgent remoteAgent = A2aRemoteAgent.builder()
                .name(request.getTargetAgent())
                .description(agentCard == null ? request.getTargetAgent() : agentCard.getDescription())
                .agentCardProvider(agentCardProvider)
                .outputKey(OUTPUT_KEY)
                .build();
        try {
            Optional<OverAllState> result = remoteAgent.invoke(serialize(request));
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

    private String serialize(A2aRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        }
        catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.A2A_CALL_FAILED, "A2A request JSON serialization failed", ex);
        }
    }
}
