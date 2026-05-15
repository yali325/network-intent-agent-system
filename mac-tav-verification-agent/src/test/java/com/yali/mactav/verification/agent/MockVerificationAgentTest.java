package com.yali.mactav.verification.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yali.mactav.common.enums.ValidationStatus;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.execution.ConnectivityTestResult;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.execution.PolicyTestResult;
import com.yali.mactav.model.execution.TestResult;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.verification.service.VerificationInput;
import java.util.List;
import org.junit.jupiter.api.Test;

class MockVerificationAgentTest {

    @Test
    void defaultDryRunResultPassesValidation() {
        ValidationReport report = new MockVerificationAgent().verify(input(defaultExecutionReport()));

        assertEquals(ValidationStatus.PASSED, report.getOverallStatus());
        assertEquals(5, report.getItems().size());
        assertTrue(report.getItems().stream().allMatch(item -> Boolean.TRUE.equals(item.getPassed())));
    }

    @Test
    void guestToServerUnexpectedPassFailsValidation() {
        ExecutionReport report = defaultExecutionReport();
        report.getTestResult().getPolicyTests().set(0, PolicyTestResult.builder()
                .testId("test-002")
                .source("guest-pc-1")
                .target("server-1")
                .expected("BLOCKED")
                .actual("REACHABLE")
                .success(false)
                .rawOutput("Mock failure")
                .build());

        ValidationReport validationReport = new MockVerificationAgent().verify(input(report));

        assertEquals(ValidationStatus.FAILED, validationReport.getOverallStatus());
        assertTrue(validationReport.getItems().stream()
                .anyMatch(item -> "val-002".equals(item.getItemId()) && Boolean.FALSE.equals(item.getPassed())));
    }

    private VerificationInput input(ExecutionReport executionReport) {
        return new VerificationInput(
                NetworkIntent.builder().taskId("task-10001").intentVersion(1).build(),
                NetworkPlan.builder().taskId("task-10001").planVersion(1).build(),
                ConfigSet.builder().taskId("task-10001").configVersion(1).build(),
                executionReport
        );
    }

    private ExecutionReport defaultExecutionReport() {
        return ExecutionReport.builder()
                .taskId("task-10001")
                .planVersion(1)
                .configVersion(1)
                .executionVersion(1)
                .executionMode("DRY_RUN")
                .testResult(TestResult.builder()
                        .connectivityTests(List.of(
                                connectivity("test-001", "office-pc-1", "server-1", "REACHABLE"),
                                connectivity("test-003", "office-pc-1", "internet", "REACHABLE"),
                                connectivity("test-004", "guest-pc-1", "internet", "REACHABLE")
                        ))
                        .policyTests(new java.util.ArrayList<>(List.of(
                                policy("test-002", "guest-pc-1", "server-1", "BLOCKED"),
                                policy("test-005", "office-pc-1", "guest-pc-1", "BLOCKED")
                        )))
                        .build())
                .build();
    }

    private ConnectivityTestResult connectivity(String testId, String source, String target, String result) {
        return ConnectivityTestResult.builder()
                .testId(testId)
                .source(source)
                .target(target)
                .expected(result)
                .actual(result)
                .success(true)
                .build();
    }

    private PolicyTestResult policy(String testId, String source, String target, String result) {
        return PolicyTestResult.builder()
                .testId(testId)
                .source(source)
                .target(target)
                .expected(result)
                .actual(result)
                .success(true)
                .build();
    }
}
