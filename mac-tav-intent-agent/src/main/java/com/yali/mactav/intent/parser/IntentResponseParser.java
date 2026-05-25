package com.yali.mactav.intent.parser;

import com.yali.mactav.agent.core.context.AgentRunContext;
import com.yali.mactav.agent.core.parser.AgentResponseParser;
import com.yali.mactav.intent.schema.IntentResponseSchema;
import com.yali.mactav.intent.schema.IntentResponseSchema.AssumptionSchema;
import com.yali.mactav.intent.schema.IntentResponseSchema.IntentConstraintSchema;
import com.yali.mactav.intent.schema.IntentResponseSchema.IntentNodeSchema;
import com.yali.mactav.intent.schema.IntentResponseSchema.IntentPreferenceSchema;
import com.yali.mactav.intent.schema.IntentResponseSchema.IntentRelationSchema;
import com.yali.mactav.model.enums.StageStatus;
import com.yali.mactav.model.intent.Assumption;
import com.yali.mactav.model.intent.IntentConstraint;
import com.yali.mactav.model.intent.IntentNode;
import com.yali.mactav.model.intent.IntentPreference;
import com.yali.mactav.model.intent.IntentRelation;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.intent.SemanticIntentGraph;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts IntentResponseSchema into the shared NetworkIntent DTO.
 *
 * <p>This parser belongs to mac-tav-intent-agent. It fills task metadata from
 * AgentRunContext and normalizes null lists, but it must not call models, write
 * NetworkWorkspace, or perform orchestration.</p>
 */
public class IntentResponseParser implements AgentResponseParser<IntentResponseSchema, NetworkIntent> {

    @Override
    public NetworkIntent parse(IntentResponseSchema schema, AgentRunContext context) {
        IntentResponseSchema safeSchema = schema == null ? new IntentResponseSchema() : schema;

        SemanticIntentGraph graph = SemanticIntentGraph.builder()
                .nodes(mapNodes(safeSchema.getNodes()))
                .relations(mapRelations(safeSchema.getRelations()))
                .build();

        return NetworkIntent.builder()
                .taskId(context == null ? null : context.getTaskId())
                .intentVersion(context == null ? null : context.getVersion())
                .rawText(context == null ? null : context.getUserInput())
                .semanticIntentGraph(graph)
                .assumptions(mapAssumptions(safeSchema.getAssumptions()))
                .constraints(mapConstraints(safeSchema.getConstraints()))
                .preferences(mapPreferences(safeSchema.getPreferences()))
                .stageStatus(StageStatus.SUCCESS)
                .traceId(context == null ? null : context.getTraceId())
                .createTime(LocalDateTime.now())
                .build();
    }

    private List<IntentNode> mapNodes(List<IntentNodeSchema> schemas) {
        List<IntentNode> nodes = new ArrayList<>();
        for (IntentNodeSchema schema : safeList(schemas)) {
            if (schema == null) {
                continue;
            }
            nodes.add(IntentNode.builder()
                    .id(schema.getId())
                    .name(schema.getName())
                    .type(schema.getType())
                    .description(schema.getDescription())
                    .attributes(copyAttributes(schema.getAttributes()))
                    .build());
        }
        return nodes;
    }

    private List<IntentRelation> mapRelations(List<IntentRelationSchema> schemas) {
        List<IntentRelation> relations = new ArrayList<>();
        for (IntentRelationSchema schema : safeList(schemas)) {
            if (schema == null) {
                continue;
            }
            relations.add(IntentRelation.builder()
                    .id(schema.getId())
                    .type(schema.getType())
                    .source(schema.getSource())
                    .target(schema.getTarget())
                    .action(schema.getAction())
                    .service(schema.getService())
                    .priority(schema.getPriority())
                    .explicit(schema.getExplicit())
                    .description(schema.getDescription())
                    .constraints(mapConstraints(schema.getConstraints()))
                    .build());
        }
        return relations;
    }

    private List<Assumption> mapAssumptions(List<AssumptionSchema> schemas) {
        List<Assumption> assumptions = new ArrayList<>();
        for (AssumptionSchema schema : safeList(schemas)) {
            if (schema == null) {
                continue;
            }
            assumptions.add(Assumption.builder()
                    .id(schema.getId())
                    .field(schema.getField())
                    .value(schema.getValue())
                    .reason(schema.getReason())
                    .confidence(schema.getConfidence())
                    .build());
        }
        return assumptions;
    }

    private List<IntentConstraint> mapConstraints(List<IntentConstraintSchema> schemas) {
        List<IntentConstraint> constraints = new ArrayList<>();
        for (IntentConstraintSchema schema : safeList(schemas)) {
            if (schema == null) {
                continue;
            }
            constraints.add(IntentConstraint.builder()
                    .id(schema.getId())
                    .type(schema.getType())
                    .value(schema.getValue())
                    .description(schema.getDescription())
                    .build());
        }
        return constraints;
    }

    private List<IntentPreference> mapPreferences(List<IntentPreferenceSchema> schemas) {
        List<IntentPreference> preferences = new ArrayList<>();
        for (IntentPreferenceSchema schema : safeList(schemas)) {
            if (schema == null) {
                continue;
            }
            preferences.add(IntentPreference.builder()
                    .id(schema.getId())
                    .type(schema.getType())
                    .value(schema.getValue())
                    .priority(schema.getPriority())
                    .build());
        }
        return preferences;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private Map<String, Object> copyAttributes(Map<String, Object> attributes) {
        return attributes == null ? new HashMap<>() : new HashMap<>(attributes);
    }
}
