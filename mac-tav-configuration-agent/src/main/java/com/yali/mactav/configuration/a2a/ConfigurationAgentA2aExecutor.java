package com.yali.mactav.configuration.a2a;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.configuration.agent.ConfigurationAgent;
import com.yali.mactav.model.a2a.A2aRequest;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.config.ConfigurationAgentInvokePayload;
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
 * A2A server executor that adapts official A2A messages to ConfigurationAgent.run().
 *
 * <p>The executor keeps Spring AI Alibaba official A2A routes, Agent Card, and
 * Nacos registration in place. It only adapts the execution boundary so remote
 * callers receive a validated ConfigSet JSON payload.</p>
 */
public class ConfigurationAgentA2aExecutor implements AgentExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationAgentA2aExecutor.class);

    private static final String OUTPUT_METADATA_KEY = "output";

    private final Function<ConfigurationAgentInvokePayload, ConfigSet> configurationRunner;

    private final ObjectMapper objectMapper;

    public ConfigurationAgentA2aExecutor(ConfigurationAgent configurationAgent, ObjectMapper objectMapper) {
        this(Objects.requireNonNull(configurationAgent, "configurationAgent must not be null")::run, objectMapper);
    }

    ConfigurationAgentA2aExecutor(
            Function<ConfigurationAgentInvokePayload, ConfigSet> configurationRunner,
            ObjectMapper objectMapper) {
        this.configurationRunner = Objects.requireNonNull(configurationRunner, "configurationRunner must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        TaskUpdater updater = new TaskUpdater(context, eventQueue);
        A2aRequest request = null;
        try {
            String userInput = extractUserInput(context);
            request = parseA2aRequest(userInput);
            ConfigurationAgentInvokePayload payload = parsePayload(request);
            LOGGER.info(
                    "ConfigurationAgentA2aExecutor entry taskId={}, traceId={}, contextId={}, requestTaskId={}, stage={}, messageTextLength={}, payloadLength={}",
                    request.getTaskId(),
                    request.getTraceId(),
                    context.getContextId(),
                    context.getTaskId(),
                    request.getStage(),
                    userInput.length(),
                    request.getPayloadJson() == null ? 0 : request.getPayloadJson().length());
            LOGGER.info(
                    "ConfigurationAgent.run start taskId={}, traceId={}, payloadLength={}, compactWorkspaceLength={}",
                    payload.getTaskId(),
                    payload.getTraceId(),
                    request.getPayloadJson() == null ? 0 : request.getPayloadJson().length(),
                    payload.getWorkspaceSnapshot() == null ? 0 : payload.getWorkspaceSnapshot().length());
            long runStart = System.nanoTime();
            ConfigSet configSet = configurationRunner.apply(payload);
            LOGGER.info(
                    "ConfigurationAgent.run completed taskId={}, traceId={}, durationMs={}, deviceCount={}, commandBlockCount={}, endpointCount={}",
                    payload.getTaskId(),
                    payload.getTraceId(),
                    elapsedMillis(runStart),
                    configSet == null || configSet.getDeviceConfigs() == null ? 0 : configSet.getDeviceConfigs().size(),
                    commandBlockCount(configSet),
                    configSet == null || configSet.getEndpointConfigs() == null ? 0 : configSet.getEndpointConfigs().size());
            String payloadJson = objectMapper.writeValueAsString(configSet);
            LOGGER.info(
                    "ConfigurationAgentA2aExecutor addArtifact start taskId={}, traceId={}, outputJsonLength={}",
                    payload.getTaskId(),
                    payload.getTraceId(),
                    payloadJson.length());
            updater.addArtifact(
                    List.of(new TextPart(payloadJson)),
                    UUID.randomUUID().toString(),
                    "conversation_result",
                    Map.of(OUTPUT_METADATA_KEY, payloadJson));
            updater.complete();
            LOGGER.info(
                    "ConfigurationAgentA2aExecutor complete taskId={}, traceId={}",
                    payload.getTaskId(),
                    payload.getTraceId());
        }
        catch (BusinessException ex) {
            LOGGER.warn(
                    "ConfigurationAgentA2aExecutor fail taskId={}, traceId={}, stage={}, errorCode={}, errorClass={}, message={}",
                    request == null ? null : request.getTaskId(),
                    request == null ? null : request.getTraceId(),
                    request == null ? null : request.getStage(),
                    ex.getErrorCode(),
                    ex.getClass().getSimpleName(),
                    summarize(ex.getMessage()));
            fail(updater, ex.getErrorCode() + ": " + ex.getMessage());
            throw new InvalidParamsError("ConfigurationAgent A2A execution failed: " + ex.getErrorCode() + ": " + ex.getMessage());
        }
        catch (JsonProcessingException ex) {
            LOGGER.warn(
                    "ConfigurationAgentA2aExecutor fail taskId={}, traceId={}, stage={}, errorClass={}, message={}",
                    request == null ? null : request.getTaskId(),
                    request == null ? null : request.getTraceId(),
                    request == null ? null : request.getStage(),
                    ex.getClass().getSimpleName(),
                    summarize(ex.getOriginalMessage()));
            fail(updater, "ConfigurationAgent A2A JSON processing failed");
            throw new InvalidParamsError("ConfigurationAgent A2A JSON processing failed: " + ex.getOriginalMessage());
        }
        catch (RuntimeException ex) {
            LOGGER.warn(
                    "ConfigurationAgentA2aExecutor fail taskId={}, traceId={}, stage={}, errorClass={}, message={}",
                    request == null ? null : request.getTaskId(),
                    request == null ? null : request.getTraceId(),
                    request == null ? null : request.getStage(),
                    ex.getClass().getSimpleName(),
                    summarize(ex.getMessage()));
            fail(updater, "ConfigurationAgent A2A execution failed");
            throw new InvalidParamsError("ConfigurationAgent A2A execution failed: " + ex.getMessage());
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

    private ConfigurationAgentInvokePayload parsePayload(A2aRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("A2aRequest must not be null");
        }
        if (request.getPayloadJson() == null || request.getPayloadJson().isBlank()) {
            throw new IllegalArgumentException("A2aRequest.payloadJson must contain ConfigurationAgentInvokePayload JSON");
        }
        try {
            ConfigurationAgentInvokePayload payload = objectMapper.readValue(
                    request.getPayloadJson(), ConfigurationAgentInvokePayload.class);
            validateConfigurationPayload(payload);
            return payload;
        }
        catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("A2aRequest.payloadJson is not valid ConfigurationAgentInvokePayload JSON", ex);
        }
    }

    private void validateConfigurationPayload(ConfigurationAgentInvokePayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("ConfigurationAgentInvokePayload must not be null");
        }
        if (isBlank(payload.getTaskId())) {
            throw new IllegalArgumentException("ConfigurationAgentInvokePayload.taskId must not be blank");
        }
        if (isBlank(payload.getPlanJson())) {
            throw new IllegalArgumentException("ConfigurationAgentInvokePayload.planJson must contain current NetworkPlan JSON");
        }
    }

    private void fail(TaskUpdater updater, String message) {
        updater.fail(updater.newAgentMessage(List.<Part<?>>of(new TextPart(message)), Map.of()));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private int commandBlockCount(ConfigSet configSet) {
        if (configSet == null || configSet.getDeviceConfigs() == null) {
            return 0;
        }
        return configSet.getDeviceConfigs().stream()
                .filter(deviceConfig -> deviceConfig != null && deviceConfig.getCommandBlocks() != null)
                .mapToInt(deviceConfig -> deviceConfig.getCommandBlocks().size())
                .sum();
    }

    private String summarize(String message) {
        if (message == null) {
            return "";
        }
        String normalized = message.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 240 ? normalized : normalized.substring(0, 240) + "...";
    }
}
