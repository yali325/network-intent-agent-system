package com.yali.mactav.planning.tool;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Spring AI method tool that suggests security policy and routing design
 * based on intent relations and preferences.
 *
 * <p>The tool returns structured planning hints. It must not generate CLI,
 * write NetworkWorkspace, or produce configuration blocks.</p>
 */
public class PlanningPlaybookTool {

    private static final Pattern INTENT_NODE_ID_PATTERN =
            Pattern.compile("\"id\"\\s*:\\s*\"(node-[^\"]+)\"");

    @Tool(name = "suggestPlanningPlaybook", description = "Suggest security policies and routing decisions based on intent relations and preferences.")
    public PlanningPlaybookSuggestion suggestPlaybook(
            @ToolParam(required = true, description = "JSON representation of the parsed NetworkIntent.") String intentJson) {

        String safeIntent = intentJson == null ? "" : intentJson.toLowerCase(Locale.ROOT);
        List<SecurityPolicyHint> securityHints = new ArrayList<>();
        List<RoutingHint> routingHints = new ArrayList<>();
        List<String> traceIntentNodeIds = extractIntentNodeIds(intentJson);
        List<RouterCandidate> routerCandidates = List.of(new RouterCandidate("rtr-edge", "ROUTER", "GATEWAY"));

        boolean hasOffice = safeIntent.contains("office") || safeIntent.contains("\u529e\u516c");
        boolean hasGuest = safeIntent.contains("guest") || safeIntent.contains("\u8bbf\u5ba2") || safeIntent.contains("\u6765\u5bbe");
        boolean hasServer = safeIntent.contains("server") || safeIntent.contains("\u670d\u52a1\u5668");
        boolean hasInternet = safeIntent.contains("internet") || safeIntent.contains("\u4e92\u8054\u7f51") || safeIntent.contains("\u5916\u7f51");

        if (hasOffice && hasServer && containsAny(safeIntent, "allow", "access", "\u8bbf\u95ee", "\u5141\u8bb8")) {
            securityHints.add(new SecurityPolicyHint("sec-office-to-server", "Office to Server Access",
                    "zone-office", "zone-server", "ALLOW", "business-application",
                    "sw-core", "inbound", "rel-office-server"));
        }
        if (hasGuest && hasServer && containsAny(safeIntent, "deny", "cannot", "\u7981\u6b62", "\u4e0d\u80fd", "\u4e0d\u53ef")) {
            securityHints.add(new SecurityPolicyHint("sec-guest-to-server", "Guest to Server Deny",
                    "zone-guest", "zone-server", "DENY", "any",
                    "sw-core", "inbound", "rel-guest-server"));
        }
        if (hasOffice && hasGuest && containsAny(safeIntent, "isolate", "isolated", "\u9694\u79bb")) {
            securityHints.add(new SecurityPolicyHint("sec-office-guest-isolation", "Office-Guest Isolation",
                    "zone-office", "zone-guest", "DENY", "any",
                    "sw-core", "inbound", "rel-office-guest"));
        }
        if (hasInternet) {
            Map<String, String> internetRules = new HashMap<>();
            if (hasOffice) {
                internetRules.put("sec-office-to-internet", "Office to Internet");
            }
            if (hasGuest) {
                internetRules.put("sec-guest-to-internet", "Guest to Internet");
            }
            for (var entry : internetRules.entrySet()) {
                securityHints.add(new SecurityPolicyHint(entry.getKey(), entry.getValue(),
                        entry.getKey().contains("office") ? "zone-office" : "zone-guest",
                        "zone-internet", "ALLOW", "internet-access",
                        "rtr-edge", "outbound", null));
            }
        }

        if (containsAny(safeIntent, "ospf")) {
            routingHints.add(new RoutingHint("routing-ospf", "OSPF", "0.0.0.0",
                    "OSPF single-area routing with area 0 for all internal zones",
                    routerCandidates, traceIntentNodeIds));
        }
        else if (containsAny(safeIntent, "bgp")) {
            routingHints.add(new RoutingHint("routing-bgp", "BGP", null,
                    "BGP routing for inter-AS connectivity",
                    routerCandidates, traceIntentNodeIds));
        }
        else {
            routingHints.add(new RoutingHint("routing-static", "STATIC", null,
                    "Static routing as default fallback",
                    routerCandidates, traceIntentNodeIds));
        }

        List<String> warnings = new ArrayList<>();
        if (securityHints.isEmpty()) {
            warnings.add("No explicit security policy hints derived from intent.");
        }

        return new PlanningPlaybookSuggestion(securityHints, routingHints, warnings);
    }

    private boolean containsAny(String input, String... candidates) {
        for (String candidate : candidates) {
            if (input.contains(candidate.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private List<String> extractIntentNodeIds(String intentJson) {
        if (intentJson == null || intentJson.isBlank()) {
            return List.of();
        }
        Set<String> ids = new LinkedHashSet<>();
        Matcher matcher = INTENT_NODE_ID_PATTERN.matcher(intentJson);
        while (matcher.find()) {
            ids.add(matcher.group(1));
        }
        return new ArrayList<>(ids);
    }

    /**
     * Tool result combining security and routing suggestions.
     */
    public record PlanningPlaybookSuggestion(
            List<SecurityPolicyHint> securityPolicies,
            List<RoutingHint> routingHints,
            List<String> warnings) {
    }

    /**
     * One suggested security policy.
     */
    public record SecurityPolicyHint(
            String id,
            String name,
            String sourceZone,
            String targetZone,
            String action,
            String service,
            String enforcementDeviceId,
            String enforcementDirection,
            String basedOnIntentRelation) {
    }

    /**
     * One suggested routing decision.
     */
    public record RoutingHint(
            String id,
            String protocol,
            String area,
            String description,
            List<RouterCandidate> routerCandidates,
            List<String> traceIntentNodeIds) {
    }

    /**
     * Router candidate that the model can reference from topologyNodes.
     */
    public record RouterCandidate(
            String deviceId,
            String nodeType,
            String role) {
    }
}
