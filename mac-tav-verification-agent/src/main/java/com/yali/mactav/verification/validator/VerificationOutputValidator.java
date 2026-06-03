package com.yali.mactav.verification.validator;

import com.yali.mactav.agent.core.validator.AgentOutputValidator;
import com.yali.mactav.agent.core.validator.ValidationResult;
import com.yali.mactav.model.verification.ValidationEvidence;
import com.yali.mactav.model.verification.ValidationItem;
import com.yali.mactav.model.verification.ValidationReport;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Validates ValidationReport output before it can leave VerificationAgent.
 */
public class VerificationOutputValidator implements AgentOutputValidator<ValidationReport> {

    private static final List<String> OVERREACH_TERMS = List.of(
            "apply config",
            "push config",
            "execute command",
            "rerun test",
            "run mininet",
            "run ryu",
            "repairplan",
            "repair plan",
            "shell",
            "powershell",
            "bash",
            "cmd.exe",
            "直接修改",
            "执行修复",
            "重新执行测试"
    );

    @Override
    public ValidationResult validate(ValidationReport report) {
        List<String> messages = new ArrayList<>();
        if (report == null) {
            messages.add("ValidationReport must not be null");
            return ValidationResult.fail(messages);
        }

        requireNotBlank(messages, "validationId", report.getValidationId());
        requireNotBlank(messages, "taskId", report.getTaskId());
        if (report.getOverallStatus() == null) {
            messages.add("overallStatus must not be null");
        }
        if (report.getItems() == null || report.getItems().isEmpty()) {
            messages.add("items must not be empty");
        }

        validateItems(messages, report.getItems());
        validateEvidences(messages, report.getEvidences());
        rejectOverreach(messages, report);
        return messages.isEmpty() ? ValidationResult.ok() : ValidationResult.fail(messages);
    }

    private void validateItems(List<String> messages, List<ValidationItem> items) {
        if (items == null) {
            return;
        }
        Set<String> itemIds = new HashSet<>();
        for (ValidationItem item : items) {
            if (item == null) {
                messages.add("validation item must not be null");
                continue;
            }
            requireNotBlank(messages, "itemId", item.getItemId());
            requireNotBlank(messages, "item.expected", item.getExpected());
            requireNotBlank(messages, "item.actual", item.getActual());
            if (item.getPassed() == null) {
                messages.add("item.passed must not be null for item: " + item.getItemId());
            }
            if (!isBlank(item.getItemId()) && !itemIds.add(item.getItemId())) {
                messages.add("itemId must be unique: " + item.getItemId());
            }
            if (isBlank(item.getRelatedIntentRelationId())
                    && isEmpty(item.getRelatedPlanElementIds())
                    && isEmpty(item.getRelatedConfigBlockIds())
                    && isBlank(item.getRelatedTestId())) {
                messages.add("item must reference at least one intent/plan/config/test id: " + item.getItemId());
            }
        }
    }

    private void validateEvidences(List<String> messages, List<ValidationEvidence> evidences) {
        if (evidences == null) {
            return;
        }
        Set<String> evidenceIds = new HashSet<>();
        for (ValidationEvidence evidence : evidences) {
            if (evidence == null) {
                messages.add("validation evidence must not be null");
                continue;
            }
            requireNotBlank(messages, "evidenceId", evidence.getEvidenceId());
            requireNotBlank(messages, "evidence.source", evidence.getSource());
            if (!isBlank(evidence.getEvidenceId()) && !evidenceIds.add(evidence.getEvidenceId())) {
                messages.add("evidenceId must be unique: " + evidence.getEvidenceId());
            }
        }
    }

    private void rejectOverreach(List<String> messages, ValidationReport report) {
        List<String> texts = new ArrayList<>();
        texts.add(report.getSummary());
        if (report.getSuggestions() != null) {
            texts.addAll(report.getSuggestions());
        }
        if (report.getItems() != null) {
            for (ValidationItem item : report.getItems()) {
                if (item != null) {
                    texts.add(item.getMessage());
                    texts.add(item.getActual());
                }
            }
        }
        for (String text : texts) {
            if (text == null || text.isBlank()) {
                continue;
            }
            String normalized = text.toLowerCase(Locale.ROOT);
            for (String term : OVERREACH_TERMS) {
                if (normalized.contains(term.toLowerCase(Locale.ROOT))) {
                    messages.add("Verification output contains overreach instruction: " + term);
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
}
