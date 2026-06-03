package com.yali.mactav.verification.tool;

import java.util.List;
import java.util.Locale;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Spring AI method tool that classifies execution facts for VerificationAgent.
 */
public class VerificationFactTool {

    @Tool(name = "classifyConnectivityExpectation", description = "Classify whether an intent relation expects traffic to be reachable or blocked.")
    public ConnectivityExpectation classifyConnectivityExpectation(
            @ToolParam(required = true, description = "Intent relation action, such as ALLOW or DENY.") String relationAction,
            @ToolParam(required = true, description = "Intent relation type, such as ACCESS or ISOLATION.") String relationType) {
        String action = normalize(relationAction);
        String type = normalize(relationType);
        if ("DENY".equals(action) || "ISOLATION".equals(type)) {
            return new ConnectivityExpectation("BLOCKED", "Traffic must be blocked by policy or isolation.");
        }
        if ("ALLOW".equals(action) || "ACCESS".equals(type) || "SERVICE_ACCESS".equals(type)) {
            return new ConnectivityExpectation("REACHABLE", "Traffic must be reachable for the requested access relation.");
        }
        return new ConnectivityExpectation("UNKNOWN", "The relation does not provide a deterministic connectivity expectation.");
    }

    @Tool(name = "summarizeTestStatus", description = "Summarize execution test status without re-running any test.")
    public TestStatusSummary summarizeTestStatus(
            @ToolParam(required = true, description = "Execution test id.") String testId,
            @ToolParam(required = true, description = "Execution test status.") String status,
            @ToolParam(required = false, description = "Expected result recorded by ExecutionReport.") String expectedResult,
            @ToolParam(required = false, description = "Actual result recorded by ExecutionReport.") String actualResult,
            @ToolParam(required = false, description = "Trace ids attached to the execution test.") List<String> traceIds) {
        return new TestStatusSummary(
                testId,
                normalize(status),
                expectedResult,
                actualResult,
                traceIds == null ? List.of() : traceIds);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    /**
     * Tool result describing the expected connectivity outcome.
     */
    public record ConnectivityExpectation(String expectedResult, String reason) {
    }

    /**
     * Tool result summarizing an already produced execution test.
     */
    public record TestStatusSummary(
            String testId,
            String status,
            String expectedResult,
            String actualResult,
            List<String> traceIds) {
    }
}
