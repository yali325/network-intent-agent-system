package com.yali.mactav.verification.a2a;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.a2a.A2aRequest;
import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.model.verification.VerificationAgentInvokePayload;
import com.yali.mactav.verification.agent.VerificationAgent;
import com.yali.mactav.verification.request.VerificationAgentRequest;
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
 * A2A server executor that adapts official A2A messages to VerificationAgent.run().
 *
 * <p>The executor keeps Spring AI Alibaba official A2A routes, Agent Card, and
 * Nacos registration in place. It only adapts the execution boundary so remote
 * callers receive a validated ValidationReport JSON payload.</p>
 */
public class VerificationAgentA2aExecutor implements AgentExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(VerificationAgentA2aExecutor.class);

    private static final String OUTPUT_METADATA_KEY = "output";

    private final Function<VerificationAgentRequest, ValidationReport> verificationRunner;

    private final ObjectMapper objectMapper;

    public VerificationAgentA2aExecutor(VerificationAgent verificationAgent, ObjectMapper objectMapper) {
        this(Objects.requireNonNull(verificationAgent, "verificationAgent must not be null")::run, objectMapper);
    }

    VerificationAgentA2aExecutor(
            Function<VerificationAgentRequest, ValidationReport> verificationRunner,
            ObjectMapper objectMapper) {
        this.verificationRunner = Objects.requireNonNull(verificationRunner, "verificationRunner must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        TaskUpdater updater = new TaskUpdater(context, eventQueue);
        A2aRequest request = null;
        try {
            String userInput = extractUserInput(context);
            request = parseA2aRequest(userInput);
            VerificationAgentInvokePayload payload = parsePayload(request);
            LOGGER.info(
                    "VerificationAgentA2aExecutor entry taskId={}, traceId={}, contextId={}, requestTaskId={}, stage={}, messageTextLength={}, payloadLength={}",
                    request.getTaskId(),
                    request.getTraceId(),
                    context.getContextId(),
                    context.getTaskId(),
                    request.getStage(),
                    userInput.length(),
                    request.getPayloadJson() == null ? 0 : request.getPayloadJson().length());
            VerificationAgentRequest verificationRequest = toVerificationRequest(request, payload);
            LOGGER.info(
                    "VerificationAgent.run start taskId={}, traceId={}, payloadLength={}, compactWorkspaceLength={}",
                    verificationRequest.getTaskId(),
                    verificationRequest.getTraceId(),
                    request.getPayloadJson() == null ? 0 : request.getPayloadJson().length(),
                    verificationRequest.getWorkspaceSnapshot() == null ? 0 : verificationRequest.getWorkspaceSnapshot().length());
            long runStart = System.nanoTime();
            ValidationReport report = verificationRunner.apply(verificationRequest);
            LOGGER.info(
                    "VerificationAgent.run completed taskId={}, traceId={}, durationMs={}, itemCount={}, evidenceCount={}",
                    verificationRequest.getTaskId(),
                    verificationRequest.getTraceId(),
                    elapsedMillis(runStart),
                    itemCount(report),
                    evidenceCount(report));
            String payloadJson = objectMapper.writeValueAsString(report);
            LOGGER.info(
                    "VerificationAgentA2aExecutor addArtifact start taskId={}, traceId={}, outputJsonLength={}",
                    verificationRequest.getTaskId(),
                    verificationRequest.getTraceId(),
                    payloadJson.length());
            updater.addArtifact(
                    List.of(new TextPart(payloadJson)),
                    UUID.randomUUID().toString(),
                    "conversation_result",
                    Map.of(OUTPUT_METADATA_KEY, payloadJson));
            updater.complete();
            LOGGER.info(
                    "VerificationAgentA2aExecutor complete taskId={}, traceId={}",
                    verificationRequest.getTaskId(),
                    verificationRequest.getTraceId());
        }
        catch (BusinessException ex) {
            LOGGER.warn(
                    "VerificationAgentA2aExecutor fail taskId={}, traceId={}, stage={}, errorCode={}, errorClass={}, message={}",
                    request == null ? null : request.getTaskId(),
                    request == null ? null : request.getTraceId(),
                    request == null ? null : request.getStage(),
                    ex.getErrorCode(),
                    ex.getClass().getSimpleName(),
                    summarize(ex.getMessage()));
            fail(updater, ex.getErrorCode() + ": " + ex.getMessage());
            throw new InvalidParamsError("VerificationAgent A2A execution failed: " + ex.getErrorCode() + ": " + ex.getMessage());
        }
        catch (JsonProcessingException ex) {
            LOGGER.warn(
                    "VerificationAgentA2aExecutor fail taskId={}, traceId={}, stage={}, errorClass={}, message={}",
                    request == null ? null : request.getTaskId(),
                    request == null ? null : request.getTraceId(),
                    request == null ? null : request.getStage(),
                    ex.getClass().getSimpleName(),
                    summarize(ex.getOriginalMessage()));
            fail(updater, "VerificationAgent A2A JSON processing failed");
            throw new InvalidParamsError("VerificationAgent A2A JSON processing failed: " + ex.getOriginalMessage());
        }
        catch (RuntimeException ex) {
            LOGGER.warn(
                    "VerificationAgentA2aExecutor fail taskId={}, traceId={}, stage={}, errorClass={}, message={}",
                    request == null ? null : request.getTaskId(),
                    request == null ? null : request.getTraceId(),
                    request == null ? null : request.getStage(),
                    ex.getClass().getSimpleName(),
                    summarize(ex.getMessage()));
            fail(updater, "VerificationAgent A2A execution failed");
            throw new InvalidParamsError("VerificationAgent A2A execution failed: " + ex.getMessage());
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

    private VerificationAgentInvokePayload parsePayload(A2aRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("A2aRequest must not be null");
        }
        if (request.getPayloadJson() == null || request.getPayloadJson().isBlank()) {
            throw new IllegalArgumentException("A2aRequest.payloadJson must contain VerificationAgentInvokePayload JSON");
        }
        try {
            VerificationAgentInvokePayload payload = objectMapper.readValue(
                    request.getPayloadJson(), VerificationAgentInvokePayload.class);
            validateVerificationPayload(payload);
            return payload;
        }
        catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("A2aRequest.payloadJson is not valid VerificationAgentInvokePayload JSON", ex);
        }
    }

    private void validateVerificationPayload(VerificationAgentInvokePayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("VerificationAgentInvokePayload must not be null");
        }
        if (isBlank(payload.getTaskId())) {
            throw new IllegalArgumentException("VerificationAgentInvokePayload.taskId must not be blank");
        }
        if (isBlank(payload.getIntentJson())) {
            throw new IllegalArgumentException("VerificationAgentInvokePayload.intentJson must contain current NetworkIntent JSON");
        }
        if (isBlank(payload.getPlanJson())) {
            throw new IllegalArgumentException("VerificationAgentInvokePayload.planJson must contain current NetworkPlan JSON");
        }
        if (isBlank(payload.getConfigSetJson())) {
            throw new IllegalArgumentException("VerificationAgentInvokePayload.configSetJson must contain current ConfigSet JSON");
        }
        if (isBlank(payload.getExecutionReportJson())) {
            throw new IllegalArgumentException("VerificationAgentInvokePayload.executionReportJson must contain current ExecutionReport JSON");
        }
    }

    private VerificationAgentRequest toVerificationRequest(A2aRequest request, VerificationAgentInvokePayload payload) {
        return VerificationAgentRequest.builder()
                .taskId(firstNonBlank(payload.getTaskId(), request.getTaskId()))
                .rawText(payload.getRawText())
                .intentVersion(payload.getIntentVersion())
                .planVersion(payload.getPlanVersion())
                .configVersion(payload.getConfigVersion())
                .executionVersion(payload.getExecutionVersion())
                .validationVersion(payload.getValidationVersion() == null
                        ? request.getArtifactVersion()
                        : payload.getValidationVersion())
                .intentJson(payload.getIntentJson())
                .planJson(payload.getPlanJson())
                .configSetJson(payload.getConfigSetJson())
                .executionReportJson(payload.getExecutionReportJson())
                .traceId(firstNonBlank(payload.getTraceId(), request.getTraceId()))
                .userContext(payload.getUserContext())
                .workspaceSnapshot(payload.getWorkspaceSnapshot())
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

    private int itemCount(ValidationReport report) {
        return report == null || report.getItems() == null ? 0 : report.getItems().size();
    }

    private int evidenceCount(ValidationReport report) {
        return report == null || report.getEvidences() == null ? 0 : report.getEvidences().size();
    }

    private String summarize(String message) {
        if (message == null) {
            return "";
        }
        String normalized = message.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 240 ? normalized : normalized.substring(0, 240) + "...";
    }
}
