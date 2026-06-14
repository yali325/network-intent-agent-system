package com.yali.mactav.configuration.a2a;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.configuration.ConfigurationTestFixtures;
import com.yali.mactav.model.a2a.A2aRequest;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.config.ConfigurationAgentInvokePayload;
import com.yali.mactav.model.enums.WorkflowStage;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.spec.Event;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the ConfigurationAgent A2A executor response contract.
 */
class ConfigurationAgentA2aExecutorTest {

    private final ObjectMapper objectMapper = objectMapper();

    @Test
    void executeShouldReturnConfigSetJsonInA2aOutputArtifact() throws Exception {
        AtomicReference<ConfigurationAgentInvokePayload> capturedPayload = new AtomicReference<>();
        ConfigurationAgentA2aExecutor executor = new ConfigurationAgentA2aExecutor(payload -> {
            capturedPayload.set(payload);
            ConfigSet configSet = ConfigurationTestFixtures.validConfigSet(objectMapper);
            configSet.setTaskId(payload.getTaskId());
            configSet.setPlanVersion(payload.getPlanVersion());
            configSet.setConfigVersion(payload.getConfigVersion());
            configSet.setCreatedBy(payload.getCreatedBy());
            return configSet;
        }, objectMapper);
        EventQueue queue = EventQueue.create();

        executor.execute(context(a2aRequest(ConfigurationTestFixtures.TASK_ID, payload(ConfigurationTestFixtures.TASK_ID))), queue);

        String payloadJson = outputPayload(queue);
        ConfigSet configSet = objectMapper.readValue(payloadJson, ConfigSet.class);
        assertEquals(ConfigurationTestFixtures.TASK_ID, configSet.getTaskId());
        assertEquals(1, configSet.getPlanVersion());
        assertEquals(1, configSet.getConfigVersion());
        assertNotNull(configSet.getDeviceConfigs());
        assertEquals(samplePlanJson(), capturedPayload.get().getPlanJson());
        assertTrue(capturedPayload.get().getWorkspaceSnapshot().contains("\"currentArtifactRefs\""));
        assertTrue(capturedPayload.get().getWorkspaceSnapshot().contains("NETWORK_PLAN"));
        assertFalse(capturedPayload.get().getWorkspaceSnapshot().contains("\"events\""));
        assertFalse(capturedPayload.get().getWorkspaceSnapshot().contains("\"agentExecutionRecords\""));
        assertFalse(capturedPayload.get().getWorkspaceSnapshot().contains("\"changeHistory\""));
        assertFalse(capturedPayload.get().getWorkspaceSnapshot().contains("\"payloadJson\""));
        assertFalse(payloadJson.contains("ConfigurationResponseSchema"), "A2A output must not expose ConfigurationResponseSchema class shell");
    }

    @Test
    void executeShouldThrowClearErrorWhenA2aRequestJsonIsInvalid() {
        ConfigurationAgentA2aExecutor executor = new ConfigurationAgentA2aExecutor(
                payload -> ConfigurationTestFixtures.validConfigSet(objectMapper), objectMapper);

        InvalidParamsError error = assertThrows(
                InvalidParamsError.class,
                () -> executor.execute(context("{not-a2a-request"), EventQueue.create()));

        assertTrue(error.getMessage().contains("ConfigurationAgent A2A execution failed"));
        assertTrue(error.getMessage().contains("A2aRequest"));
    }

    @Test
    void executeShouldFailWhenPayloadJsonIsNotConfigurationPayload() throws Exception {
        ConfigurationAgentA2aExecutor executor = new ConfigurationAgentA2aExecutor(
                payload -> ConfigurationTestFixtures.validConfigSet(objectMapper), objectMapper);
        A2aRequest request = a2aRequest("task-wrong-payload", "{\"taskId\":\"task-wrong-payload\",\"rawText\":\"intent-only\"}");

        InvalidParamsError error = assertThrows(
                InvalidParamsError.class,
                () -> executor.execute(context(request), EventQueue.create()));

        assertTrue(error.getMessage().contains("ConfigurationAgent A2A execution failed"));
        assertTrue(error.getMessage().contains("planJson"));
    }

