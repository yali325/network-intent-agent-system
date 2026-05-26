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

    private static final Pattern IP_OR_CIDR_PATTERN = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}(?:/\\d{1,2})?\\b");

    private static final Pattern NETWORK_COMMAND_PATTERN =
            Pattern.compile("\\bnetwork\\s+(?:\\d{1,3}\\.){3}\\d{1,3}\\b");

    private static final Pattern INTERFACE_COMMAND_PATTERN =
            Pattern.compile("\\binterface\\s+(?:gigabitethernet|fastethernet|ethernet|loopback|vlanif|ge|xge)\\S*\\b");

    private static final Pattern OSPF_AREA_PATTERN = Pattern.compile("\\barea\\s+\\d+\\b");

    private static final List<String> IMPLEMENTATION_KEYWORDS = List.of(
            "vlan",
            "cli",
            "topology",
            "device",
            "acl",
            "access-list",
            "route-map",
            "ip address",
            "ip route",
            "router ospf",
            "router bgp",
            "router-id",
            "router id",
            "interface ",
            "gigabitethernet",
            "fastethernet",
            "configure terminal",
            "show running-config",
            "command block"
    );

    private static final Set<String> ROUTING_PROTOCOL_TERMS = Set.of("OSPF", "BGP", "STATIC");

    private static final Set<String> DISALLOWED_NODE_TYPES = Set.of(
            "DEVICE",
            "ROUTER",
            "SWITCH",
            "FIREWALL",
            "INTERFACE"
    );

    private static final Set<String> ALLOWED_RELATION_ACTIONS = Set.of(
            "ALLOW",
            "DENY",
            "REQUIRE",
            "ISOLATE"
    );

    private static final Set<String> ALLOWED_RELATION_TYPES = Set.of(
            "ACCESS",
            "ISOLATION",
            "INTERNET_ACCESS",
            "SERVICE_ACCESS",
            "ROUTING_REQUIREMENT"
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
            else if (!nodeIds.add(node.getId())) {
                messages.add("intent node id must be unique: " + node.getId());
            }
        }
        return nodeIds;
    }

    private void validateRelations(List<String> messages, List<IntentRelation> relations, Set<String> nodeIds) {
        if (relations == null) {
            return;
        }
        Set<String> relationIds = new HashSet<>();
        for (IntentRelation relation : relations) {
            if (relation == null) {
                messages.add("intent relation must not be null");
                continue;
            }
            requireNotBlank(messages, "intent relation id", relation.getId());
            requireNotBlank(messages, "intent relation source", relation.getSource());
            requireNotBlank(messages, "intent relation target", relation.getTarget());
            requireNotBlank(messages, "intent relation action", relation.getAction());
            requireNotBlank(messages, "intent relation type", relation.getType());

            if (!isBlank(relation.getId()) && !relationIds.add(relation.getId())) {
                messages.add("intent relation id must be unique: " + relation.getId());
            }

            validateAllowedValue(messages, "intent relation type", relation.getType(), ALLOWED_RELATION_TYPES);
            validateAllowedValue(messages, "intent relation action", relation.getAction(), ALLOWED_RELATION_ACTIONS);

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
                rejectImplementationText(messages, "node.name", node.getName(), false);
                rejectImplementationText(messages, "node.type", node.getType(), false);
                rejectImplementationText(messages, "node.description", node.getDescription(), false);
                rejectDisallowedNodeType(messages, node.getType());
                rejectBoundaryAttributes(messages, node.getAttributes());
            }
        }

        if (graph != null && graph.getRelations() != null) {
            for (IntentRelation relation : graph.getRelations()) {
                if (relation == null) {
                    continue;
                }
                rejectImplementationText(messages, "relation.type", relation.getType(), false);
                rejectImplementationText(messages, "relation.action", relation.getAction(), false);
                rejectImplementationText(messages, "relation.source", relation.getSource(), false);
                rejectImplementationText(messages, "relation.target", relation.getTarget(), false);
                rejectImplementationText(messages, "relation.service", relation.getService(), false);
                rejectImplementationText(messages, "relation.description", relation.getDescription(), false);
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
            rejectImplementationText(messages, "node.attribute." + entry.getKey(), entry.getKey(), false);
            rejectImplementationText(messages, "node.attribute." + entry.getKey(), String.valueOf(entry.getValue()), false);
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
            rejectImplementationText(messages, "assumption.field", assumption.getField(), false);
            rejectImplementationText(messages, "assumption.value", assumption.getValue(), true);
            rejectImplementationText(messages, "assumption.reason", assumption.getReason(), false);
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
            rejectImplementationText(messages, "constraint.type", constraint.getType(), false);
            rejectImplementationText(messages, "constraint.value", constraint.getValue(), true);
            rejectImplementationText(messages, "constraint.description", constraint.getDescription(), false);
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
            rejectImplementationText(messages, "preference.type", preference.getType(), false);
            rejectImplementationText(messages, "preference.value", preference.getValue(), true);
        }
    }

    private void rejectImplementationText(List<String> messages,
                                          String fieldName,
                                          String value,
                                          boolean allowRoutingProtocolTerm) {
        if (isBlank(value)) {
            return;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (IP_OR_CIDR_PATTERN.matcher(normalized).find()) {
            messages.add(fieldName + " contains IP or CIDR content outside IntentAgent boundary");
            return;
        }
        if (containsCliPattern(normalized)) {
            messages.add(fieldName + " contains CLI or routing configuration content outside IntentAgent boundary");
            return;
        }
        for (String keyword : IMPLEMENTATION_KEYWORDS) {
            if (normalized.contains(keyword)) {
                messages.add(fieldName + " contains " + keyword + " content outside IntentAgent boundary");
                return;
            }
        }
        if (!allowRoutingProtocolTerm && containsRoutingProtocolTerm(value)) {
            messages.add(fieldName + " contains routing protocol content outside its allowed intent field");
        }
    }

    private boolean containsCliPattern(String normalized) {
        return NETWORK_COMMAND_PATTERN.matcher(normalized).find()
                || INTERFACE_COMMAND_PATTERN.matcher(normalized).find()
                || OSPF_AREA_PATTERN.matcher(normalized).find();
    }

    private void rejectDisallowedNodeType(List<String> messages, String nodeType) {
        String normalized = normalizeToken(nodeType);
        if (DISALLOWED_NODE_TYPES.contains(normalized)) {
            messages.add("node.type must remain a business object type, not implementation type: " + nodeType);
        }
    }

    private void validateAllowedValue(List<String> messages, String fieldName, String value, Set<String> allowedValues) {
        if (isBlank(value)) {
            return;
        }
        String normalized = normalizeToken(value);
        if (!allowedValues.contains(normalized)) {
            messages.add(fieldName + " has unsupported value: " + value);
        }
    }

    private boolean containsRoutingProtocolTerm(String value) {
        String normalized = normalizeToken(value);
        return ROUTING_PROTOCOL_TERMS.contains(normalized)
                || normalized.contains("_OSPF")
                || normalized.contains("OSPF_")
                || normalized.contains("_BGP")
                || normalized.contains("BGP_")
                || normalized.contains("_STATIC")
                || normalized.contains("STATIC_");
    }

    private String normalizeToken(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
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
