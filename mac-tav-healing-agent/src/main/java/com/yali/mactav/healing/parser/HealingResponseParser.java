package com.yali.mactav.healing.parser;

import com.yali.mactav.agent.core.context.AgentRunContext;
import com.yali.mactav.agent.core.parser.AgentResponseParser;
import com.yali.mactav.healing.schema.HealingResponseSchema;
import com.yali.mactav.healing.schema.HealingResponseSchema.FailureAnalysisSchema;
import com.yali.mactav.healing.schema.HealingResponseSchema.RepairActionSchema;
import com.yali.mactav.model.enums.RepairStatus;
import com.yali.mactav.model.enums.StageStatus;
import com.yali.mactav.model.healing.FailureAnalysis;
import com.yali.mactav.model.healing.RepairAction;
import com.yali.mactav.model.healing.RepairPlan;
import com.yali.mactav.model.workspace.TraceRefs;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts HealingResponseSchema into the shared RepairPlan DTO.
 */
public class HealingResponseParser implements AgentResponseParser<HealingResponseSchema, RepairPlan> {

    @Override
    public RepairPlan parse(HealingResponseSchema schema, AgentRunContext context) {
        HealingResponseSchema safeSchema = schema == null ? new HealingResponseSchema() : schema;
        List<FailureAnalysis> analyses = mapAnalyses(safeSchema.getFailureAnalysis());
        List<RepairAction> actions = mapActions(safeSchema.getActions());
        return RepairPlan.builder()
                .taskId(context == null ? null : context.getTaskId())
                .repairVersion(context == null ? null : context.getVersion())
                .overallRepairStrategy(safeSchema.getOverallRepairStrategy())
                .failureAnalysis(analyses)
                .actions(actions)
                .requiresUserConfirmation(Boolean.TRUE.equals(safeSchema.getRequiresUserConfirmation())
                        || actions.stream().anyMatch(action -> Boolean.TRUE.equals(action.getRequiresApproval())))
                .stageStatus(StageStatus.SUCCESS)
                .createTime(LocalDateTime.now())
                .build();
    }

    private List<FailureAnalysis> mapAnalyses(List<FailureAnalysisSchema> schemas) {
        List<FailureAnalysis> analyses = new ArrayList<>();
        for (FailureAnalysisSchema schema : safeList(schemas)) {
            if (schema == null) {
                continue;
            }
            analyses.add(FailureAnalysis.builder()
                    .analysisId(schema.getAnalysisId())
                    .failureType(normalizeToken(schema.getFailureType()))
                    .rootCauseSummary(schema.getRootCauseSummary())
                    .relatedValidationItemIds(copyStrings(schema.getRelatedValidationItemIds()))
                    .relatedIntentRelationIds(copyStrings(schema.getRelatedIntentRelationIds()))
                    .relatedPlanElementIds(copyStrings(schema.getRelatedPlanElementIds()))
                    .relatedConfigBlockIds(copyStrings(schema.getRelatedConfigBlockIds()))
                    .confidence(schema.getConfidence())
                    .evidenceIds(copyStrings(schema.getEvidenceIds()))
                    .build());
        }
        return analyses;
    }

    private List<RepairAction> mapActions(List<RepairActionSchema> schemas) {
        List<RepairAction> actions = new ArrayList<>();
        for (RepairActionSchema schema : safeList(schemas)) {
            if (schema == null) {
                continue;
            }
            actions.add(RepairAction.builder()
                    .actionId(schema.getActionId())
                    .actionType(normalizeToken(schema.getActionType()))
                    .targetStage(schema.getTargetStage())
                    .description(schema.getDescription())
                    .relatedFailureAnalysisId(schema.getRelatedFailureAnalysisId())
                    .inputArtifactIds(copyStrings(schema.getInputArtifactIds()))
                    .expectedOutputArtifactType(schema.getExpectedOutputArtifactType())
                    .riskLevel(normalizeToken(schema.getRiskLevel()))
                    .riskReason(schema.getRiskReason())
                    .requiresApproval(schema.getRequiresApproval())
                    .traceRefs(traceRefs(schema))
                    .status(RepairStatus.PROPOSED)
                    .build());
        }
        return actions;
    }

    private TraceRefs traceRefs(RepairActionSchema schema) {
        TraceRefs refs = TraceRefs.builder().build();
        add(refs.getRepairActionIds(), schema.getActionId());
        addAll(refs.getValidationItemIds(), schema.getRelatedValidationItemIds());
        addAll(refs.getIntentRelationIds(), schema.getRelatedIntentRelationIds());
        addAll(refs.getPlanElementIds(), schema.getRelatedPlanElementIds());
        addAll(refs.getConfigBlockIds(), schema.getRelatedConfigBlockIds());
        return refs;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private List<String> copyStrings(List<String> values) {
        List<String> copy = new ArrayList<>();
        addAll(copy, values);
        return copy;
    }

    private void addAll(List<String> target, List<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            add(target, value);
        }
    }

    private void add(List<String> target, String value) {
        if (value != null && !value.isBlank() && !target.contains(value)) {
            target.add(value);
        }
    }

    private String normalizeToken(String value) {
        return value == null ? null : value.trim().replace('-', '_').replace(' ', '_').toUpperCase();
    }
}
