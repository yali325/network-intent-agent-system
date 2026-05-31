package com.yali.mactav.planning.validator;

import com.yali.mactav.agent.core.validator.AgentOutputValidator;
import com.yali.mactav.agent.core.validator.ValidationResult;
import com.yali.mactav.model.plan.AddressPlanItem;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.plan.RoutingPlan;
import com.yali.mactav.model.plan.SecurityPolicyPlanItem;
import com.yali.mactav.model.plan.TargetEnvironment;
import com.yali.mactav.model.plan.VlanPlanItem;
import com.yali.mactav.model.workspace.TraceRefs;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates NetworkPlan output before it can leave mac-tav-planning-agent.
 *
 * <p>The validator enforces the planning-stage boundary: NetworkPlan must not
 * contain executable CLI, concrete device configs, or downstream-stage content.
 * It must not call a model, repair output, write NetworkWorkspace, or decide
 * orchestration flow.</p>
 */
public class PlanningOutputValidator implements AgentOutputValidator<NetworkPlan> {

    private static final Pattern CLI_COMMAND_PATTERN =
            Pattern.compile("\\b(configure terminal|show running-config|no shutdown|switchport mode|"
                    + "ip address|ip route|router ospf|router bgp|router-id|"
                    + "network \\d+\\.\\d+\\.\\d+\\.\\d+|access-list|snmp-server|"
                    + "ntp server|logging host|aaa new-model|line vty|enable secret)\\b",
                    Pattern.CASE_INSENSITIVE);

    private static final Set<String> ALLOWED_SECURITY_ACTIONS = Set.of("ALLOW", "DENY", "PERMIT", "BLOCK");

    @Override
    public ValidationResult validate(NetworkPlan plan) {
        List<String> messages = new ArrayList<>();
        if (plan == null) {
            messages.add("NetworkPlan must not be null");
            return ValidationResult.fail(messages);
        }

        requireNotBlank(messages, "taskId", plan.getTaskId());
        requireNotNull(messages, "intentVersion", plan.getIntentVersion());

        if (plan.getTopology() == null) {
            messages.add("topology must not be null");
        }
        else {
            if (plan.getTopology().getNodes() == null || plan.getTopology().getNodes().isEmpty()) {
                messages.add("topology.nodes must not be empty");
            }
            validateTopologyNodes(messages, plan.getTopology().getNodes());
            validateTopologyLinks(messages, plan.getTopology().getLinks());
        }

        validateZones(messages, plan);
        validateTargetEnvironment(messages, plan.getTargetEnvironment());
        validateAddressPlan(messages, plan.getAddressPlan());
        validateVlanPlan(messages, plan.getVlanPlan());
        validateRoutingPlan(messages, plan.getRoutingPlan());
        validateSecurityPolicies(messages, plan);
        validateTraceRefs(messages, "traceRefs", plan.getTraceRefs());

        if (plan.getPlanSummary() != null) {
            rejectCliContent(messages, "planSummary", plan.getPlanSummary());
        }

        return messages.isEmpty() ? ValidationResult.ok() : ValidationResult.fail(messages);
    }

    private void validateTopologyNodes(List<String> messages,
                                       List<com.yali.mactav.model.plan.TopologyNode> nodes) {
        if (nodes == null) {
            return;
        }
        Set<String> seen = new HashSet<>();
        for (com.yali.mactav.model.plan.TopologyNode node : nodes) {
            if (node == null) {
                messages.add("topology node must not be null");
                continue;
            }
            if (isBlank(node.getId())) {
                messages.add("topology node id must not be blank");
            }
            else if (!seen.add(node.getId())) {
                messages.add("topology node id must be unique: " + node.getId());
            }
        }
    }

    private void validateTopologyLinks(List<String> messages,
                                       List<com.yali.mactav.model.plan.TopologyLink> links) {
        if (links == null) {
            return;
        }
        Set<String> seen = new HashSet<>();
        for (com.yali.mactav.model.plan.TopologyLink link : links) {
            if (link == null) {
                messages.add("topology link must not be null");
                continue;
            }
            if (isBlank(link.getId())) {
                messages.add("topology link id must not be blank");
            }
            else if (!seen.add(link.getId())) {
                messages.add("topology link id must be unique: " + link.getId());
            }
        }
    }

    private void validateZones(List<String> messages, NetworkPlan plan) {
        if (plan.getZones() == null) {
            return;
        }
        Set<String> seen = new HashSet<>();
        for (com.yali.mactav.model.plan.NetworkZone zone : plan.getZones()) {
            if (zone == null) {
                messages.add("zone must not be null");
                continue;
            }
            if (isBlank(zone.getId())) {
                messages.add("zone id must not be blank");
            }
            else if (!seen.add(zone.getId())) {
                messages.add("zone id must be unique: " + zone.getId());
            }
        }
    }

