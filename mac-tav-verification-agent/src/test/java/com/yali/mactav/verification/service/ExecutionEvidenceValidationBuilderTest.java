package com.yali.mactav.verification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yali.mactav.model.config.CommandBlock;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.config.DeviceConfig;
import com.yali.mactav.model.enums.ValidationStatus;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.execution.TestResult;
import com.yali.mactav.model.execution.TestResultStatus;
import com.yali.mactav.model.execution.TestResultType;
import com.yali.mactav.model.intent.IntentRelation;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.intent.SemanticIntentGraph;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.plan.SecurityPolicyPlanItem;
import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.model.workspace.TraceRefs;
import com.yali.mactav.verification.request.VerificationAgentRequest;
import com.yali.mactav.verification.validator.VerificationOutputValidator;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Offline tests for deterministic validation from real execution evidence.
 */
class ExecutionEvidenceValidationBuilderTest {

    private static final String TASK_ID = "task-evidence-validation";

    private final ObjectMapper objectMapper = objectMapper();

    private final ExecutionEvidenceValidationBuilder builder = new ExecutionEvidenceValidationBuilder(
            objectMapper, new VerificationOutputValidator());

    @Test
    void buildShouldPassDenyRelationWhenExpectedUnreachableTestPassed() throws Exception {
        ValidationReport report = builder.build(request(List.of(
                passingOfficeServerTest(),
                test(
                        "test-ping-guest-server",
                        "guest",
                        "server",
                        "unreachable",
                        null,
                        null,
                        TestResultStatus.PASSED,
                        TraceRefs.builder()
                                .intentRelationIds(List.of("rel-guest-server"))
                                .planElementIds(List.of("policy-guest-server"))
                                .build()))));

        assertEquals(ValidationStatus.PASSED, report.getOverallStatus());
        var item = report.getItems().stream()
                .filter(candidate -> "rel-guest-server".equals(candidate.getRelatedIntentRelationId()))
                .findFirst()
                .orElseThrow();
        assertTrue(item.getPassed());
        assertEquals("UNREACHABLE", item.getActual());
        assertEquals("test-ping-guest-server", item.getRelatedTestId());
    }

    @Test
    void buildShouldFailDenyRelationWhenPingWasActuallyReachable() throws Exception {
        ValidationReport report = builder.build(request(List.of(
                passingOfficeServerTest(),
                test(
                        "test-ping-guest-server",
                        "guest",
                        "server",
                        "unreachable",
                        "0% packet loss",
                        "1 packets transmitted, 1 received, 0% packet loss",
                        TestResultStatus.FAILED,
                        TraceRefs.builder()
                                .intentRelationIds(List.of("rel-guest-server"))
                                .planElementIds(List.of("policy-guest-server"))
                                .build()))));

        assertEquals(ValidationStatus.FAILED, report.getOverallStatus());
        var item = report.getItems().stream()
                .filter(candidate -> "rel-guest-server".equals(candidate.getRelatedIntentRelationId()))
                .findFirst()
                .orElseThrow();
        assertFalse(item.getPassed());
        assertEquals("REACHABLE", item.getActual());
        assertFalse(report.getEvidences().isEmpty());
    }

    @Test
    void buildShouldPassAllowRelationWhenPingIsReachable() throws Exception {
        ValidationReport report = builder.build(request(List.of(
                passingGuestServerDenyTest(),
                passingOfficeServerTest())));

        assertEquals(ValidationStatus.PASSED, report.getOverallStatus());
        assertTrue(report.getItems().stream()
                .filter(item -> "rel-office-server".equals(item.getRelatedIntentRelationId()))
                .findFirst()
                .orElseThrow()
                .getPassed());
    }

    @Test
    void buildShouldReturnPartialWhenRelationEvidenceIsMissing() throws Exception {
        ValidationReport report = builder.build(request(List.of(test(
                "test-ping-office-server",
                "office",
                "server",
                "reachable",
                "0% packet loss",
                null,
                TestResultStatus.PASSED,
                TraceRefs.builder()
                        .intentRelationIds(List.of("rel-office-server"))
                        .planElementIds(List.of("policy-office-server"))
                        .build()))));

        assertEquals(ValidationStatus.PARTIAL, report.getOverallStatus());
        assertTrue(report.getItems().stream()
                .anyMatch(item -> "rel-guest-server".equals(item.getRelatedIntentRelationId())
                        && "UNKNOWN".equals(item.getActual())));
    }

    @Test
    void buildShouldSerializeAndDeserializeValidatedReport() throws Exception {
        ValidationReport report = builder.build(request(List.of(test(
                "test-ping-office-server",
                "office",
                "server",
                "reachable",
                "0% packet loss",
                null,
                TestResultStatus.PASSED,
                TraceRefs.builder()
                        .intentRelationIds(List.of("rel-office-server"))
                        .planElementIds(List.of("policy-office-server"))
                        .build()))));

        String json = objectMapper.writeValueAsString(report);
        ValidationReport reparsed = objectMapper.readValue(json, ValidationReport.class);

        new VerificationOutputValidator().validateAndReturn(reparsed);
    }

