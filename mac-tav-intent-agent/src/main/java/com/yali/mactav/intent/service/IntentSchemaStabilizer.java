package com.yali.mactav.intent.service;

import com.yali.mactav.intent.schema.IntentResponseSchema;
import com.yali.mactav.intent.schema.IntentResponseSchema.IntentConstraintSchema;
import com.yali.mactav.intent.schema.IntentResponseSchema.IntentNodeSchema;
import com.yali.mactav.intent.schema.IntentResponseSchema.IntentPreferenceSchema;
import com.yali.mactav.intent.schema.IntentResponseSchema.IntentRelationSchema;
import com.yali.mactav.intent.tool.IntentExtractTool.BusinessObjectHint;
import com.yali.mactav.intent.tool.IntentExtractTool.ConstraintHint;
import com.yali.mactav.intent.tool.IntentExtractTool.IntentExtractionHints;
import com.yali.mactav.intent.tool.IntentExtractTool.PreferenceHint;
import com.yali.mactav.intent.tool.IntentExtractTool.RelationHint;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Normalizes IntentResponseSchema using production intent-extraction hints.
 *
 * <p>The stabilizer only operates at the business-intent layer. It never creates
 * devices, interfaces, VLANs, IP addresses, topology, ACLs, CLI, or
 * configuration commands. The result must still pass IntentResponseParser and
 * IntentOutputValidator.</p>
 */
public class IntentSchemaStabilizer {

    public IntentResponseSchema stabilize(String rawText,
                                          IntentResponseSchema schema,
                                          IntentExtractionHints hints) {
        IntentResponseSchema stabilized = copySchema(schema);
        IntentExtractionHints safeHints = safeHints(hints);

        mergeBusinessObjects(stabilized, safeHints.businessObjects());
        mergeRelations(stabilized, safeHints.relations());
        mergePreferences(stabilized, safeHints.preferences());
        mergeConstraints(stabilized, safeHints.constraints());
        mergeWarnings(stabilized, safeHints.warnings());
        if (isBlank(stabilized.getSummary()) && rawText != null && !rawText.isBlank()) {
            stabilized.setSummary("Business intent normalized from user request.");
        }
        return stabilized;
    }

    private IntentResponseSchema copySchema(IntentResponseSchema schema) {
        if (schema == null) {
            return new IntentResponseSchema();
        }
        return IntentResponseSchema.builder()
                .nodes(new ArrayList<>(safeList(schema.getNodes())))
                .relations(new ArrayList<>(safeList(schema.getRelations())))
                .assumptions(new ArrayList<>(safeList(schema.getAssumptions())))
                .constraints(new ArrayList<>(safeList(schema.getConstraints())))
                .preferences(new ArrayList<>(safeList(schema.getPreferences())))
                .summary(schema.getSummary())
                .warnings(new ArrayList<>(safeList(schema.getWarnings())))
                .build();
    }

    private IntentExtractionHints safeHints(IntentExtractionHints hints) {
        if (hints == null) {
            return new IntentExtractionHints(List.of(), List.of(), List.of(), List.of(), List.of());
        }
        return hints;
    }

    private void mergeBusinessObjects(IntentResponseSchema schema, List<BusinessObjectHint> hints) {
        Map<String, IntentNodeSchema> nodesById = new LinkedHashMap<>();
        for (IntentNodeSchema node : safeList(schema.getNodes())) {
            if (node != null && !isBlank(node.getId())) {
                nodesById.put(node.getId(), node);
            }
        }
        for (BusinessObjectHint hint : safeList(hints)) {
            if (hint == null || isBlank(hint.id())) {
                continue;
            }
            nodesById.putIfAbsent(hint.id(), IntentNodeSchema.builder()
                    .id(hint.id())
                    .name(hint.name())
                    .type(hint.type())
                    .description("Business object extracted from intent hints.")
                    .build());
        }
        schema.setNodes(new ArrayList<>(nodesById.values()));
    }

    private void mergeRelations(IntentResponseSchema schema, List<RelationHint> hints) {
        Map<String, IntentRelationSchema> relationsById = new LinkedHashMap<>();
        for (IntentRelationSchema relation : safeList(schema.getRelations())) {
            if (relation != null && !isBlank(relation.getId())) {
                relationsById.put(relation.getId(), relation);
            }
        }
        for (RelationHint hint : safeList(hints)) {
            if (hint == null || isBlank(hint.id()) || isBlank(hint.source()) || isBlank(hint.target())) {
                continue;
            }
            if (!hasNode(schema, hint.source()) || !hasNode(schema, hint.target())) {
                continue;
            }
            relationsById.putIfAbsent(hint.id(), IntentRelationSchema.builder()
                    .id(hint.id())
                    .type(hint.type())
                    .source(hint.source())
                    .target(hint.target())
                    .action(hint.action())
                    .service("business-application")
                    .priority(1)
                    .explicit(true)
                    .description("Business relation extracted from intent hints.")
                    .constraints(List.of())
                    .build());
        }
        schema.setRelations(new ArrayList<>(relationsById.values()));
    }

    private void mergePreferences(IntentResponseSchema schema, List<PreferenceHint> hints) {
        Map<String, IntentPreferenceSchema> preferencesById = new LinkedHashMap<>();
        for (IntentPreferenceSchema preference : safeList(schema.getPreferences())) {
            if (preference != null && !isBlank(preference.getId())) {
                preferencesById.put(preference.getId(), preference);
            }
        }
        for (PreferenceHint hint : safeList(hints)) {
            if (hint == null || isBlank(hint.id())) {
                continue;
            }
            preferencesById.putIfAbsent(hint.id(), IntentPreferenceSchema.builder()
                    .id(hint.id())
                    .type(hint.type())
                    .value(hint.value())
                    .priority(1)
                    .build());
        }
        schema.setPreferences(new ArrayList<>(preferencesById.values()));
    }

    private void mergeConstraints(IntentResponseSchema schema, List<ConstraintHint> hints) {
        Map<String, IntentConstraintSchema> constraintsById = new LinkedHashMap<>();
        for (IntentConstraintSchema constraint : safeList(schema.getConstraints())) {
            if (constraint != null && !isBlank(constraint.getId())) {
                constraintsById.put(constraint.getId(), constraint);
            }
        }
        for (ConstraintHint hint : safeList(hints)) {
            if (hint == null || isBlank(hint.id())) {
                continue;
            }
            constraintsById.putIfAbsent(hint.id(), IntentConstraintSchema.builder()
                    .id(hint.id())
                    .type(hint.type())
                    .value(hint.value())
                    .description("Business constraint extracted from intent hints.")
                    .build());
        }
        schema.setConstraints(new ArrayList<>(constraintsById.values()));
    }

    private void mergeWarnings(IntentResponseSchema schema, List<String> hints) {
        List<String> warnings = new ArrayList<>(safeList(schema.getWarnings()));
        for (String warning : safeList(hints)) {
            if (!isBlank(warning) && !warnings.contains(warning)) {
                warnings.add(warning);
            }
        }
        schema.setWarnings(warnings);
    }

    private boolean hasNode(IntentResponseSchema schema, String nodeId) {
        for (IntentNodeSchema node : safeList(schema.getNodes())) {
            if (node != null && nodeId.equals(node.getId())) {
                return true;
            }
        }
        return false;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
