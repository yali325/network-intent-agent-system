package com.yali.mactav.intent.a2a;

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
import com.yali.mactav.intent.request.IntentAgentRequest;
import com.yali.mactav.model.a2a.A2aRequest;
import com.yali.mactav.model.enums.StageStatus;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.intent.IntentAgentInvokePayload;
import com.yali.mactav.model.intent.IntentNode;
import com.yali.mactav.model.intent.IntentRelation;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.intent.SemanticIntentGraph;
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
 * Unit tests for the IntentAgent A2A executor response contract.
 */
class IntentAgentA2aExecutorTest {

    private final ObjectMapper objectMapper = objectMapper();

    @Test
    void executeShouldReturnNetworkIntentJsonInA2aOutputArtifact() throws Exception {
        AtomicReference<IntentAgentRequest> capturedRequest = new AtomicReference<>();
        IntentAgentA2aExecutor executor = new IntentAgentA2aExecutor(request -> {
            capturedRequest.set(request);
            return networkIntent(request);
        }, objectMapper);
        EventQueue queue = EventQueue.create();

        executor.execute(context(a2aRequest("task-a2a", payload("task-a2a", "allow office to server"))), queue);

        String payloadJson = outputPayload(queue);
        NetworkIntent intent = objectMapper.readValue(payloadJson, NetworkIntent.class);
        assertEquals("task-a2a", intent.getTaskId());
        assertEquals("allow office to server", intent.getRawText());
        assertEquals(StageStatus.SUCCESS, intent.getStageStatus());
        assertNotNull(intent.getSemanticIntentGraph());
        assertEquals("allow office to server", capturedRequest.get().getRawText());
        assertFalse(payloadJson.contains("semanticSummary"), "A2A output must not expose IntentResponseSchema shell");
    }

    @Test
    void executeShouldThrowClearErrorWhenPayloadJsonIsInvalid() throws Exception {
        IntentAgentA2aExecutor executor = new IntentAgentA2aExecutor(request -> networkIntent(request), objectMapper);
        A2aRequest request = a2aRequest("task-invalid", "{not-json");

        InvalidParamsError error = assertThrows(
                InvalidParamsError.class,
                () -> executor.execute(context(request), EventQueue.create()));

        assertTrue(error.getMessage().contains("IntentAgent A2A execution failed"));
        assertTrue(error.getMessage().contains("payloadJson"));
    }

    @Test
    void executeShouldReturnFailureWhenIntentAgentRunThrowsBusinessException() throws Exception {
        IntentAgentA2aExecutor executor = new IntentAgentA2aExecutor(request -> {
            throw new BusinessException(ErrorCode.AGENT_OUTPUT_INVALID, "invalid intent graph");
        }, objectMapper);

        InvalidParamsError error = assertThrows(
                InvalidParamsError.class,
                () -> executor.execute(context(a2aRequest("task-business", payload("task-business", "x"))), EventQueue.create()));

        assertTrue(error.getMessage().contains(ErrorCode.AGENT_OUTPUT_INVALID.name()));
        assertTrue(error.getMessage().contains("invalid intent graph"));
    }

    private RequestContext context(A2aRequest request) throws Exception {
        String requestJson = objectMapper.writeValueAsString(request);
        Message message = new Message(
                Message.Role.USER,
                List.of(new TextPart(requestJson)),
                "msg-" + request.getTaskId(),
                "ctx-" + request.getTaskId(),
                request.getTaskId(),
                List.of(),
                Map.of());
        Task task = new Task(
                request.getTaskId(),
                "ctx-" + request.getTaskId(),
                new TaskStatus(TaskState.SUBMITTED),
                new ArrayList<>(),
                List.of(message),
                Map.of());
        return new RequestContext(new MessageSendParams(message, null, Map.of()), request.getTaskId(), task.getContextId(), task, List.of());
    }

    private A2aRequest a2aRequest(String taskId, String payloadJson) {
        return A2aRequest.builder()
                .taskId(taskId)
                .sourceAgent("MacTavOrchestrator")
                .targetAgent("IntentAgent")
                .stage(WorkflowStage.INTENT)
                .artifactVersion(1)
                .payloadJson(payloadJson)
                .traceId("trace-" + taskId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private String payload(String taskId, String rawText) throws Exception {
        return objectMapper.writeValueAsString(IntentAgentInvokePayload.builder()
                .taskId(taskId)
                .rawText(rawText)
                .intentVersion(1)
                .traceId("trace-" + taskId)
                .targetEnvironmentHint("lab")
                .createdBy("unit-test")
                .build());
    }

    private NetworkIntent networkIntent(IntentAgentRequest request) {
        return NetworkIntent.builder()
                .taskId(request.getTaskId())
                .intentVersion(request.getIntentVersion())
                .rawText(request.getRawText())
                .semanticIntentGraph(SemanticIntentGraph.builder()
                        .nodes(List.of(IntentNode.builder().id("node-office").name("office").type("ZONE").build()))
                        .relations(List.of(IntentRelation.builder()
                                .id("rel-office-server")
                                .type("ACCESS")
                                .source("node-office")
                                .target("node-server")
                                .action("ALLOW")
                                .build()))
                        .build())
                .stageStatus(StageStatus.SUCCESS)
                .traceId(request.getTraceId())
                .createTime(LocalDateTime.now())
                .build();
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