    private void validateTargetEnvironment(List<String> messages, TargetEnvironment targetEnvironment) {
        if (targetEnvironment == null) {
            messages.add("targetEnvironment must not be null");
            return;
        }
        requireNotBlank(messages, "targetEnvironment.vendor", targetEnvironment.getVendor());
        requireNotBlank(messages, "targetEnvironment.configStyle", targetEnvironment.getConfigStyle());
        requireNotBlank(messages, "targetEnvironment.adapterType", targetEnvironment.getAdapterType());
    }

    private void validateAddressPlan(List<String> messages, List<AddressPlanItem> items) {
        if (items == null || items.isEmpty()) {
            messages.add("addressPlan must not be empty");
            return;
        }
        Set<String> seen = new HashSet<>();
        for (AddressPlanItem item : items) {
            if (item == null) {
                messages.add("addressPlan item must not be null");
                continue;
            }
            if (isBlank(item.getId())) {
                messages.add("addressPlan item id must not be blank");
            }
            else if (!seen.add(item.getId())) {
                messages.add("addressPlan item id must be unique: " + item.getId());
            }
            requireNotBlank(messages, "addressPlan.subnet", item.getSubnet());
            requireNotBlank(messages, "addressPlan.gateway", item.getGateway());
            validateTraceRefs(messages, "addressPlan.traceRefs", item.getTraceRefs());
        }
    }

    private void validateVlanPlan(List<String> messages, List<VlanPlanItem> items) {
        if (items == null || items.isEmpty()) {
            messages.add("vlanPlan must not be empty");
            return;
        }
        Set<String> seenIds = new HashSet<>();
        Set<Integer> seenVlans = new HashSet<>();
        for (VlanPlanItem item : items) {
            if (item == null) {
                messages.add("vlanPlan item must not be null");
                continue;
            }
            if (isBlank(item.getId())) {
                messages.add("vlanPlan item id must not be blank");
            }
            else if (!seenIds.add(item.getId())) {
                messages.add("vlanPlan item id must be unique: " + item.getId());
            }
            if (item.getVlanId() == null) {
                messages.add("vlanPlan.vlanId must not be null");
            }
            else {
                if (item.getVlanId() < 1 || item.getVlanId() > 4094) {
                    messages.add("vlanPlan.vlanId must be between 1 and 4094: " + item.getVlanId());
                }
                if (!seenVlans.add(item.getVlanId())) {
                    messages.add("vlanPlan.vlanId must be unique: " + item.getVlanId());
                }
            }
            requireNotBlank(messages, "vlanPlan.zoneId", item.getZoneId());
        }
    }

    private void validateRoutingPlan(List<String> messages, RoutingPlan routingPlan) {
        if (routingPlan == null) {
            messages.add("routingPlan must not be null");
            return;
        }
        requireNotBlank(messages, "routingPlan.id", routingPlan.getId());
        requireNotBlank(messages, "routingPlan.protocol", routingPlan.getProtocol());
        if (routingPlan.getRouters() == null || routingPlan.getRouters().isEmpty()) {
            messages.add("routingPlan.routers must not be empty");
        }
        validateTraceRefs(messages, "routingPlan.traceRefs", routingPlan.getTraceRefs());
    }

    private void validateSecurityPolicies(List<String> messages, NetworkPlan plan) {
        List<SecurityPolicyPlanItem> policies = plan.getSecurityPolicyPlan();
        if (policies == null) {
            return;
        }
        Set<String> seen = new HashSet<>();
        for (SecurityPolicyPlanItem policy : policies) {
            if (policy == null) {
                messages.add("security policy must not be null");
                continue;
            }
            if (isBlank(policy.getId())) {
                messages.add("security policy id must not be blank");
            }
            else if (!seen.add(policy.getId())) {
                messages.add("security policy id must be unique: " + policy.getId());
            }
            if (!isBlank(policy.getAction())) {
                String normalized = policy.getAction().trim().toUpperCase();
                if (!ALLOWED_SECURITY_ACTIONS.contains(normalized)) {
                    messages.add("security policy action has unsupported value: " + policy.getAction());
                }
            }
        }
    }

    private void validateTraceRefs(List<String> messages, String fieldName, TraceRefs traceRefs) {
        if (traceRefs == null || traceRefs.getIntentNodeIds() == null || traceRefs.getIntentNodeIds().isEmpty()) {
            messages.add(fieldName + " must include at least one intentNodeId");
        }
    }

    private void rejectCliContent(List<String> messages, String fieldName, String value) {
        if (isBlank(value)) {
            return;
        }
        if (CLI_COMMAND_PATTERN.matcher(value).find()) {
            messages.add(fieldName + " contains CLI command content outside PlanningAgent boundary");
        }
    }

    private void requireNotBlank(List<String> messages, String fieldName, String value) {
        if (isBlank(value)) {
            messages.add(fieldName + " must not be blank");
        }
    }

    private void requireNotNull(List<String> messages, String fieldName, Object value) {
        if (value == null) {
            messages.add(fieldName + " must not be null");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
