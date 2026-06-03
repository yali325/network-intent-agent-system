package com.yali.mactav.healing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.agent.core.agent.AgentUtils;
import com.yali.mactav.agent.core.context.AgentRunContext;
import com.yali.mactav.agent.core.parser.AgentResponseParser;
import com.yali.mactav.agent.core.validator.AgentOutputValidator;
import com.yali.mactav.healing.parser.HealingResponseParser;
import com.yali.mactav.healing.request.HealingAgentRequest;
import com.yali.mactav.healing.schema.HealingResponseSchema;
import com.yali.mactav.healing.tool.HealingDiagnosisTool;
import com.yali.mactav.healing.tool.HealingDiagnosisTool.AffectedScope;
import com.yali.mactav.healing.tool.HealingDiagnosisTool.FailureClassification;
import com.yali.mactav.healing.tool.HealingDiagnosisTool.RepairActionSuggestion;
import com.yali.mactav.healing.validator.HealingOutputValidator;
import com.yali.mactav.model.enums.RepairStatus;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.healing.FailureAnalysis;
import com.yali.mactav.model.healing.RepairAction;
import com.yali.mactav.model.healing.RepairPlan;
import com.yali.mactav.model.verification.ValidationItem;
import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.model.workspace.TraceRefs;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Default HealingService implementation that runs Parser -> Validator.
 */
public class HealingServiceImpl implements HealingService {

    private final AgentResponseParser<HealingResponseSchema, RepairPlan> parser;

    private final AgentOutputValidator<RepairPlan> validator;

    private final HealingDiagnosisTool diagnosisTool;

    private final ObjectMapper objectMapper;

    public HealingServiceImpl() {
        this(new HealingResponseParser(), new HealingOutputValidator(), new HealingDiagnosisTool(), new ObjectMapper());
    }

