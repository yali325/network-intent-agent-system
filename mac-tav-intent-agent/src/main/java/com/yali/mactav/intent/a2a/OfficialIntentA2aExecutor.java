package com.yali.mactav.intent.a2a;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.intent.agent.IntentAgent;
import com.yali.mactav.model.a2a.A2aRequest;
import com.yali.mactav.model.intent.IntentAgentInvokePayload;
import com.yali.mactav.model.intent.NetworkIntent;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.TextPart;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Official Spring AI Alibaba A2A server executor for the IntentAgent service.
 *
 * <p>This adapter lets the SAA A2A router and Nacos registry own transport and
 * Agent Card exposure while preserving MAC-TAV's ResponseSchema -> Parser -> DTO
 * -> Validator boundary inside IntentAgent. It does not write Workspace state or
 * expose frontend business APIs.</p>
 */
public class OfficialIntentA2aExecutor implements AgentExecutor {

    private static final String OUTPUT_KEY = "output";

    private final IntentAgent intentAgent;

    private final IntentAgentInvokePayloadMapper payloadMapper;

    private final ObjectMapper objectMapper;

    public OfficialIntentA2aExecutor(IntentAgent intentAgent,
                                     IntentAgentInvokePayloadMapper payloadMapper,
                                     ObjectMapper objectMapper) {
        this.intentAgent = intentAgent;
        this.payloadMapper = payloadMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void execute(RequestContext requestContext, EventQueue eventQueue) throws JSONRPCError {
        TaskUpdater updater = new TaskUpdater(requestContext, eventQueue);
        try {
            updater.submit();
            updater.startWork();
            String outputJson = invokeIntentAgent(requestContext.getUserInput("\n"));
            updater.addArtifact(
                    List.of(new TextPart(outputJson)),
                    "artifact-" + UUID.randomUUID(),
                    "NetworkIntent",
                    outputMetadata(outputJson));
            updater.complete(updater.newAgentMessage(
                    List.of(new TextPart(outputJson)),
                    outputMetadata(outputJson)));
        }
        catch (BusinessException ex) {
            updater.fail(updater.newAgentMessage(
                    List.of(new TextPart(safeFailureMessage(ex.getErrorCode(), ex.getMessage()))),
                    Map.of("errorCode", ex.getErrorCode())));
        }
        catch (RuntimeException ex) {
            updater.fail(updater.newAgentMessage(
                    List.of(new TextPart("IntentAgent A2A execution failed")),
                    Map.of("errorCode", ErrorCode.AGENT_EXECUTION_FAILED.getErrorCode())));
        }
    }

    @Override
    public void cancel(RequestContext requestContext, EventQueue eventQueue) throws JSONRPCError {
        new TaskUpdater(requestContext, eventQueue).cancel();
    }

    private String invokeIntentAgent(String requestText) {
        A2aRequest request = parseRequest(requestText);
        IntentAgentInvokePayload payload = parsePayload(request);
        NetworkIntent intent = intentAgent.run(payloadMapper.toRequest(payload));
        return serializeIntent(intent);
    }

    private A2aRequest parseRequest(String requestText) {
        if (requestText == null || requestText.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "A2A request text must not be blank");
        }
        try {
            return objectMapper.readValue(requestText, A2aRequest.class);
        }
        catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.A2A_RESPONSE_INVALID, "A2A request JSON is invalid", ex);
        }
    }

    private IntentAgentInvokePayload parsePayload(A2aRequest request) {
        if (request.getPayloadJson() == null || request.getPayloadJson().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "A2A request payloadJson must not be blank");
        }
        try {
            return objectMapper.readValue(request.getPayloadJson(), IntentAgentInvokePayload.class);
        }
        catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.A2A_RESPONSE_INVALID, "IntentAgent payload JSON is invalid", ex);
        }
    }

    private String serializeIntent(NetworkIntent intent) {
        try {
            return objectMapper.writeValueAsString(intent);
        }
        catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.A2A_RESPONSE_INVALID, "NetworkIntent JSON serialization failed", ex);
        }
    }

    private Map<String, Object> outputMetadata(String outputJson) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(OUTPUT_KEY, outputJson);
        metadata.put("payloadType", NetworkIntent.class.getName());
        return metadata;
    }

    private String safeFailureMessage(String errorCode, String message) {
        String safeMessage = message == null || message.isBlank() ? "IntentAgent A2A execution failed" : message;
        return errorCode + ": " + safeMessage;
    }
}
