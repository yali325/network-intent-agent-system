package com.yali.mactav.intent.validator;

import com.yali.mactav.agent.core.validator.AgentOutputValidator;
import com.yali.mactav.agent.core.validator.ValidationResult;
import com.yali.mactav.model.intent.Assumption;
import com.yali.mactav.model.intent.IntentConstraint;
import com.yali.mactav.model.intent.IntentNode;
import com.yali.mactav.model.intent.IntentPreference;
import com.yali.mactav.model.intent.IntentRelation;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.intent.SemanticIntentGraph;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates NetworkIntent output before it can leave mac-tav-intent-agent.
 *
 * <p>The validator enforces the intent-stage boundary only. It must not call a
 * model, repair output, write NetworkWorkspace, or decide orchestration flow.</p>
 */
public class IntentOutputValidator implements AgentOutputValidator<NetworkIntent> {

    private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");

    private static final List<String> BOUNDARY_KEYWORDS = List.of(
            "vlan",
            "interface",
            "cli",
            "topology",
            "router",
            "switch",
            "firewall",
            "device",
            "acl",
            "access-list",
            "route-map",
            "ospf",
            "bgp",
            "static route",
            "ip address",
            "gigabitethernet",
            "fastethernet",
            "configure terminal",
            "show running-config"
    );

    @Override
    public ValidationResult validate(NetworkIntent intent) {
        List<String> messages = new ArrayList<>();
        if (intent == null) {
            messages.add("NetworkIntent must not be null");
            return ValidationResult.fail(messages);
        }

        requireNotBlank(messages, "taskId", intent.getTaskId());
        requireNotBlank(messages, "rawText", intent.getRawText());

        SemanticIntentGraph graph = intent.getSemanticIntentGraph();
        if (graph == null) {
            messages.add("semanticIntentGraph must not be null");
            return ValidationResult.fail(messages);
        }

        List<IntentNode> nodes = graph.getNodes();
        List<IntentRelation> relations = graph.getRelations();
        if (nodes == null || nodes.isEmpty()) {
            messages.add("semanticIntentGraph.nodes must not be empty");
        }
        if (relations == null || relations.isEmpty()) {
            messages.add("semanticIntentGraph.relations must not be empty");
        }

        Set<String> nodeIds = validateNodes(messages, nodes);
        validateRelations(messages, relations, nodeIds);
        validateIntentBoundary(messages, intent);

        return messages.isEmpty() ? ValidationResult.ok() : ValidationResult.fail(messages);
    }

    private Set<String> validateNodes(List<String> messages, List<IntentNode> nodes) {
        Set<String> nodeIds = new HashSet<>();
        if (nodes == null) {
            return nodeIds;
        }
        for (IntentNode node : nodes) {
            if (node == null) {
                messages.add("intent node must not be null");
                continue;
            }
            if (isBlank(node.getId())) {
                messages.add("intent node id must not be blank");
            }
            else {
                nodeIds.add(node.getId());
            }
        }
        return nodeIds;
    }

    private void validateRelations(List<String> messages, List<IntentRelation> relations, Set<String> nodeIds) {
        if (relations == null) {
            return;
        }
        for (IntentRelation relation : relations) {
            if (relation == null) {
                messages.add("intent relation must not be null");
                continue;
            }
            requireNotBlank(messages, "intent relation id", relation.getId());
            requireNotBlank(messages, "intent relation source", relation.getSource());
            requireNotBlank(messages, "intent relation target", relation.getTarget());
            requireNotBlank(messages, "intent relation action", relation.getAction());

            if (!isBlank(relation.getSource()) && !nodeIds.contains(relation.getSource())) {
                messages.add("intent relation source does not reference an existing node: " + relation.getSource());
            }
            if (!isBlank(relation.getTarget()) && !nodeIds.contains(relation.getTarget())) {
                messages.add("intent relation target does not reference an existing node: " + relation.getTarget());
            }
        }
    }

    private void validateIntentBoundary(List<String> messages, NetworkIntent intent) {
        SemanticIntentGraph graph = intent.getSemanticIntentGraph();
        if (graph != null && graph.getNodes() != null) {
            for (IntentNode node : graph.getNodes()) {
                if (node == null) {
                    continue;
                }
                rejectBoundaryText(messages, "node.name", node.getName());
                rejectBoundaryText(messages, "node.type", node.getType());
                rejectBoundaryText(messages, "node.description", node.getDescription());
                rejectBoundaryAttributes(messages, node.getAttributes());
            }
        }

        if (graph != null && graph.getRelations() != null) {
            for (IntentRelation relation : graph.getRelations()) {
                if (relation == null) {
                    continue;
                }
                rejectBoundaryText(messages, "relation.type", relation.getType());
                rejectBoundaryText(messages, "relation.action", relation.getAction());
                rejectBoundaryText(messages, "relation.service", relation.getService());
                rejectBoundaryText(messages, "relation.description", relation.getDescription());
                rejectBoundaryConstraints(messages, relation.getConstraints());
            }
        }

        rejectBoundaryAssumptions(messages, intent.getAssumptions());
        rejectBoundaryConstraints(messages, intent.getConstraints());
        rejectBoundaryPreferences(messages, intent.getPreferences());
    }

    private void rejectBoundaryAttributes(List<String> messages, Map<String, Object> attributes) {
        if (attributes == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            rejectBoundaryText(messages, "node.attribute." + entry.getKey(), entry.getKey());
            rejectBoundaryText(messages, "node.attribute." + entry.getKey(), String.valueOf(entry.getValue()));
        }
    }

    private void rejectBoundaryAssumptions(List<String> messages, List<Assumption> assumptions) {
        if (assumptions == null) {
            return;
        }
        for (Assumption assumption : assumptions) {
            if (assumption == null) {
                continue;
            }
            rejectBoundaryText(messages, "assumption.field", assumption.getField());
            rejectBoundaryText(messages, "assumption.value", assumption.getValue());
            rejectBoundaryText(messages, "assumption.reason", assumption.getReason());
        }
    }

    private void rejectBoundaryConstraints(List<String> messages, List<IntentConstraint> constraints) {
        if (constraints == null) {
            return;
        }
        for (IntentConstraint constraint : constraints) {
            if (constraint == null) {
                continue;
            }
            rejectBoundaryText(messages, "constraint.type", constraint.getType());
            rejectBoundaryText(messages, "constraint.value", constraint.getValue());
            rejectBoundaryText(messages, "constraint.description", constraint.getDescription());
        }
    }

    private void rejectBoundaryPreferences(List<String> messages, List<IntentPreference> preferences) {
        if (preferences == null) {
            return;
        }
        for (IntentPreference preference : preferences) {
            if (preference == null) {
                continue;
            }
            rejectBoundaryText(messages, "preference.type", preference.getType());
            rejectBoundaryText(messages, "preference.value", preference.getValue());
        }
    }

    private void rejectBoundaryText(List<String> messages, String fieldName, String value) {
        if (isBlank(value)) {
            return;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (IP_ADDRESS_PATTERN.matcher(normalized).find()) {
            messages.add(fieldName + " contains IP-level content outside IntentAgent boundary");
            return;
        }
        for (String keyword : BOUNDARY_KEYWORDS) {
            if (normalized.contains(keyword)) {
                messages.add(fieldName + " contains " + keyword + " content outside IntentAgent boundary");
                return;
            }
        }
    }

    private void requireNotBlank(List<String> messages, String fieldName, String value) {
        if (isBlank(value)) {
            messages.add(fieldName + " must not be blank");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