    public HealingServiceImpl(AgentResponseParser<HealingResponseSchema, RepairPlan> parser,
                              AgentOutputValidator<RepairPlan> validator,
                              HealingDiagnosisTool diagnosisTool,
                              ObjectMapper objectMapper) {
        this.parser = parser;
        this.validator = validator;
        this.diagnosisTool = Objects.requireNonNull(diagnosisTool, "diagnosisTool must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public RepairPlan parse(HealingResponseSchema schema, HealingAgentRequest request) {
        RepairPlan plan = parser.parse(schema, toContext(request));
        enrichPlan(plan, request);
        normalizePlan(plan, request);
        return validator.validateAndReturn(plan);
    }

    private AgentRunContext toContext(HealingAgentRequest request) {
        return AgentRunContext.builder()
                .taskId(request == null ? null : request.getTaskId())
                .stage(WorkflowStage.HEALING)
                .version(request == null ? null : request.getRepairVersion())
                .traceId(request == null ? null : request.getTraceId())
                .userInput(request == null ? null : request.getRawText())
                .workspaceSnapshot(request == null ? null : request.getWorkspaceSnapshot())
                .createdBy(request == null ? null : request.getCreatedBy())
                .build();
    }

    private void normalizePlan(RepairPlan plan, HealingAgentRequest request) {
        if (plan == null || request == null) {
            return;
        }
        plan.setValidationVersion(request.getValidationVersion());
        if (plan.getRepairVersion() == null) {
            plan.setRepairVersion(request.getRepairVersion());
        }
        if (plan.getTaskId() == null || plan.getTaskId().isBlank()) {
            plan.setTaskId(request.getTaskId());
        }
        if (plan.getStageStatus() == null) {
            plan.setStageStatus(com.yali.mactav.model.enums.StageStatus.SUCCESS);
        }
        if (plan.getCreateTime() == null) {
            plan.setCreateTime(java.time.LocalDateTime.now());
        }
    }

    private void enrichPlan(RepairPlan plan, HealingAgentRequest request) {
        if (plan == null) {
            return;
        }
        ValidationReport report = readValidationReport(request);
        List<ValidationItem> failedItems = failedItems(report, request);
        if ((plan.getFailureAnalysis() == null || plan.getFailureAnalysis().isEmpty()) && !failedItems.isEmpty()) {
            plan.setFailureAnalysis(buildFailureAnalysis(failedItems));
        }
        if ((plan.getActions() == null || plan.getActions().isEmpty()) && !failedItems.isEmpty()) {
            plan.setActions(buildActions(failedItems, safeAnalyses(plan.getFailureAnalysis())));
        }
        normalizeActions(plan);
        if (plan.getOverallRepairStrategy() == null || plan.getOverallRepairStrategy().isBlank()) {
            plan.setOverallRepairStrategy(defaultStrategy(plan));
        }
        if (plan.getRequiresUserConfirmation() == null) {
            plan.setRequiresUserConfirmation(plan.getActions() != null
                    && plan.getActions().stream().anyMatch(action -> Boolean.TRUE.equals(action.getRequiresApproval())));
        }
    }

    private ValidationReport readValidationReport(HealingAgentRequest request) {
        if (request == null || request.getValidationReportJson() == null || request.getValidationReportJson().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(request.getValidationReportJson(), ValidationReport.class);
        }
        catch (JsonProcessingException ex) {
            throw AgentUtils.parseFailed("Failed to parse HealingAgent validationReportJson", ex);
        }
    }

    private List<ValidationItem> failedItems(ValidationReport report, HealingAgentRequest request) {
        List<ValidationItem> items = new ArrayList<>();
        if (report == null || report.getItems() == null) {
            return items;
        }
        Set<String> requestedIds = new HashSet<>();
        if (request != null && request.getFailedValidationItemIds() != null) {
            requestedIds.addAll(request.getFailedValidationItemIds());
        }
        for (ValidationItem item : report.getItems()) {
            if (item == null) {
                continue;
            }
            boolean requested = requestedIds.isEmpty() || requestedIds.contains(item.getItemId());
            if (requested && Boolean.FALSE.equals(item.getPassed())) {
                items.add(item);
            }
        }
        return items;
    }

    private List<FailureAnalysis> buildFailureAnalysis(List<ValidationItem> failedItems) {
        List<FailureAnalysis> analyses = new ArrayList<>();
        int index = 1;
        for (ValidationItem item : failedItems) {
            FailureClassification classification = diagnosisTool.classifyValidationFailure(
                    item.getType(),
                    item.getExpected(),
                    item.getActual(),
                    item.getSeverity(),
                    item.getMessage());
            AffectedScope scope = scope(item);
            analyses.add(FailureAnalysis.builder()
                    .analysisId("analysis-" + item.getItemId())
                    .failureType(classification.failureType())
                    .rootCauseSummary(classification.rootCauseSummary())
                    .relatedValidationItemIds(nonEmpty(scope.validationItemIds(), List.of(item.getItemId())))
                    .relatedIntentRelationIds(scope.intentRelationIds())
                    .relatedPlanElementIds(scope.planElementIds())
                    .relatedConfigBlockIds(scope.configBlockIds())
                    .confidence(classification.confidence())
                    .evidenceIds(scope.evidenceIds())
                    .build());
            index++;
        }
        return analyses;
    }

    private List<RepairAction> buildActions(List<ValidationItem> failedItems, List<FailureAnalysis> analyses) {
        List<RepairAction> actions = new ArrayList<>();
        int index = 1;
        for (ValidationItem item : failedItems) {
            FailureAnalysis analysis = findAnalysis(analyses, item.getItemId());
            FailureClassification classification = diagnosisTool.classifyValidationFailure(
                    item.getType(),
                    item.getExpected(),
                    item.getActual(),
                    item.getSeverity(),
                    item.getMessage());
            RepairActionSuggestion suggestion = diagnosisTool.suggestRepairAction(
                    analysis == null ? classification.failureType() : analysis.getFailureType(),
                    item.getRelatedPlanElementIds(),
                    item.getRelatedConfigBlockIds(),
                    classification.riskLevel(),
                    classification.confidence() != null && classification.confidence() < 0.55);
            actions.add(RepairAction.builder()
                    .actionId("repair-action-" + index)
                    .actionType(suggestion.actionType())
                    .targetStage(suggestion.targetStage())
                    .description(suggestion.description())
                    .relatedFailureAnalysisId(analysis == null ? null : analysis.getAnalysisId())
                    .expectedOutputArtifactType(suggestion.expectedOutputArtifactType())
                    .riskLevel(suggestion.riskLevel())
                    .riskReason("Risk is based on validation severity and affected trace scope.")
                    .requiresApproval(Boolean.TRUE.equals(suggestion.requiresApproval()) || "HIGH".equals(suggestion.riskLevel()))
                    .traceRefs(traceRefs(item, "repair-action-" + index))
                    .status(RepairStatus.PROPOSED)
                    .build());
            index++;
        }
        return actions;
    }

    private AffectedScope scope(ValidationItem item) {
        return diagnosisTool.extractAffectedScope(
                item.getItemId(),
                item.getRelatedIntentRelationId(),
                item.getRelatedPlanElementIds(),
                item.getRelatedConfigBlockIds(),
                item.getRelatedTestId(),
                item.getEvidenceIds());
    }

    private TraceRefs traceRefs(ValidationItem item, String actionId) {
        AffectedScope scope = scope(item);
        TraceRefs refs = TraceRefs.builder().build();
        addAll(refs.getValidationItemIds(), scope.validationItemIds());
        addAll(refs.getIntentRelationIds(), scope.intentRelationIds());
        addAll(refs.getPlanElementIds(), scope.planElementIds());
        addAll(refs.getConfigBlockIds(), scope.configBlockIds());
        addAll(refs.getTestIds(), scope.testIds());
        add(refs.getRepairActionIds(), actionId);
        return refs;
    }

    private void normalizeActions(RepairPlan plan) {
        if (plan.getActions() == null) {
            plan.setActions(new ArrayList<>());
            return;
        }
        int index = 1;
        for (RepairAction action : plan.getActions()) {
            if (action == null) {
                continue;
            }
            if (action.getActionId() == null || action.getActionId().isBlank()) {
                action.setActionId("repair-action-" + index);
            }
            if (action.getStatus() == null) {
                action.setStatus(RepairStatus.PROPOSED);
            }
            if (action.getRiskLevel() != null && "HIGH".equals(action.getRiskLevel().trim().toUpperCase(java.util.Locale.ROOT))) {
                action.setRequiresApproval(true);
            }
            if (action.getTraceRefs() == null) {
                action.setTraceRefs(TraceRefs.builder().build());
            }
            add(action.getTraceRefs().getRepairActionIds(), action.getActionId());
            index++;
        }
    }

    private FailureAnalysis findAnalysis(List<FailureAnalysis> analyses, String validationItemId) {
        for (FailureAnalysis analysis : analyses) {
            if (analysis != null
                    && analysis.getRelatedValidationItemIds() != null
                    && analysis.getRelatedValidationItemIds().contains(validationItemId)) {
                return analysis;
            }
        }
        return analyses.isEmpty() ? null : analyses.get(0);
    }

    private List<FailureAnalysis> safeAnalyses(List<FailureAnalysis> analyses) {
        return analyses == null ? List.of() : analyses;
    }

    private String defaultStrategy(RepairPlan plan) {
        int actionCount = plan.getActions() == null ? 0 : plan.getActions().size();
        return "Produce an Orchestrator-controlled repair plan with " + actionCount
                + " proposed action(s); no workspace mutation or command execution is performed by HealingAgent.";
    }

    private List<String> nonEmpty(List<String> values, List<String> fallback) {
        return values == null || values.isEmpty() ? fallback : values;
    }

    private void addAll(List<String> target, List<String> values) {
        if (target == null || values == null) {
            return;
        }
        for (String value : values) {
            add(target, value);
        }
    }

    private void add(List<String> target, String value) {
        if (target != null && value != null && !value.isBlank() && !target.contains(value)) {
            target.add(value);
        }
    }
}