    @Test
    void executeShouldReturnFailureWhenConfigurationAgentRunThrowsBusinessException() throws Exception {
        ConfigurationAgentA2aExecutor executor = new ConfigurationAgentA2aExecutor(payload -> {
            throw new BusinessException(ErrorCode.AGENT_OUTPUT_INVALID, "invalid config set");
        }, objectMapper);

        InvalidParamsError error = assertThrows(
                InvalidParamsError.class,
                () -> executor.execute(context(a2aRequest("task-business", payload("task-business"))), EventQueue.create()));

        assertTrue(error.getMessage().contains(ErrorCode.AGENT_OUTPUT_INVALID.name()));
        assertTrue(error.getMessage().contains("invalid config set"));
    }

    private RequestContext context(A2aRequest request) throws Exception {
        return context(objectMapper.writeValueAsString(request));
    }

    private RequestContext context(String requestJson) {
        Message message = new Message(
                Message.Role.USER,
                List.of(new TextPart(requestJson)),
                "msg-configuration",
                "ctx-configuration",
                "task-configuration",
                List.of(),
                Map.of());
        Task task = new Task(
                "task-configuration",
                "ctx-configuration",
                new TaskStatus(TaskState.SUBMITTED),
                new ArrayList<>(),
                List.of(message),
                Map.of());
        return new RequestContext(new MessageSendParams(message, null, Map.of()), task.getId(), task.getContextId(), task, List.of());
    }

    private A2aRequest a2aRequest(String taskId, String payloadJson) {
        return A2aRequest.builder()
                .taskId(taskId)
                .sourceAgent("MacTavOrchestrator")
                .targetAgent("ConfigurationAgent")
                .stage(WorkflowStage.CONFIGURATION)
                .artifactVersion(1)
                .payloadJson(payloadJson)
                .traceId("trace-" + taskId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private String payload(String taskId) throws Exception {
        return objectMapper.writeValueAsString(ConfigurationAgentInvokePayload.builder()
                .taskId(taskId)
                .rawText("Generate structured configuration from the current NetworkPlan.")
                .intentVersion(1)
                .planVersion(1)
                .planJson(samplePlanJson())
                .configVersion(1)
                .traceId("trace-" + taskId)
                .workspaceSnapshot(compactWorkspaceSnapshot(taskId))
                .targetEnvironmentHint("lab")
                .createdBy("unit-test")
                .build());
    }

    private String samplePlanJson() {
        return "{\"taskId\":\"task-enterprise-office-guest\",\"planVersion\":1,\"topology\":{\"nodes\":[],\"links\":[]}}";
    }

    private String compactWorkspaceSnapshot(String taskId) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "task", Map.of(
                        "taskId", taskId,
                        "taskStatus", "RUNNING",
                        "currentStage", "CONFIGURATION"),
                "currentIntentVersion", 1,
                "currentPlanVersion", 1,
                "currentArtifactRefs", Map.of(
                        "NETWORK_INTENT", "artifact-network-intent-v1",
                        "NETWORK_PLAN", "artifact-network-plan-v1"),
                "currentPlanSummary", Map.of(
                        "taskId", taskId,
                        "planVersion", 1,
                        "topologyNodeCount", 2,
                        "securityPolicyCount", 1)));
    }

    private String outputPayload(EventQueue queue) throws Exception {
        for (int i = 0; i < 5; i++) {
            Event event = queue.dequeueEvent(100);
            if (event instanceof TaskArtifactUpdateEvent artifactEvent) {
                Object output = artifactEvent.getArtifact().metadata().get("output");
                if (output != null) {
                    return output.toString();
                }
            }
            if (event instanceof TaskStatusUpdateEvent statusEvent && statusEvent.getStatus().state().isFinal()) {
                break;
            }
        }
        throw new AssertionError("A2A output artifact metadata was not emitted");
    }

    private static ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
