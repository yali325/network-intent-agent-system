package com.yali.mactav.healing.validator;

import com.yali.mactav.agent.core.validator.AgentOutputValidator;
import com.yali.mactav.agent.core.validator.ValidationResult;
import com.yali.mactav.model.enums.RepairStatus;
import com.yali.mactav.model.healing.FailureAnalysis;
import com.yali.mactav.model.healing.RepairAction;
import com.yali.mactav.model.healing.RepairPlan;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Validates RepairPlan output before it can leave HealingAgent.
 */
public class HealingOutputValidator implements AgentOutputValidator<RepairPlan> {

    private static final List<String> ALLOWED_ACTION_TYPES = List.of(
            "REPLAN",
            "REGENERATE_CONFIG",
            "PATCH_CONFIG",
            "REEXECUTE",
            "ASK_USER",
            "ROLLBACK"
    );

    private static final List<String> FORBIDDEN_TERMS = List.of(
            "executed",
            "applied",
            "modified workspace",
            "updated workspace",
            "pushed config",
            "deployed config",
            "changed config",
            "issued command",
            "ran command",
            "run command",
            "shell",
            "bash",
            "powershell",
            "cmd.exe",
            "sudo ",
            "sh ",
            "python ",
            "mininet>",
            "ovs-vsctl",
            "ryu-manager",
            "docker ",
            "ssh "
    );

    @Override
    public ValidationResult validate(RepairPlan plan) {
        List<String> messages = new ArrayList<>();
        if (plan == null) {
            messages.add("RepairPlan must not be null");
            return ValidationResult.fail(messages);
        }
        requireNotBlank(messages, "taskId", plan.getTaskId());
        requireNotBlank(messages, "overallRepairStrategy", plan.getOverallRepairStrategy());
        if (plan.getActions() == null || plan.getActions().isEmpty()) {
            messages.add("actions must not be empty");
        }
        if (plan.getFailureAnalysis() == null || plan.getFailureAnalysis().isEmpty()) {
            messages.add("failureAnalysis must not be empty");
        }
        validateFailureAnalysis(messages, plan.getFailureAnalysis());
        validateActions(messages, plan.getActions());
        rejectOverreach(messages, plan);
        return messages.isEmpty() ? ValidationResult.ok() : ValidationResult.fail(messages);
    }

    private void validateFailureAnalysis(List<String> messages, List<FailureAnalysis> analyses) {
        if (analyses == null) {
            return;
        }
        Set<String> analysisIds = new HashSet<>();
        for (FailureAnalysis analysis : analyses) {
            if (analysis == null) {
                messages.add("failureAnalysis item must not be null");
                continue;
            }
            requireNotBlank(messages, "analysisId", analysis.getAnalysisId());
            requireNotBlank(messages, "rootCauseSummary", analysis.getRootCauseSummary());
            if (!isBlank(analysis.getAnalysisId()) && !analysisIds.add(analysis.getAnalysisId())) {
                messages.add("analysisId must be unique: " + analysis.getAnalysisId());
            }
        }
    }

    private void validateActions(List<String> messages, List<RepairAction> actions) {
        if (actions == null) {
            return;
        }
        Set<String> actionIds = new HashSet<>();
        for (RepairAction action : actions) {
            if (action == null) {
                messages.add("repair action must not be null");
                continue;
            }
            requireNotBlank(messages, "actionId", action.getActionId());
            requireNotBlank(messages, "actionType", action.getActionType());
            requireNotBlank(messages, "description", action.getDescription());
            requireNotBlank(messages, "riskLevel", action.getRiskLevel());
            if (action.getStatus() == null) {
                messages.add("action.status must not be null: " + action.getActionId());
            }
            if (RepairStatus.APPLIED.equals(action.getStatus())) {
                messages.add("HealingAgent must not return an already applied action: " + action.getActionId());
            }
            if (!isBlank(action.getActionId()) && !actionIds.add(action.getActionId())) {
                messages.add("actionId must be unique: " + action.getActionId());
            }
            if (!isBlank(action.getActionType()) && !ALLOWED_ACTION_TYPES.contains(action.getActionType())) {
                messages.add("unsupported actionType: " + action.getActionType());
            }
            if (action.getTraceRefs() == null || isEmpty(action.getTraceRefs().getValidationItemIds())) {
                if (isBlank(action.getRelatedFailureAnalysisId())) {
                    messages.add("action must reference failed validation item context or failureAnalysis: "
                            + action.getActionId());
                }
            }
            if ("HIGH".equals(normalize(action.getRiskLevel())) && !Boolean.TRUE.equals(action.getRequiresApproval())) {
                messages.add("high risk action must require approval: " + action.getActionId());
            }
        }
    }

    private void rejectOverreach(List<String> messages, RepairPlan plan) {
        List<String> texts = new ArrayList<>();
        texts.add(plan.getOverallRepairStrategy());
        if (plan.getFailureAnalysis() != null) {
            for (FailureAnalysis analysis : plan.getFailureAnalysis()) {
                if (analysis != null) {
                    texts.add(analysis.getRootCauseSummary());
                }
            }
        }
        if (plan.getActions() != null) {
            for (RepairAction action : plan.getActions()) {
                if (action != null) {
                    texts.add(action.getDescription());
                    texts.add(action.getRiskReason());
                    texts.add(action.getApprovalComment());
                }
            }
        }
        for (String text : texts) {
            if (text == null || text.isBlank()) {
                continue;
            }
            String normalized = text.toLowerCase(Locale.ROOT);
            for (String term : FORBIDDEN_TERMS) {
                if (normalized.contains(term.toLowerCase(Locale.ROOT))) {
                    messages.add("Healing output contains forbidden execution/workspace claim: " + term);
                    return;
                }
            }
        }
    }

    private void requireNotBlank(List<String> messages, String fieldName, String value) {
        if (isBlank(value)) {
            messages.add(fieldName + " must not be blank");
        }
    }

    private boolean isEmpty(List<String> values) {
        return values == null || values.isEmpty();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }
}
