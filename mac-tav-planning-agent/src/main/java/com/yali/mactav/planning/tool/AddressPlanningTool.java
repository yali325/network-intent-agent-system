package com.yali.mactav.planning.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Spring AI method tool that suggests address-plan entries for planning.
 *
 * <p>The tool returns structured address plan hints for intent zones. It must
 * not generate CLI, write NetworkWorkspace, or produce configuration blocks.</p>
 */
public class AddressPlanningTool {

    @Tool(name = "suggestAddressPlan", description = "Suggest address plan entries for network zones based on intent context.")
    public AddressPlanSuggestion suggestAddressPlan(
            @ToolParam(required = true, description = "JSON representation of the parsed NetworkIntent.") String intentJson,
            @ToolParam(required = false, description = "Optional target environment hint.") String environmentHint) {

        String safeIntent = intentJson == null ? "" : intentJson.toLowerCase(Locale.ROOT);
        List<AddressSuggestion> suggestions = new ArrayList<>();

        if (safeIntent.contains("office") || safeIntent.contains("\u529e\u516c")) {
            suggestions.add(new AddressSuggestion("addr-office",
                    "zone-office", "10.1.0.0/24", "10.1.0.1",
                    List.of("8.8.8.8"), "10.1.0.100"));
        }
        if (safeIntent.contains("guest") || safeIntent.contains("\u8bbf\u5ba2") || safeIntent.contains("\u6765\u5bbe")) {
            suggestions.add(new AddressSuggestion("addr-guest",
                    "zone-guest", "10.2.0.0/24", "10.2.0.1",
                    List.of("8.8.8.8"), "10.2.0.100"));
        }
        if (safeIntent.contains("server") || safeIntent.contains("\u670d\u52a1\u5668")) {
            suggestions.add(new AddressSuggestion("addr-server",
                    "zone-server", "10.3.0.0/24", "10.3.0.1",
                    List.of("8.8.8.8"), "10.3.0.10"));
        }
        if (safeIntent.contains("internet") || safeIntent.contains("\u4e92\u8054\u7f51") || safeIntent.contains("\u5916\u7f51")) {
            suggestions.add(new AddressSuggestion("addr-internet",
                    "zone-internet", "203.0.113.0/30", "203.0.113.1",
                    List.of(), null));
        }

        List<String> warnings = new ArrayList<>();
        if (suggestions.isEmpty()) {
            warnings.add("No zones detected for address planning. Provide explicit zone information.");
        }

        return new AddressPlanSuggestion(suggestions, warnings);
    }

    /**
     * Tool result for suggested address plan entries.
     */
    public record AddressPlanSuggestion(
            List<AddressSuggestion> suggestions,
            List<String> warnings) {
    }

    /**
     * One suggested address plan entry.
     */
    public record AddressSuggestion(
            String id,
            String zoneId,
            String subnet,
            String gateway,
            List<String> dnsServers,
            String exampleHostAddress) {
    }
}
