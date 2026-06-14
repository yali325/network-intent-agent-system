package com.yali.mactav.planning.a2a;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.a2a.A2aRequest;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.plan.PlanningAgentInvokePayload;
import com.yali.mactav.planning.agent.PlanningAgent;
import com.yali.mactav.planning.request.PlanningAgentRequest;
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
 * A2A server executor that adapts official A2A messages to PlanningAgent.run().
 *
 * <p>The executor keeps Spring AI Alibaba's official A2A routes, Agent Card,
 * and Nacos registration in place. It only adapts the server execution
 * boundary so remote callers receive a validated NetworkPlan JSON payload
 * instead of the raw PlanningResponseSchema emitted by ReactAgent.</p>
 */
public class PlanningAgentA2aExecutor implements AgentExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlanningAgentA2aExecutor.class);

    private static final String OUTPUT_METADATA_KEY = "output";

    private final Function<PlanningAgentRequest, NetworkPlan> planningRunner;

    private final ObjectMapper objectMapper;

    public PlanningAgentA2aExecutor(PlanningAgent planningAgent, ObjectMapper objectMapper) {
        this(Objects.requireNonNull(planningAgent, "planningAgent must not be null")::run, objectMapper);
    }

    PlanningAgentA2aExecutor(Function<PlanningAgentRequest, NetworkPlan> planningRunner, ObjectMapper objectMapper) {
        this.planningRunner = Objects.requireNonNull(planningRunner, "planningRunner must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        TaskUpdater updater = new TaskUpdater(context, eventQueue);
        try {
            String userInput = extractUserInput(context);
            A2aRequest request = parseA2aRequest(userInput);
            PlanningAgentInvokePayload payload = parsePayload(request);
            LOGGER.info(
                    "PlanningAgentA2aExecutor entry taskId={}, traceId={}, contextId={}, requestTaskId={}, stage={}, messageTextLength={}, payloadLength={}",
                    request.getTaskId(),
                    request.getTraceId(),
                    context.getContextId(),
                    context.getTaskId(),
                    request.getStage(),
                    userInput.length(),
                    request.getPayloadJson() == null ? 0 : request.getPayloadJson().length());
            PlanningAgentRequest planningRequest = toPlanningRequest(request, payload);
            LOGGER.info(
                    "PlanningAgent.run start taskId={}, traceId={}, payloadLength={}, compactWorkspaceLength={}",
                    planningRequest.getTaskId(),
                    planningRequest.getTraceId(),
                    request.getPayloadJson() == null ? 0 : request.getPayloadJson().length(),
                    planningRequest.getWorkspaceSnapshot() == null ? 0 : planningRequest.getWorkspaceSnapshot().length());
            long runStart = System.nanoTime();
            NetworkPlan plan = planningRunner.apply(planningRequest);
            long runDurationMs = elapsedMillis(runStart);
            LOGGER.info(
                    "PlanningAgent.run completed taskId={}, traceId={}, durationMs={}, nodeCount={}, linkCount={}, zoneCount={}, policyCount={}",
                    planningRequest.getTaskId(),
                    planningRequest.getTraceId(),
                    runDurationMs,
                    topologyNodeCount(plan),
                    topologyLinkCount(plan),
                    plan == null || plan.getZones() == null ? 0 : plan.getZones().size(),
                    plan == null || plan.getSecurityPolicyPlan() == null ? 0 : plan.getSecurityPolicyPlan().size());
            String payloadJson = objectMapper.writeValueAsString(plan);
            LOGGER.info(
                    "PlanningAgentA2aExecutor addArtifact start taskId={}, traceId={}, outputJsonLength={}",
                    planningRequest.getTaskId(),
                    planningRequest.getTraceId(),
                    payloadJson.length());
            updater.addArtifact(
                    List.of(new TextPart(payloadJson)),
                    UUID.randomUUID().toString(),
                    "conversation_result",
                    Map.of(OUTPUT_METADATA_KEY, payloadJson));
            updater.complete();
            LOGGER.info(
                    "PlanningAgentA2aExecutor complete taskId={}, traceId={}",
                    planningRequest.getTaskId(),
                    planningRequest.getTraceId());
        }
        catch (BusinessException ex) {
            LOGGER.warn(
                    "PlanningAgentA2aExecutor fail errorCode={}, errorClass={}, message={}",
                    ex.getErrorCode(),
                    ex.getClass().getSimpleName(),
                    summarize(ex.getMessage()));
            fail(updater, ex.getErrorCode() + ": " + ex.getMessage());
            throw new InvalidParamsError("PlanningAgent A2A execution failed: " + ex.getErrorCode() + ": " + ex.getMessage());
        }
        catch (JsonProcessingException ex) {
            LOGGER.warn(
                    "PlanningAgentA2aExecutor fail errorClass={}, message={}",
                    ex.getClass().getSimpleName(),
                    summarize(ex.getOriginalMessage()));
            fail(updater, "PlanningAgent A2A JSON processing failed");
            throw new InvalidParamsError("PlanningAgent A2A JSON processing failed: " + ex.getOriginalMessage());
        }
        catch (RuntimeException ex) {
            LOGGER.warn(
                    "PlanningAgentA2aExecutor fail errorClass={}, message={}",
                    ex.getClass().getSimpleName(),
                    summarize(ex.getMessage()));
            fail(updater, "PlanningAgent A2A execution failed");
            throw new InvalidParamsError("PlanningAgent A2A execution failed: " + ex.getMessage());
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

    private PlanningAgentInvokePayload parsePayload(A2aRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("A2aRequest must not be null");
        }
        if (request.getPayloadJson() == null || request.getPayloadJson().isBlank()) {
            throw new IllegalArgumentException("A2aRequest.payloadJson must contain PlanningAgentInvokePayload JSON");
        }
        try {
            PlanningAgentInvokePayload payload = objectMapper.readValue(request.getPayloadJson(), PlanningAgentInvokePayload.class);
            validatePlanningPayload(payload);
            return payload;
        }
        catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("A2aRequest.payloadJson is not valid PlanningAgentInvokePayload JSON", ex);
        }
    }

    private void validatePlanningPayload(PlanningAgentInvokePayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("PlanningAgentInvokePayload must not be null");
        }
        if (isBlank(payload.getTaskId())) {
            throw new IllegalArgumentException("PlanningAgentInvokePayload.taskId must not be blank");
        }
        if (isBlank(payload.getIntentJson())) {
            throw new IllegalArgumentException("PlanningAgentInvokePayload.intentJson must contain current NetworkIntent JSON");
        }
    }

    private PlanningAgentRequest toPlanningRequest(A2aRequest request, PlanningAgentInvokePayload payload) {
        return PlanningAgentRequest.builder()
                .taskId(firstNonBlank(payload.getTaskId(), request.getTaskId()))
                .rawText(payload.getRawText())
                .intentVersion(payload.getIntentVersion())
                .intentJson(payload.getIntentJson())
                .planVersion(payload.getPlanVersion() == null ? request.getArtifactVersion() : payload.getPlanVersion())
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private int topologyNodeCount(NetworkPlan plan) {
        return plan == null || plan.getTopology() == null || plan.getTopology().getNodes() == null
                ? 0
                : plan.getTopology().getNodes().size();
    }

    private int topologyLinkCount(NetworkPlan plan) {
        return plan == null || plan.getTopology() == null || plan.getTopology().getLinks() == null
                ? 0
                : plan.getTopology().getLinks().size();
    }

    private String summarize(String message) {
        if (message == null) {
            return "";
        }
        String normalized = message.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 240 ? normalized : normalized.substring(0, 240) + "...";
    }
}
