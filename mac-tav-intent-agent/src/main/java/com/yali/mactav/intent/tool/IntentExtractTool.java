package com.yali.mactav.intent.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Spring AI method tool that extracts intent-stage business hints.
 *
 * <p>The tool returns structured hints for business objects, relations,
 * preferences, constraints, and warnings. It must not generate devices,
 * interfaces, VLANs, IP addresses, topology, routing configuration, ACLs, CLI,
 * or write NetworkWorkspace.</p>
 */
public class IntentExtractTool {

    private static final Pattern IP_OR_CIDR_PATTERN =
            Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}(?:/\\d{1,2})?\\b");

    private static final List<String> IMPLEMENTATION_TERMS = List.of(
            "vlan",
            "interface",
            "gigabitethernet",
            "fastethernet",
            "ip address",
            "access-list",
            "acl",
            "router-id",
            "router ospf",
            "network ",
            "configure terminal",
            "show running-config"
    );

    @Tool(name = "extractIntentHints", description = "Extract business objects, access relations, constraints, and preferences for MAC-TAV IntentAgent.")
    public IntentExtractionHints extractIntentHints(
            @ToolParam(required = true, description = "User natural language network requirement.") String rawText) {
        String safeText = rawText == null ? "" : rawText;
        String normalized = safeText.toLowerCase(Locale.ROOT);

        List<BusinessObjectHint> objects = extractBusinessObjects(normalized);
        List<RelationHint> relations = extractRelations(normalized);
        List<PreferenceHint> preferences = extractPreferences(normalized);
        List<ConstraintHint> constraints = extractConstraints(normalized);
        List<String> warnings = extractWarnings(normalized, objects);

        return new IntentExtractionHints(objects, relations, preferences, constraints, warnings);
    }

    private List<BusinessObjectHint> extractBusinessObjects(String normalized) {
        List<BusinessObjectHint> objects = new ArrayList<>();
        if (containsAny(normalized, "office", "办公")) {
            objects.add(new BusinessObjectHint("node-office", "office", "ZONE"));
        }
        if (containsAny(normalized, "guest", "访客", "来宾")) {
            objects.add(new BusinessObjectHint("node-guest", "guest", "ZONE"));
        }
        if (containsAny(normalized, "server", "服务器")) {
            objects.add(new BusinessObjectHint("node-server", "server", "SERVICE"));
        }
        if (containsAny(normalized, "internet", "互联网", "外网")) {
            objects.add(new BusinessObjectHint("node-internet", "internet", "EXTERNAL_NETWORK"));
        }
        return objects;
    }

    private List<RelationHint> extractRelations(String normalized) {
        List<RelationHint> relations = new ArrayList<>();
        if (containsAny(normalized, "office", "办公")
                && containsAny(normalized, "server", "服务器")
                && containsAny(normalized, "access", "访问", "allow", "允许")) {
            relations.add(new RelationHint("rel-office-server", "ACCESS", "node-office", "node-server", "ALLOW"));
        }
        if (containsAny(normalized, "guest", "访客", "来宾")
                && containsAny(normalized, "server", "服务器")
                && containsAny(normalized, "cannot", "can not", "deny", "禁止", "不可", "不能")) {
            relations.add(new RelationHint("rel-guest-server", "ACCESS", "node-guest", "node-server", "DENY"));
        }
        if (containsAny(normalized, "office", "办公")
                && containsAny(normalized, "guest", "访客", "来宾")
                && containsAny(normalized, "isolate", "isolated", "隔离")) {
            relations.add(new RelationHint("rel-office-guest", "ISOLATION", "node-office", "node-guest", "DENY"));
        }
        return relations;
    }

    private List<PreferenceHint> extractPreferences(String normalized) {
        List<PreferenceHint> preferences = new ArrayList<>();
        if (containsAny(normalized, "ospf")) {
            preferences.add(new PreferenceHint("pref-routing-protocol", "routing-protocol-preference", "OSPF"));
        }
        else if (containsAny(normalized, "bgp")) {
            preferences.add(new PreferenceHint("pref-routing-protocol", "routing-protocol-preference", "BGP"));
        }
        else if (containsAny(normalized, "static route", "静态路由")) {
            preferences.add(new PreferenceHint("pref-routing-protocol", "routing-protocol-preference", "STATIC"));
        }
        return preferences;
    }

    private List<ConstraintHint> extractConstraints(String normalized) {
        List<ConstraintHint> constraints = new ArrayList<>();
        if (containsAny(normalized, "low risk", "低风险")) {
            constraints.add(new ConstraintHint("con-low-risk", "risk", "prefer low-risk implementation"));
        }
        if (containsAny(normalized, "isolate", "isolated", "隔离")) {
            constraints.add(new ConstraintHint("con-isolation", "security", "keep isolated business groups separated"));
        }
        return constraints;
    }

    private List<String> extractWarnings(String normalized, List<BusinessObjectHint> objects) {
        List<String> warnings = new ArrayList<>();
        if (objects.isEmpty()) {
            warnings.add("No explicit business object hint was recognized.");
        }
        if (IP_OR_CIDR_PATTERN.matcher(normalized).find() || containsImplementationTerm(normalized)) {
            warnings.add("Implementation details were ignored because IntentAgent only keeps business intent.");
        }
        return warnings;
    }

    private boolean containsImplementationTerm(String normalized) {
        for (String term : IMPLEMENTATION_TERMS) {
            if (normalized.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAny(String normalized, String... candidates) {
        for (String candidate : candidates) {
            if (normalized.contains(candidate.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Structured result returned from the intent extraction tool.
     */
    public record IntentExtractionHints(
            List<BusinessObjectHint> businessObjects,
            List<RelationHint> relations,
            List<PreferenceHint> preferences,
            List<ConstraintHint> constraints,
            List<String> warnings) {
    }

    /**
     * Tool hint for one business object, not a network device.
     */
    public record BusinessObjectHint(String id, String name, String type) {
    }

    /**
     * Tool hint for one business-level relation between objects.
     */
    public record RelationHint(String id, String type, String source, String target, String action) {
    }

    /**
     * Tool hint for one user preference preserved at intent level.
     */
    public record PreferenceHint(String id, String type, String value) {
    }

    /**
     * Tool hint for one user or inferred business constraint.
     */
    public record ConstraintHint(String id, String type, String value) {
    }
}
