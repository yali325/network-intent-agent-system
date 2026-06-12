package com.yali.mactav.intent.a2a;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.intent.agent.IntentAgent;
import com.yali.mactav.intent.request.IntentAgentRequest;
import com.yali.mactav.model.a2a.A2aRequest;
import com.yali.mactav.model.intent.IntentAgentInvokePayload;
import com.yali.mactav.model.intent.NetworkIntent;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A2A server executor that adapts official A2A messages to IntentAgent.run().
 *
 * <p>The executor keeps Spring AI Alibaba's official A2A routes, Agent Card,
 * and Nacos registration in place. It only changes the server execution
 * adapter so remote callers receive a validated NetworkIntent JSON payload
 * instead of the raw IntentResponseSchema emitted by ReactAgent.</p>
 */
public class IntentAgentA2aExecutor implements AgentExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntentAgentA2aExecutor.class);

    private static final String OUTPUT_METADATA_KEY = "output";

    private final Function<IntentAgentRequest, NetworkIntent> intentRunner;

    private final ObjectMapper objectMapper;

    public IntentAgentA2aExecutor(IntentAgent intentAgent, ObjectMapper objectMapper) {
        this(Objects.requireNonNull(intentAgent, "intentAgent must not be null")::run, objectMapper);
    }

    IntentAgentA2aExecutor(Function<IntentAgentRequest, NetworkIntent> intentRunner, ObjectMapper objectMapper) {
        this.intentRunner = Objects.requireNonNull(intentRunner, "intentRunner must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        TaskUpdater updater = new TaskUpdater(context, eventQueue);
        try {
            String userInput = extractUserInput(context);
            LOGGER.info(
                    "IntentAgentA2aExecutor entry contextId={}, requestTaskId={}, messageTextLength={}",
                    context.getContextId(),
                    context.getTaskId(),
                    userInput.length());
            A2aRequest request = parseA2aRequest(userInput);
            IntentAgentInvokePayload payload = parsePayload(request);
            LOGGER.info(
                    "Executing IntentAgent A2A request taskId={}, traceId={}, stage={}, payloadLength={}",
                    request.getTaskId(),
                    request.getTraceId(),
                    request.getStage(),
                    request.getPayloadJson() == null ? 0 : request.getPayloadJson().length());
            NetworkIntent intent = intentRunner.apply(toIntentRequest(request, payload));
            String payloadJson = objectMapper.writeValueAsString(intent);
            updater.addArtifact(
                    List.of(new TextPart(payloadJson)),
                    UUID.randomUUID().toString(),
                    "conversation_result",
                    Map.of(OUTPUT_METADATA_KEY, payloadJson));
            updater.complete();
        }
        catch (BusinessException ex) {
            fail(updater, ex.getErrorCode() + ": " + ex.getMessage());
            throw new InvalidParamsError("IntentAgent A2A execution failed: " + ex.getErrorCode() + ": " + ex.getMessage());
        }
        catch (JsonProcessingException ex) {
            fail(updater, "IntentAgent A2A JSON processing failed");
            throw new InvalidParamsError("IntentAgent A2A JSON processing failed: " + ex.getOriginalMessage());
        }
        catch (RuntimeException ex) {
            fail(updater, "IntentAgent A2A execution failed");
            throw new InvalidParamsError("IntentAgent A2A execution failed: " + ex.getMessage());
        }
    }

    @Override
    public void cancel(RequestContext context, EventQueue eventQueue) {
        new TaskUpdater(context, eventQueue).cancel();
    }

    private String extractUserInput(RequestContext context) {
        if (context == null) {
            throw new IllegalArgumentException("A2A RequestContext must not be null");
        }
        String userInput = context.getUserInput("\n");
        if (userInput == null || userInput.isBlank()) {
            userInput = extractTextParts(context);
        }
        if (userInput == null || userInput.isBlank()) {
            throw new IllegalArgumentException("A2A message text must contain serialized A2aRequest JSON");
        }
        return userInput.trim();
    }

    private String extractTextParts(RequestContext context) {
        if (context.getMessage() == null || context.getMessage().getParts() == null) {
            return "";
        }
        return context.getMessage().getParts().stream()
                .filter(TextPart.class::isInstance)
                .map(TextPart.class::cast)
                .map(TextPart::getText)
                .filter(text -> text != null && !text.isBlank())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private A2aRequest parseA2aRequest(String text) {
        try {
            return objectMapper.readValue(text, A2aRequest.class);
        }
        catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("A2A message text is not valid A2aRequest JSON", ex);
        }
    }

    private IntentAgentInvokePayload parsePayload(A2aRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("A2aRequest must not be null");
        }
        if (request.getPayloadJson() == null || request.getPayloadJson().isBlank()) {
            throw new IllegalArgumentException("A2aRequest.payloadJson must contain IntentAgentInvokePayload JSON");
        }
        try {
            return objectMapper.readValue(request.getPayloadJson(), IntentAgentInvokePayload.class);
        }
        catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("A2aRequest.payloadJson is not valid IntentAgentInvokePayload JSON", ex);
        }
    }

    private IntentAgentRequest toIntentRequest(A2aRequest request, IntentAgentInvokePayload payload) {
        return IntentAgentRequest.builder()
                .taskId(firstNonBlank(payload.getTaskId(), request.getTaskId()))
                .rawText(payload.getRawText())
                .intentVersion(payload.getIntentVersion() == null ? request.getArtifactVersion() : payload.getIntentVersion())
                .traceId(firstNonBlank(payload.getTraceId(), request.getTraceId()))
                .userContext(payload.getUserContext())
                .workspaceSnapshot(payload.getWorkspaceSnapshot())
                .targetEnvironmentHint(payload.getTargetEnvironmentHint())
                .createdBy(payload.getCreatedBy())
                .build();
    }

    private void fail(TaskUpdater updater, String message) {
        updater.fail(updater.newAgentMessage(List.<Part<?>>of(new TextPart(message)), Map.of()));
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
