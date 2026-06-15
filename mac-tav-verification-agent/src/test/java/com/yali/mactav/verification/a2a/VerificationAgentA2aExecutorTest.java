package com.yali.mactav.verification.a2a;

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
import com.yali.mactav.model.enums.StageStatus;
import com.yali.mactav.model.enums.ValidationStatus;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.verification.ValidationEvidence;
import com.yali.mactav.model.verification.ValidationItem;
import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.model.verification.VerificationAgentInvokePayload;
import com.yali.mactav.model.workspace.TraceRefs;
import com.yali.mactav.verification.request.VerificationAgentRequest;
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
 * Unit tests for the VerificationAgent A2A executor response contract.
 */
class VerificationAgentA2aExecutorTest {

    private static final String TASK_ID = "task-verification-a2a";

    private final ObjectMapper objectMapper = objectMapper();

    @Test
    void executeShouldReturnValidationReportJsonInA2aOutputArtifact() throws Exception {
        AtomicReference<VerificationAgentRequest> capturedRequest = new AtomicReference<>();
        VerificationAgentA2aExecutor executor = new VerificationAgentA2aExecutor(request -> {
            capturedRequest.set(request);
            return validationReport(request.getTaskId(), request.getValidationVersion());
        }, objectMapper);
        EventQueue queue = EventQueue.create();

        executor.execute(context(a2aRequest(TASK_ID, payload(TASK_ID))), queue);

        String payloadJson = outputPayload(queue);
        ValidationReport report = objectMapper.readValue(payloadJson, ValidationReport.class);
        assertEquals(TASK_ID, report.getTaskId());
        assertEquals(1, report.getValidationVersion());
        assertEquals(ValidationStatus.PASSED, report.getOverallStatus());
        assertNotNull(report.getItems());
        assertEquals(1, report.getItems().size());
        assertEquals(sampleExecutionReportJson(), capturedRequest.get().getExecutionReportJson());
        assertTrue(capturedRequest.get().getWorkspaceSnapshot().contains("\"currentArtifactRefs\""));
        assertTrue(capturedRequest.get().getWorkspaceSnapshot().contains("EXECUTION_REPORT"));
        assertTrue(capturedRequest.get().getWorkspaceSnapshot().contains("currentExecutionReportSummary"));
        assertFalse(capturedRequest.get().getWorkspaceSnapshot().contains("\"events\""));
        assertFalse(capturedRequest.get().getWorkspaceSnapshot().contains("\"agentExecutionRecords\""));
        assertFalse(capturedRequest.get().getWorkspaceSnapshot().contains("\"changeHistory\""));
        assertFalse(capturedRequest.get().getWorkspaceSnapshot().contains("\"payloadJson\""));
        assertFalse(payloadJson.contains("VerificationResponseSchema"), "A2A output must not expose VerificationResponseSchema shell");
    }

    @Test
    void executeShouldThrowClearErrorWhenA2aRequestJsonIsInvalid() {
        VerificationAgentA2aExecutor executor = new VerificationAgentA2aExecutor(
                request -> validationReport(request.getTaskId(), request.getValidationVersion()), objectMapper);

        InvalidParamsError error = assertThrows(
                InvalidParamsError.class,
                () -> executor.execute(context("{not-a2a-request"), EventQueue.create()));

        assertTrue(error.getMessage().contains("VerificationAgent A2A execution failed")
                || error.getMessage().contains("VerificationAgent A2A JSON processing failed"));
        assertTrue(error.getMessage().contains("A2aRequest"));
    }

    @Test
    void executeShouldFailWhenPayloadJsonIsNotVerificationPayload() throws Exception {
        VerificationAgentA2aExecutor executor = new VerificationAgentA2aExecutor(
                request -> validationReport(request.getTaskId(), request.getValidationVersion()), objectMapper);
        A2aRequest request = a2aRequest("task-wrong-payload", "{\"taskId\":\"task-wrong-payload\",\"rawText\":\"intent-only\"}");

        InvalidParamsError error = assertThrows(
                InvalidParamsError.class,
                () -> executor.execute(context(request), EventQueue.create()));

        assertTrue(error.getMessage().contains("VerificationAgent A2A execution failed"));
        assertTrue(error.getMessage().contains("intentJson"));
    }

    @Test
    void executeShouldReturnFailureWhenVerificationAgentRunThrowsBusinessException() throws Exception {
        VerificationAgentA2aExecutor executor = new VerificationAgentA2aExecutor(request -> {
            throw new BusinessException(ErrorCode.AGENT_OUTPUT_INVALID, "invalid validation report");
        }, objectMapper);

        InvalidParamsError error = assertThrows(
                InvalidParamsError.class,
                () -> executor.execute(context(a2aRequest("task-business", payload("task-business"))), EventQueue.create()));

        assertTrue(error.getMessage().contains(ErrorCode.AGENT_OUTPUT_INVALID.name()));
        assertTrue(error.getMessage().contains("invalid validation report"));
    }

