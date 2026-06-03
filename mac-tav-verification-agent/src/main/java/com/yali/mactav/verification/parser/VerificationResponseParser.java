package com.yali.mactav.verification.parser;

import com.yali.mactav.agent.core.context.AgentRunContext;
import com.yali.mactav.agent.core.parser.AgentResponseParser;
import com.yali.mactav.model.enums.StageStatus;
import com.yali.mactav.model.enums.ValidationStatus;
import com.yali.mactav.model.verification.ValidationEvidence;
import com.yali.mactav.model.verification.ValidationItem;
import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.model.workspace.TraceRefs;
import com.yali.mactav.verification.schema.VerificationResponseSchema;
import com.yali.mactav.verification.schema.VerificationResponseSchema.ValidationEvidenceSchema;
import com.yali.mactav.verification.schema.VerificationResponseSchema.ValidationItemSchema;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Converts VerificationResponseSchema into the shared ValidationReport DTO.
 */
public class VerificationResponseParser implements AgentResponseParser<VerificationResponseSchema, ValidationReport> {

    @Override
    public ValidationReport parse(VerificationResponseSchema schema, AgentRunContext context) {
        VerificationResponseSchema safeSchema = schema == null ? new VerificationResponseSchema() : schema;
        List<ValidationItem> items = mapItems(safeSchema.getItems());
        List<ValidationEvidence> evidences = mapEvidences(safeSchema.getEvidences());
        LocalDateTime now = LocalDateTime.now();
        Integer version = context == null ? null : context.getVersion();
        return ValidationReport.builder()
                .validationId("validation-" + valueOrFallback(context == null ? null : context.getTaskId(), "unknown")
                        + "-v" + (version == null ? "0" : version))
                .taskId(context == null ? null : context.getTaskId())
                .validationVersion(version)
                .overallStatus(safeSchema.getOverallStatus() == null
                        ? inferOverallStatus(items)
                        : safeSchema.getOverallStatus())
                .summary(safeSchema.getSummary())
                .items(items)
                .evidences(evidences)
                .suggestions(copyStrings(safeSchema.getSuggestions()))
                .traceRefs(traceRefs(items))
                .stageStatus(StageStatus.SUCCESS)
                .createTime(now)
                .updateTime(now)
                .build();
    }

    private List<ValidationItem> mapItems(List<ValidationItemSchema> schemas) {
        List<ValidationItem> items = new ArrayList<>();
        for (ValidationItemSchema schema : safeList(schemas)) {
            if (schema == null) {
                continue;
            }
            items.add(ValidationItem.builder()
                    .itemId(schema.getItemId())
                    .name(schema.getName())
                    .type(normalizeToken(schema.getType()))
                    .expected(schema.getExpected())
                    .actual(schema.getActual())
                    .passed(schema.getPassed())
                    .severity(normalizeToken(schema.getSeverity()))
                    .relatedIntentRelationId(schema.getRelatedIntentRelationId())
                    .relatedPlanElementIds(copyStrings(schema.getRelatedPlanElementIds()))
                    .relatedConfigBlockIds(copyStrings(schema.getRelatedConfigBlockIds()))
                    .relatedTestId(schema.getRelatedTestId())
                    .evidenceIds(copyStrings(schema.getEvidenceIds()))
                    .message(schema.getMessage())
                    .build());
        }
        return items;
    }

    private List<ValidationEvidence> mapEvidences(List<ValidationEvidenceSchema> schemas) {
        List<ValidationEvidence> evidences = new ArrayList<>();
        for (ValidationEvidenceSchema schema : safeList(schemas)) {
            if (schema == null) {
                continue;
            }
            evidences.add(ValidationEvidence.builder()
                    .evidenceId(schema.getEvidenceId())
                    .evidenceType(normalizeToken(schema.getEvidenceType()))
                    .source(schema.getSource())
                    .rawValue(schema.getRawValue())
                    .normalizedValue(schema.getNormalizedValue())
                    .metadata(schema.getMetadata() == null ? new HashMap<>() : new HashMap<>(schema.getMetadata()))
                    .relatedTestId(schema.getRelatedTestId())
                    .relatedRuntimeObjectId(schema.getRelatedRuntimeObjectId())
                    .build());
        }
        return evidences;
    }

    private ValidationStatus inferOverallStatus(List<ValidationItem> items) {
        if (items == null || items.isEmpty()) {
            return ValidationStatus.UNKNOWN;
        }
        long passed = items.stream().filter(item -> Boolean.TRUE.equals(item.getPassed())).count();
        long failed = items.stream().filter(item -> Boolean.FALSE.equals(item.getPassed())).count();
        if (failed == 0 && passed == items.size()) {
            return ValidationStatus.PASSED;
        }
        if (failed > 0 && passed > 0) {
            return ValidationStatus.PARTIAL;
        }
        if (failed > 0) {
            return ValidationStatus.FAILED;
        }
        return ValidationStatus.UNKNOWN;
    }

    private TraceRefs traceRefs(List<ValidationItem> items) {
        TraceRefs refs = TraceRefs.builder().build();
        for (ValidationItem item : safeList(items)) {
            add(refs.getValidationItemIds(), item.getItemId());
            add(refs.getIntentRelationIds(), item.getRelatedIntentRelationId());
            addAll(refs.getPlanElementIds(), item.getRelatedPlanElementIds());
            addAll(refs.getConfigBlockIds(), item.getRelatedConfigBlockIds());
            add(refs.getTestIds(), item.getRelatedTestId());
        }
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

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
