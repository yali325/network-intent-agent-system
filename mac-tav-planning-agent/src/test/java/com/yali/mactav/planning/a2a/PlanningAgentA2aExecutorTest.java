package com.yali.mactav.planning.a2a;

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
import com.yali.mactav.model.a2a.A2aRequest;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.plan.PlanningAgentInvokePayload;
import com.yali.mactav.planning.PlanningTestFixtures;
import com.yali.mactav.planning.request.PlanningAgentRequest;
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
 * Unit tests for the PlanningAgent A2A executor response contract.
 */
class PlanningAgentA2aExecutorTest {

    private final ObjectMapper objectMapper = objectMapper();

    @Test
    void executeShouldReturnNetworkPlanJsonInA2aOutputArtifact() throws Exception {
        AtomicReference<PlanningAgentRequest> capturedRequest = new AtomicReference<>();
        PlanningAgentA2aExecutor executor = new PlanningAgentA2aExecutor(request -> {
            capturedRequest.set(request);
            NetworkPlan plan = PlanningTestFixtures.validPlan();
            plan.setTaskId(request.getTaskId());
            plan.setIntentVersion(request.getIntentVersion());
            plan.setPlanVersion(request.getPlanVersion());
            plan.setCreatedBy(request.getCreatedBy());
            return plan;
        }, objectMapper);
        EventQueue queue = EventQueue.create();

        executor.execute(context(a2aRequest(PlanningTestFixtures.TASK_ID, payload(PlanningTestFixtures.TASK_ID))), queue);

        String payloadJson = outputPayload(queue);
        NetworkPlan plan = objectMapper.readValue(payloadJson, NetworkPlan.class);
        assertEquals(PlanningTestFixtures.TASK_ID, plan.getTaskId());
        assertEquals(2, plan.getIntentVersion());
        assertEquals(1, plan.getPlanVersion());
        assertNotNull(plan.getTopology());
        assertEquals(PlanningTestFixtures.SAMPLE_INTENT_JSON, capturedRequest.get().getIntentJson());
        assertTrue(capturedRequest.get().getWorkspaceSnapshot().contains("\"currentArtifactRefs\""));
        assertTrue(capturedRequest.get().getWorkspaceSnapshot().contains("NETWORK_INTENT"));
        assertFalse(capturedRequest.get().getWorkspaceSnapshot().contains("\"events\""));
        assertFalse(capturedRequest.get().getWorkspaceSnapshot().contains("\"agentExecutionRecords\""));
        assertFalse(capturedRequest.get().getWorkspaceSnapshot().contains("\"changeHistory\""));
        assertFalse(capturedRequest.get().getWorkspaceSnapshot().contains("\"payloadJson\""));
        assertFalse(payloadJson.contains("topologyNodes"), "A2A output must not expose PlanningResponseSchema shell");
    }

    @Test
    void executeShouldThrowClearErrorWhenA2aRequestJsonIsInvalid() {
        PlanningAgentA2aExecutor executor = new PlanningAgentA2aExecutor(request -> PlanningTestFixtures.validPlan(), objectMapper);

        InvalidParamsError error = assertThrows(
                InvalidParamsError.class,
                () -> executor.execute(context("{not-a2a-request"), EventQueue.create()));

        assertTrue(error.getMessage().contains("PlanningAgent A2A execution failed"));
        assertTrue(error.getMessage().contains("A2aRequest"));
    }

    @Test
    void executeShouldFailWhenPayloadJsonIsNotPlanningPayload() throws Exception {
        PlanningAgentA2aExecutor executor = new PlanningAgentA2aExecutor(request -> PlanningTestFixtures.validPlan(), objectMapper);
        A2aRequest request = a2aRequest("task-wrong-payload", "{\"taskId\":\"task-wrong-payload\",\"rawText\":\"intent-only\"}");

        InvalidParamsError error = assertThrows(
                InvalidParamsError.class,
                () -> executor.execute(context(request), EventQueue.create()));

        assertTrue(error.getMessage().contains("PlanningAgent A2A execution failed"));
        assertTrue(error.getMessage().contains("intentJson"));
    }

    @Test
    void executeShouldReturnFailureWhenPlanningAgentRunThrowsBusinessException() throws Exception {
        PlanningAgentA2aExecutor executor = new PlanningAgentA2aExecutor(request -> {
            throw new BusinessException(ErrorCode.AGENT_OUTPUT_INVALID, "invalid network plan");
        }, objectMapper);

        InvalidParamsError error = assertThrows(
                InvalidParamsError.class,
                () -> executor.execute(context(a2aRequest("task-business", payload("task-business"))), EventQueue.create()));

        assertTrue(error.getMessage().contains(ErrorCode.AGENT_OUTPUT_INVALID.name()));
        assertTrue(error.getMessage().contains("invalid network plan"));
    }

    private RequestContext context(A2aRequest request) throws Exception {
        return context(objectMapper.writeValueAsString(request));
    }

    private RequestContext context(String requestJson) {
        Message message = new Message(
                Message.Role.USER,
                List.of(new TextPart(requestJson)),
                "msg-planning",
                "ctx-planning",
                "task-planning",
                List.of(),
                Map.of());
        Task task = new Task(
                "task-planning",
                "ctx-planning",
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
                .targetAgent("PlanningAgent")
                .stage(WorkflowStage.PLANNING)
                .artifactVersion(1)
                .payloadJson(payloadJson)
                .traceId("trace-" + taskId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private String payload(String taskId) throws Exception {
        return objectMapper.writeValueAsString(PlanningAgentInvokePayload.builder()
                .taskId(taskId)
                .rawText(PlanningTestFixtures.RAW_TEXT)
                .intentVersion(2)
                .intentJson(PlanningTestFixtures.SAMPLE_INTENT_JSON)
                .planVersion(1)
                .traceId("trace-" + taskId)
                .workspaceSnapshot(compactWorkspaceSnapshot(taskId))
                .targetEnvironmentHint("lab")
                .createdBy("unit-test")
                .build());
    }

    private String compactWorkspaceSnapshot(String taskId) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "task", Map.of(
                        "taskId", taskId,
                        "taskStatus", "RUNNING",
                        "currentStage", "PLANNING"),
                "currentIntentVersion", 2,
                "currentArtifactRefs", Map.of("NETWORK_INTENT", "artifact-network-intent-v1"),
                "currentIntentSummary", Map.of(
                        "taskId", taskId,
                        "intentVersion", 2,
                        "semanticNodeCount", 2,
                        "semanticRelationCount", 1)));
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
