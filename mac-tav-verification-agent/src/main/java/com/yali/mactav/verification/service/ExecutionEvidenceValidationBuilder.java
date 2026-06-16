package com.yali.mactav.verification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.agent.core.validator.AgentOutputValidator;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.config.CommandBlock;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.config.DeviceConfig;
import com.yali.mactav.model.enums.StageStatus;
import com.yali.mactav.model.enums.ValidationStatus;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.execution.TestResult;
import com.yali.mactav.model.execution.TestResultStatus;
import com.yali.mactav.model.intent.IntentRelation;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.plan.SecurityPolicyPlanItem;
import com.yali.mactav.model.verification.ValidationEvidence;
import com.yali.mactav.model.verification.ValidationItem;
import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.model.workspace.TraceRefs;
import com.yali.mactav.verification.request.VerificationAgentRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds ValidationReport deterministically from real ExecutionReport evidence.
 */
public class ExecutionEvidenceValidationBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionEvidenceValidationBuilder.class);

    private final ObjectMapper objectMapper;

    private final AgentOutputValidator<ValidationReport> validator;

    public ExecutionEvidenceValidationBuilder(ObjectMapper objectMapper,
                                              AgentOutputValidator<ValidationReport> validator) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
    }

    public ValidationReport build(VerificationAgentRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "VerificationAgentRequest must not be null");
        }
        long start = System.nanoTime();
        NetworkIntent intent = readOptional(request.getIntentJson(), NetworkIntent.class, "NetworkIntent");
        NetworkPlan plan = readOptional(request.getPlanJson(), NetworkPlan.class, "NetworkPlan");
        ConfigSet configSet = readOptional(request.getConfigSetJson(), ConfigSet.class, "ConfigSet");
        ExecutionReport executionReport = readRequired(
                request.getExecutionReportJson(), ExecutionReport.class, "ExecutionReport");

        List<ExpectedRelation> expectedRelations = expectedRelations(intent, plan);
        List<ValidationItem> items = new ArrayList<>();
        List<ValidationEvidence> evidences = new ArrayList<>();
        TraceRefs reportRefs = TraceRefs.builder().build();

        int index = 1;
        int missingEvidence = 0;
        int failedItems = 0;
        for (ExpectedRelation expectedRelation : expectedRelations) {
            Optional<TestResult> matched = findTestResult(executionReport, expectedRelation);
            if (matched.isEmpty()) {
                missingEvidence++;
                ValidationItem item = missingEvidenceItem(expectedRelation, index++);
                items.add(item);
                mergeReportRefs(reportRefs, item, null);
                continue;
            }
            TestResult testResult = matched.get();
            Connectivity actual = inferActualConnectivity(testResult, expectedRelation.expectedConnectivity());
            boolean passed = actual == expectedRelation.expectedConnectivity();
            if (!passed) {
                failedItems++;
            }
            ValidationEvidence evidence = evidenceFor(testResult, actual);
            ValidationItem item = itemFor(expectedRelation, testResult, evidence, actual, passed, configSet, index++);
            evidences.add(evidence);
            items.add(item);
            mergeReportRefs(reportRefs, item, testResult);
        }

        if (items.isEmpty()) {
            for (TestResult testResult : safeList(executionReport.getTestResults())) {
                Connectivity actual = inferActualConnectivity(testResult, Connectivity.UNKNOWN);
                boolean passed = testResult.getStatus() == TestResultStatus.PASSED;
                ValidationEvidence evidence = evidenceFor(testResult, actual);
                ValidationItem item = genericItemFor(testResult, evidence, actual, passed, index++);
                evidences.add(evidence);
                items.add(item);
                if (!passed) {
                    failedItems++;
                }
                mergeReportRefs(reportRefs, item, testResult);
            }
        }

        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.AGENT_OUTPUT_INVALID,
                    "Execution evidence validation cannot build report: no execution test results");
        }

        ValidationStatus status = overallStatus(failedItems, missingEvidence);
        ValidationReport report = ValidationReport.builder()
                .validationId("validation-" + valueOrFallback(request.getTaskId(), executionReport.getTaskId())
                        + "-v" + valueOrFallback(request.getValidationVersion(), 1))
                .taskId(valueOrFallback(request.getTaskId(), executionReport.getTaskId()))
                .executionId(executionReport.getExecutionId())
                .intentVersion(request.getIntentVersion())
                .planVersion(request.getPlanVersion())
                .configVersion(request.getConfigVersion())
                .executionVersion(request.getExecutionVersion())
                .validationVersion(valueOrFallback(request.getValidationVersion(), 1))
                .overallStatus(status)
                .summary(summary(status, items.size(), failedItems, missingEvidence))
                .items(items)
                .evidences(evidences)
                .suggestions(suggestions(status))
                .traceRefs(reportRefs)
                .stageStatus(StageStatus.SUCCESS)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        ValidationReport validatedReport = validator.validateAndReturn(report);
        LOGGER.info(
                "Execution evidence validation built taskId={}, traceId={}, executionStatus={}, testCount={}, failedTestCount={}, itemCount={}, evidenceCount={}, overallStatus={}, durationMs={}",
                request.getTaskId(),
                request.getTraceId(),
                executionReport.getOverallStatus(),
                safeList(executionReport.getTestResults()).size(),
                failedTestCount(executionReport),
                items.size(),
                evidences.size(),
                status,
                elapsedMillis(start));
        return validatedReport;
    }

    private List<ExpectedRelation> expectedRelations(NetworkIntent intent, NetworkPlan plan) {
        List<ExpectedRelation> relations = new ArrayList<>();
        for (SecurityPolicyPlanItem policy : safeList(plan == null ? null : plan.getSecurityPolicyPlan())) {
            Connectivity expected = expectedConnectivity(policy.getAction(), null);
            if (expected == Connectivity.UNKNOWN) {
                continue;
            }
            TraceRefs refs = TraceRefs.builder().build();
            mergeInto(refs, policy.getTraceRefs());
            add(refs.getPlanElementIds(), policy.getId());
            add(refs.getIntentRelationIds(), policy.getBasedOnIntentRelation());
            relations.add(new ExpectedRelation(
                    valueOrFallback(policy.getBasedOnIntentRelation(), "policy-" + policy.getId()),
                    policy.getSourceZone(),
                    policy.getTargetZone(),
                    expected,
                    refs));
        }
        if (!relations.isEmpty()) {
            return relations;
        }
        if (intent == null || intent.getSemanticIntentGraph() == null) {
            return relations;
        }
        for (IntentRelation relation : safeList(intent.getSemanticIntentGraph().getRelations())) {
            Connectivity expected = expectedConnectivity(relation.getAction(), relation.getType());
            if (expected == Connectivity.UNKNOWN) {
                continue;
            }
            TraceRefs refs = TraceRefs.builder()
                    .intentRelationIds(new ArrayList<>(List.of(relation.getId())))
                    .build();
            relations.add(new ExpectedRelation(
                    relation.getId(),
                    relation.getSource(),
                    relation.getTarget(),
                    expected,
                    refs));
        }
        return relations;
    }

    private Optional<TestResult> findTestResult(ExecutionReport report, ExpectedRelation relation) {
        for (TestResult result : safeList(report.getTestResults())) {
            TraceRefs refs = result.getTraceRefs();
            if (intersects(refs == null ? null : refs.getIntentRelationIds(), relation.traceRefs().getIntentRelationIds())
                    || intersects(refs == null ? null : refs.getPlanElementIds(), relation.traceRefs().getPlanElementIds())) {
                return Optional.of(result);
            }
        }
        String source = compactKey(relation.source());
        String target = compactKey(relation.target());
        for (TestResult result : safeList(report.getTestResults())) {
            String haystack = compactKey(String.join(" ",
                    valueOrFallback(result.getTestId(), ""),
                    valueOrFallback(result.getSourceNode(), ""),
                    valueOrFallback(result.getTargetNode(), "")));
            if (!source.isBlank() && !target.isBlank() && haystack.contains(source) && haystack.contains(target)) {
                return Optional.of(result);
            }
        }
        return Optional.empty();
    }

    private ValidationItem itemFor(ExpectedRelation relation,
                                   TestResult testResult,
                                   ValidationEvidence evidence,
                                   Connectivity actual,
                                   boolean passed,
                                   ConfigSet configSet,
                                   int index) {
        String relationId = valueOrFallback(first(relation.traceRefs().getIntentRelationIds()), relation.id());
        return ValidationItem.builder()
                .itemId("validation-" + safeId(relationId, String.valueOf(index)))
                .name("Relation evidence " + relation.source() + " -> " + relation.target())
                .type("RELATION_CONNECTIVITY")
                .expected(relation.expectedConnectivity().name())
                .actual(actual.name())
                .passed(passed)
                .severity(passed ? "LOW" : "HIGH")
                .relatedIntentRelationId(first(relation.traceRefs().getIntentRelationIds()))
                .relatedPlanElementIds(new ArrayList<>(relation.traceRefs().getPlanElementIds()))
                .relatedConfigBlockIds(relatedConfigBlockIds(configSet, relation))
                .relatedTestId(testResult.getTestId())
                .evidenceIds(List.of(evidence.getEvidenceId()))
                .message(passed
                        ? "Execution evidence matches the expected relation outcome."
                        : "Execution evidence conflicts with the expected relation outcome.")
                .build();
    }

    private ValidationItem missingEvidenceItem(ExpectedRelation relation, int index) {
        return ValidationItem.builder()
                .itemId("validation-missing-" + safeId(relation.id(), String.valueOf(index)))
                .name("Missing relation evidence " + relation.source() + " -> " + relation.target())
                .type("RELATION_CONNECTIVITY")
                .expected(relation.expectedConnectivity().name())
                .actual("UNKNOWN")
                .passed(false)
                .severity("MEDIUM")
                .relatedIntentRelationId(first(relation.traceRefs().getIntentRelationIds()))
                .relatedPlanElementIds(new ArrayList<>(relation.traceRefs().getPlanElementIds()))
                .message("No execution test result was found for this relation.")
                .build();
    }

    private ValidationItem genericItemFor(TestResult testResult,
                                          ValidationEvidence evidence,
                                          Connectivity actual,
                                          boolean passed,
                                          int index) {
        return ValidationItem.builder()
                .itemId("validation-test-" + safeId(testResult.getTestId(), String.valueOf(index)))
                .name("Execution test " + valueOrFallback(testResult.getTestId(), String.valueOf(index)))
                .type("EXECUTION_TEST")
                .expected(valueOrFallback(testResult.getExpectedResult(), "KNOWN_RESULT"))
                .actual(actual.name())
                .passed(passed)
                .severity(passed ? "LOW" : "HIGH")
                .relatedIntentRelationId(first(testResult.getTraceRefs() == null
                        ? null : testResult.getTraceRefs().getIntentRelationIds()))
                .relatedPlanElementIds(copy(testResult.getTraceRefs() == null
                        ? null : testResult.getTraceRefs().getPlanElementIds()))
                .relatedTestId(testResult.getTestId())
                .evidenceIds(List.of(evidence.getEvidenceId()))
                .message("Validation derived from execution test evidence.")
                .build();
    }

    private ValidationEvidence evidenceFor(TestResult result, Connectivity actual) {
        String raw = "status=" + result.getStatus()
                + ", expected=" + valueOrFallback(result.getExpectedResult(), "n/a")
                + ", actual=" + valueOrFallback(summarize(firstNonBlank(result.getActualResult(), result.getLogsSummary())), "n/a");
        return ValidationEvidence.builder()
                .evidenceId("evidence-" + safeId(result.getTestId(), "test"))
                .evidenceType("TEST_RESULT")
                .source("EXECUTION_REPORT")
                .rawValue(raw)
                .normalizedValue(actual.name())
                .relatedTestId(result.getTestId())
                .relatedRuntimeObjectId(firstNonBlank(result.getSourceNode(), result.getTargetNode()))
                .build();
    }

    private Connectivity expectedConnectivity(String action, String type) {
        String normalized = normalize(action + " " + type);
        if (normalized.contains("ALLOW") || normalized.contains("PERMIT") || normalized.contains("ACCEPT")) {
            return Connectivity.REACHABLE;
        }
        if (normalized.contains("DENY")
                || normalized.contains("DROP")
                || normalized.contains("REJECT")
                || normalized.contains("BLOCK")
                || normalized.contains("ISOLATION")) {
            return Connectivity.UNREACHABLE;
        }
        return Connectivity.UNKNOWN;
    }

    private Connectivity inferActualConnectivity(TestResult result, Connectivity expected) {
        String actualText = normalize(firstNonBlank(result.getActualResult(), result.getLogsSummary()));
        if (actualText.contains("0%_PACKET_LOSS")
                || actualText.contains("REACHABLE")
                || actualText.contains("SUCCESS")) {
            return Connectivity.REACHABLE;
        }
        if (actualText.contains("100%_PACKET_LOSS")
                || actualText.contains("0_RECEIVED")
                || actualText.contains("DESTINATION_HOST_UNREACHABLE")
                || actualText.contains("UNREACHABLE")
                || actualText.contains("TIMEOUT")) {
            return Connectivity.UNREACHABLE;
        }
        if (result.getStatus() == TestResultStatus.PASSED && expected != Connectivity.UNKNOWN) {
            return expected;
        }
        if (result.getStatus() == TestResultStatus.FAILED && expected == Connectivity.UNREACHABLE) {
            return Connectivity.REACHABLE;
        }
        if (result.getStatus() == TestResultStatus.FAILED && expected == Connectivity.REACHABLE) {
            return Connectivity.UNREACHABLE;
        }
        return Connectivity.UNKNOWN;
    }

    private List<String> relatedConfigBlockIds(ConfigSet configSet, ExpectedRelation relation) {
        Set<String> ids = new LinkedHashSet<>();
        for (DeviceConfig deviceConfig : safeList(configSet == null ? null : configSet.getDeviceConfigs())) {
            for (CommandBlock block : safeList(deviceConfig == null ? null : deviceConfig.getCommandBlocks())) {
                TraceRefs refs = block.getTraceRefs();
                if (intersects(refs == null ? null : refs.getIntentRelationIds(), relation.traceRefs().getIntentRelationIds())
                        || intersects(refs == null ? null : refs.getPlanElementIds(), relation.traceRefs().getPlanElementIds())) {
                    add(ids, block.getBlockId());
                }
            }
        }
        return new ArrayList<>(ids);
    }

    private void mergeReportRefs(TraceRefs refs, ValidationItem item, TestResult result) {
        add(refs.getIntentRelationIds(), item.getRelatedIntentRelationId());
        addAll(refs.getPlanElementIds(), item.getRelatedPlanElementIds());
        addAll(refs.getConfigBlockIds(), item.getRelatedConfigBlockIds());
        add(refs.getTestIds(), item.getRelatedTestId());
        add(refs.getValidationItemIds(), item.getItemId());
        if (result != null && result.getTraceRefs() != null) {
            mergeInto(refs, result.getTraceRefs());
        }
    }

    private ValidationStatus overallStatus(int failedItems, int missingEvidence) {
        if (failedItems > 0) {
            return ValidationStatus.FAILED;
        }
        if (missingEvidence > 0) {
            return ValidationStatus.PARTIAL;
        }
        return ValidationStatus.PASSED;
    }

    private String summary(ValidationStatus status, int itemCount, int failedItems, int missingEvidence) {
        return "Execution evidence validation completed with status=" + status
                + ", items=" + itemCount
                + ", failedItems=" + failedItems
                + ", missingEvidence=" + missingEvidence + ".";
    }

    private List<String> suggestions(ValidationStatus status) {
        if (status == ValidationStatus.PASSED) {
            return List.of("No repair action is required based on current execution evidence.");
        }
        return List.of(
                "进入修复阶段检查隔离策略生成与下发。",
                "检查 deny 关系对应的执行证据和配置块追溯。");
    }

    private <T> T readRequired(String json, Class<T> type, String label) {
        if (json == null || json.isBlank()) {
            throw new BusinessException(ErrorCode.AGENT_OUTPUT_INVALID, label + " JSON is required for verification");
        }
        return readOptional(json, type, label);
    }

    private <T> T readOptional(String json, Class<T> type, String label) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        }
        catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.AGENT_PARSE_FAILED,
                    "Execution evidence validation cannot parse " + label + " JSON", ex);
        }
    }

    private int failedTestCount(ExecutionReport report) {
        int count = 0;
        for (TestResult result : safeList(report.getTestResults())) {
            if (result.getStatus() == TestResultStatus.FAILED) {
                count++;
            }
        }
        return count;
    }

    private void mergeInto(TraceRefs target, TraceRefs source) {
        if (target == null || source == null) {
            return;
        }
        addAll(target.getIntentNodeIds(), source.getIntentNodeIds());
        addAll(target.getIntentRelationIds(), source.getIntentRelationIds());
        addAll(target.getPlanElementIds(), source.getPlanElementIds());
        addAll(target.getConfigBlockIds(), source.getConfigBlockIds());
        addAll(target.getTestIds(), source.getTestIds());
        addAll(target.getValidationItemIds(), source.getValidationItemIds());
    }

    private boolean intersects(List<String> left, List<String> right) {
        for (String value : safeList(left)) {
            if (contains(right, value)) {
                return true;
            }
        }
        return false;
    }

    private boolean contains(List<String> values, String expected) {
        if (expected == null || expected.isBlank()) {
            return false;
        }
        return safeList(values).stream().anyMatch(value -> expected.equalsIgnoreCase(value));
    }

    private void addAll(List<String> target, List<String> values) {
        for (String value : safeList(values)) {
            add(target, value);
        }
    }

    private void add(List<String> target, String value) {
        if (target != null && value != null && !value.isBlank() && !target.contains(value)) {
            target.add(value);
        }
    }

    private void add(Set<String> target, String value) {
        if (value != null && !value.isBlank()) {
            target.add(value);
        }
    }

    private List<String> copy(List<String> values) {
        return new ArrayList<>(safeList(values));
    }

    private String first(List<String> values) {
        return safeList(values).stream().findFirst().orElse(null);
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values.stream().filter(Objects::nonNull).toList();
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Integer valueOrFallback(Integer value, Integer fallback) {
        return value == null ? fallback : value;
    }

    private String safeId(String value, String fallback) {
        return valueOrFallback(value, fallback).replaceAll("[^A-Za-z0-9_.-]", "-");
    }

    private String compactKey(String value) {
        return normalize(value).replace("NODE_", "").replace("ZONE_", "").replace("HOST_", "");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    private String summarize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 160 ? normalized : normalized.substring(0, 160) + "...";
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private enum Connectivity {
        REACHABLE,
        UNREACHABLE,
        UNKNOWN
    }

    private record ExpectedRelation(String id, String source, String target, Connectivity expectedConnectivity, TraceRefs traceRefs) {
    }
}
