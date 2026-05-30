package com.yali.mactav.planning.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Spring AI method tool that suggests VLAN plan entries for planning.
 *
 * <p>The tool returns structured VLAN plan hints. It must not generate CLI,
 * write NetworkWorkspace, or produce configuration blocks.</p>
 */
public class VlanPlanningTool {

    @Tool(name = "suggestVlanPlan", description = "Suggest VLAN plan entries for network zones based on intent context.")
    public VlanPlanSuggestion suggestVlanPlan(
            @ToolParam(required = true, description = "JSON representation of the parsed NetworkIntent.") String intentJson,
            @ToolParam(required = false, description = "Optional target environment hint.") String environmentHint) {

        String safeIntent = intentJson == null ? "" : intentJson.toLowerCase(Locale.ROOT);
        List<VlanSuggestion> suggestions = new ArrayList<>();

        int vlanBase = 100;
        if (safeIntent.contains("office") || safeIntent.contains("\u529e\u516c")) {
            suggestions.add(new VlanSuggestion("vlan-office", vlanBase, "office", "zone-office"));
            vlanBase += 10;
        }
        if (safeIntent.contains("guest") || safeIntent.contains("\u8bbf\u5ba2") || safeIntent.contains("\u6765\u5bbe")) {
            suggestions.add(new VlanSuggestion("vlan-guest", vlanBase, "guest", "zone-guest"));
            vlanBase += 10;
        }
        if (safeIntent.contains("server") || safeIntent.contains("\u670d\u52a1\u5668")) {
            suggestions.add(new VlanSuggestion("vlan-server", vlanBase, "server", "zone-server"));
            vlanBase += 10;
        }
        if (safeIntent.contains("internet")) {
            suggestions.add(new VlanSuggestion("vlan-internet", vlanBase, "internet", "zone-internet"));
        }

        List<String> warnings = new ArrayList<>();
        if (suggestions.isEmpty()) {
            warnings.add("No zones detected for VLAN planning. Provide explicit zone information.");
        }

        return new VlanPlanSuggestion(suggestions, warnings);
    }

    /**
     * Tool result for suggested VLAN plan entries.
     */
    public record VlanPlanSuggestion(
            List<VlanSuggestion> suggestions,
            List<String> warnings) {
    }

    /**
     * One suggested VLAN plan entry.
     */
    public record VlanSuggestion(
            String id,
            Integer vlanId,
            String name,
            String zoneId) {
    }
}