    private RequestContext context(A2aRequest request) throws Exception {
        return context(objectMapper.writeValueAsString(request));
    }

    private RequestContext context(String requestJson) {
        Message message = new Message(
                Message.Role.USER,
                List.of(new TextPart(requestJson)),
                "msg-verification",
                "ctx-verification",
                "task-verification",
                List.of(),
                Map.of());
        Task task = new Task(
                "task-verification",
                "ctx-verification",
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
                .targetAgent("VerificationAgent")
                .stage(WorkflowStage.VERIFICATION)
                .artifactVersion(1)
                .payloadJson(payloadJson)
                .traceId("trace-" + taskId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private String payload(String taskId) throws Exception {
        return objectMapper.writeValueAsString(VerificationAgentInvokePayload.builder()
                .taskId(taskId)
                .rawText("Validate execution evidence against current intent and plan.")
                .intentVersion(1)
                .planVersion(1)
                .configVersion(1)
                .executionVersion(1)
                .validationVersion(1)
                .intentJson("{\"taskId\":\"" + taskId + "\",\"intentVersion\":1}")
                .planJson("{\"taskId\":\"" + taskId + "\",\"planVersion\":1}")
                .configSetJson("{\"taskId\":\"" + taskId + "\",\"configVersion\":1}")
                .executionReportJson(sampleExecutionReportJson())
                .traceId("trace-" + taskId)
                .workspaceSnapshot(compactWorkspaceSnapshot(taskId))
                .createdBy("unit-test")
                .build());
    }

    private String sampleExecutionReportJson() {
        return "{\"taskId\":\"" + TASK_ID + "\",\"executionVersion\":1,\"overallStatus\":\"SUCCESS\",\"testResults\":[]}";
    }

    private String compactWorkspaceSnapshot(String taskId) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "task", Map.of(
                        "taskId", taskId,
                        "taskStatus", "RUNNING",
                        "currentStage", "VERIFICATION"),
                "currentIntentVersion", 1,
                "currentPlanVersion", 1,
                "currentConfigVersion", 1,
                "currentExecutionVersion", 1,
                "validationVersion", 1,
                "currentArtifactRefs", Map.of(
                        "NETWORK_INTENT", "artifact-network-intent-v1",
                        "NETWORK_PLAN", "artifact-network-plan-v1",
                        "CONFIG_SET", "artifact-config-set-v1",
                        "EXECUTION_REPORT", "artifact-execution-report-v1"),
                "currentExecutionReportSummary", Map.of(
                        "taskId", taskId,
                        "overallStatus", "SUCCESS",
                        "testResultCount", 1,
                        "errorCount", 0,
                        "warningCount", 0)));
    }

    private ValidationReport validationReport(String taskId, Integer validationVersion) {
        return ValidationReport.builder()
                .validationId("validation-" + taskId + "-v" + validationVersion)
                .taskId(taskId)
                .executionId("execution-" + taskId + "-v1")
                .intentVersion(1)
                .planVersion(1)
                .configVersion(1)
                .executionVersion(1)
                .validationVersion(validationVersion)
                .overallStatus(ValidationStatus.PASSED)
                .summary("Execution evidence satisfies the requested intent.")
                .items(List.of(ValidationItem.builder()
                        .itemId("validation-item-1")
                        .name("Reachability check")
                        .type("REACHABILITY")
                        .expected("allowed")
                        .actual("allowed")
                        .passed(true)
                        .severity("LOW")
                        .relatedIntentRelationId("rel-allow-1")
                        .relatedPlanElementIds(List.of("policy-allow-1"))
                        .relatedConfigBlockIds(List.of("block-allow-1"))
                        .relatedTestId("test-ping-1")
                        .evidenceIds(List.of("evidence-1"))
                        .message("Reachability test passed")
                        .build()))
                .evidences(List.of(ValidationEvidence.builder()
                        .evidenceId("evidence-1")
                        .evidenceType("TEST_RESULT")
                        .source("EXECUTION_REPORT")
                        .rawValue("passed")
                        .normalizedValue("PASSED")
                        .relatedTestId("test-ping-1")
                        .build()))
                .traceRefs(TraceRefs.builder()
                        .intentRelationIds(List.of("rel-allow-1"))
                        .planElementIds(List.of("policy-allow-1"))
                        .configBlockIds(List.of("block-allow-1"))
                        .testIds(List.of("test-ping-1"))
                        .build())
                .stageStatus(StageStatus.SUCCESS)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
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
