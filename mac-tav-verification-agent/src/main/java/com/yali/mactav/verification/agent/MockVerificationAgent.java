package com.yali.mactav.verification.agent;

import com.yali.mactav.agent.core.context.AgentContext;
import com.yali.mactav.agent.core.result.AgentResult;
import com.yali.mactav.common.enums.StageStatus;
import com.yali.mactav.common.enums.ValidationStatus;
import com.yali.mactav.model.execution.ConnectivityTestResult;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.execution.PolicyTestResult;
import com.yali.mactav.model.execution.TestResult;
import com.yali.mactav.model.verification.ValidationItem;
import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.verification.service.VerificationInput;
import java.util.ArrayList;
import java.util.List;

public class MockVerificationAgent implements VerificationAgent {

    public static final String AGENT_NAME = "MockVerificationAgent";

    @Override
    public AgentResult<ValidationReport> execute(AgentContext context, VerificationInput input) {
        ValidationReport report = verify(input);
        return AgentResult.success(report, "Mock validation completed", AGENT_NAME, "VERIFICATION");
    }

    public ValidationReport verify(VerificationInput input) {
        ExecutionReport executionReport = input == null ? null : input.getExecutionReport();
        List<ValidationItem> items = List.of(
                item("val-001", "Office reaches server", "CONNECTIVITY", "REACHABLE",
                        observed(executionReport, "test-001"), "rel-001",
                        List.of("address-office", "address-server", "routing-ospf", "routing-ospf-r1"),
                        List.of("R1-IF-001", "R1-OSPF-001"), "test-001"),
                item("val-002", "Guest is blocked from server", "ISOLATION", "BLOCKED",
                        observed(executionReport, "test-002"), "rel-002",
                        List.of("policy-001"),
                        List.of("R1-ACL-001"), "test-002"),
                item("val-003", "Office reaches internet", "CONNECTIVITY", "REACHABLE",
                        observed(executionReport, "test-003"), "rel-003",
                        List.of("nat-internet-access", "routing-ospf"),
                        List.of("R1-NAT-001", "R1-OSPF-001"), "test-003"),
                item("val-004", "Guest reaches internet", "CONNECTIVITY", "REACHABLE",
                        observed(executionReport, "test-004"), "rel-004",
                        List.of("nat-internet-access", "routing-ospf"),
                        List.of("R1-NAT-001", "R1-OSPF-001"), "test-004"),
                item("val-005", "Office and guest are isolated", "ISOLATION", "BLOCKED",
                        observed(executionReport, "test-005"), "rel-005",
                        List.of("policy-002", "vlan-office", "vlan-guest"),
                        List.of("R1-ACL-001", "SW1-VLAN-001", "SW1-PORT-001"), "test-005")
        );
        boolean allPassed = items.stream().allMatch(item -> Boolean.TRUE.equals(item.getPassed()));
        List<String> suggestions = new ArrayList<>();
        suggestions.add("DryRun result only; replace with real Mininet/Ryu tests in later phases.");
        if (!allPassed) {
            suggestions.add("Guest-to-server failures should inspect policy-001 and R1-ACL-001 first.");
        }
        return ValidationReport.builder()
                .taskId(executionReport == null ? inputTaskId(input) : executionReport.getTaskId())
                .intentVersion(input == null || input.getIntent() == null ? 1 : input.getIntent().getIntentVersion())
                .planVersion(input == null || input.getPlan() == null ? 1 : input.getPlan().getPlanVersion())
                .configVersion(input == null || input.getConfigSet() == null ? 1 : input.getConfigSet().getConfigVersion())
                .executionVersion(executionReport == null ? 1 : executionReport.getExecutionVersion())
                .validationVersion(1)
                .overallStatus(allPassed ? ValidationStatus.PASSED : ValidationStatus.FAILED)
                .summary(allPassed
                        ? "All DryRun tests satisfy the network intent."
                        : "One or more DryRun tests do not satisfy the network intent.")
                .items(items)
                .suggestions(suggestions)
                .stageStatus(StageStatus.SUCCESS)
                .build();
    }

    private String inputTaskId(VerificationInput input) {
        if (input == null) {
            return null;
        }
        if (input.getIntent() != null) {
            return input.getIntent().getTaskId();
        }
        if (input.getPlan() != null) {
            return input.getPlan().getTaskId();
        }
        if (input.getConfigSet() != null) {
            return input.getConfigSet().getTaskId();
        }
        return null;
    }

    private ValidationItem item(String itemId, String name, String type, String expected, ObservedTest observed,
                                String relatedIntentRelationId, List<String> relatedPlanElementIds,
                                List<String> relatedConfigBlockIds, String relatedTestId) {
        String actual = observed == null ? "UNKNOWN" : observed.actual;
        boolean passed = observed != null && expected.equals(actual) && observed.success;
        return ValidationItem.builder()
                .itemId(itemId)
                .name(name)
                .type(type)
                .expected(expected)
                .actual(actual)
                .passed(passed)
                .relatedIntentRelationId(relatedIntentRelationId)
                .relatedPlanElementIds(relatedPlanElementIds)
                .relatedConfigBlockIds(relatedConfigBlockIds)
                .relatedTestId(relatedTestId)
                .message(passed
                        ? name + " matched expected result."
                        : name + " expected " + expected + " but actual was " + actual + ".")
                .build();
    }

    private ObservedTest observed(ExecutionReport executionReport, String testId) {
        if (executionReport == null || executionReport.getTestResult() == null) {
            return null;
        }
        TestResult testResult = executionReport.getTestResult();
        if (testResult.getConnectivityTests() != null) {
            for (ConnectivityTestResult result : testResult.getConnectivityTests()) {
                if (testId.equals(result.getTestId())) {
                    return new ObservedTest(result.getActual(), Boolean.TRUE.equals(result.getSuccess()));
                }
            }
        }
        if (testResult.getPolicyTests() != null) {
            for (PolicyTestResult result : testResult.getPolicyTests()) {
                if (testId.equals(result.getTestId())) {
                    return new ObservedTest(result.getActual(), Boolean.TRUE.equals(result.getSuccess()));
                }
            }
        }
        return null;
    }

    private static final class ObservedTest {

        private final String actual;
        private final boolean success;

        private ObservedTest(String actual, boolean success) {
            this.actual = actual;
            this.success = success;
        }
    }
}