    private VerificationAgentRequest request(List<TestResult> testResults) throws Exception {
        return VerificationAgentRequest.builder()
                .taskId(TASK_ID)
                .rawText("Validate execution evidence.")
                .intentVersion(1)
                .planVersion(1)
                .configVersion(1)
                .executionVersion(1)
                .validationVersion(1)
                .intentJson(objectMapper.writeValueAsString(intent()))
                .planJson(objectMapper.writeValueAsString(plan()))
                .configSetJson(objectMapper.writeValueAsString(configSet()))
                .executionReportJson(objectMapper.writeValueAsString(executionReport(testResults)))
                .traceId("trace-evidence-validation")
                .build();
    }

    private NetworkIntent intent() {
        return NetworkIntent.builder()
                .taskId(TASK_ID)
                .intentVersion(1)
                .semanticIntentGraph(SemanticIntentGraph.builder()
                        .relations(List.of(
                                IntentRelation.builder()
                                        .id("rel-office-server")
                                        .source("office")
                                        .target("server")
                                        .action("ALLOW")
                                        .type("ACCESS")
                                        .build(),
                                IntentRelation.builder()
                                        .id("rel-guest-server")
                                        .source("guest")
                                        .target("server")
                                        .action("DENY")
                                        .type("ACCESS")
                                        .build()))
                        .build())
                .build();
    }

    private NetworkPlan plan() {
        return NetworkPlan.builder()
                .taskId(TASK_ID)
                .planId("plan-" + TASK_ID)
                .planVersion(1)
                .securityPolicyPlan(List.of(
                        SecurityPolicyPlanItem.builder()
                                .id("policy-office-server")
                                .sourceZone("office")
                                .targetZone("server")
                                .action("ALLOW")
                                .basedOnIntentRelation("rel-office-server")
                                .traceRefs(TraceRefs.builder()
                                        .planElementIds(List.of("policy-office-server"))
                                        .intentRelationIds(List.of("rel-office-server"))
                                        .build())
                                .build(),
                        SecurityPolicyPlanItem.builder()
                                .id("policy-guest-server")
                                .sourceZone("guest")
                                .targetZone("server")
                                .action("DENY")
                                .basedOnIntentRelation("rel-guest-server")
                                .traceRefs(TraceRefs.builder()
                                        .planElementIds(List.of("policy-guest-server"))
                                        .intentRelationIds(List.of("rel-guest-server"))
                                        .build())
                                .build()))
                .build();
    }

    private ConfigSet configSet() {
        return ConfigSet.builder()
                .taskId(TASK_ID)
                .planId("plan-" + TASK_ID)
                .configSetId("config-" + TASK_ID + "-v1")
                .configVersion(1)
                .deviceConfigs(List.of(DeviceConfig.builder()
                        .deviceId("sw-core")
                        .commandBlocks(List.of(
                                block("cfg-office-server", "policy-office-server", "rel-office-server"),
                                block("cfg-guest-server", "policy-guest-server", "rel-guest-server")))
                        .build()))
                .build();
    }

    private CommandBlock block(String blockId, String planElementId, String relationId) {
        return CommandBlock.builder()
                .blockId(blockId)
                .commands(List.of("acl number 3001", "return"))
                .rollbackCommands(List.of("undo acl number 3001"))
                .traceRefs(TraceRefs.builder()
                        .planElementIds(List.of(planElementId))
                        .intentRelationIds(List.of(relationId))
                        .build())
                .build();
    }

    private ExecutionReport executionReport(List<TestResult> testResults) {
        return ExecutionReport.builder()
                .executionId("execution-" + TASK_ID + "-v1")
                .taskId(TASK_ID)
                .planId("plan-" + TASK_ID)
                .configSetId("config-" + TASK_ID + "-v1")
                .executionVersion(1)
                .testResults(testResults)
                .build();
    }

    private TestResult test(String testId,
                            String source,
                            String target,
                            String expected,
                            String actual,
                            String logs,
                            TestResultStatus status,
                            TraceRefs traceRefs) {
        return TestResult.builder()
                .testId(testId)
                .testType(TestResultType.PING)
                .sourceNode(source)
                .targetNode(target)
                .expectedResult(expected)
                .actualResult(actual)
                .logsSummary(logs)
                .status(status)
                .traceRefs(traceRefs)
                .build();
    }

    private TestResult passingOfficeServerTest() {
        return test(
                "test-ping-office-server",
                "office",
                "server",
                "reachable",
                "0% packet loss",
                null,
                TestResultStatus.PASSED,
                TraceRefs.builder()
                        .intentRelationIds(List.of("rel-office-server"))
                        .planElementIds(List.of("policy-office-server"))
                        .build());
    }

    private TestResult passingGuestServerDenyTest() {
        return test(
                "test-ping-guest-server",
                "guest",
                "server",
                "unreachable",
                null,
                null,
                TestResultStatus.PASSED,
                TraceRefs.builder()
                        .intentRelationIds(List.of("rel-guest-server"))
                        .planElementIds(List.of("policy-guest-server"))
                        .build());
    }

    private static ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
