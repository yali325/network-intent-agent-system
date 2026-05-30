package com.yali.mactav.planning.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Spring AI method tool that suggests topology templates for planning.
 *
 * <p>The tool returns structured topology hints. It must not generate CLI,
 * write NetworkWorkspace, or produce configuration blocks.</p>
 */
public class TopologyTemplateTool {

    @Tool(name = "suggestTopologyTemplate", description = "Suggest topology node and link templates for network planning.")
    public TopologyTemplateSuggestion suggestTopology(
            @ToolParam(required = true, description = "JSON representation of the parsed NetworkIntent.") String intentJson) {

        String safeIntent = intentJson == null ? "" : intentJson.toLowerCase(Locale.ROOT);
        List<NodeSuggestion> nodes = new ArrayList<>();
        List<LinkSuggestion> links = new ArrayList<>();

        boolean hasOffice = safeIntent.contains("office") || safeIntent.contains("\u529e\u516c");
        boolean hasGuest = safeIntent.contains("guest") || safeIntent.contains("\u8bbf\u5ba2") || safeIntent.contains("\u6765\u5bbe");
        boolean hasServer = safeIntent.contains("server") || safeIntent.contains("\u670d\u52a1\u5668");
        boolean hasInternet = safeIntent.contains("internet") || safeIntent.contains("\u4e92\u8054\u7f51") || safeIntent.contains("\u5916\u7f51");

        nodes.add(new NodeSuggestion("sw-core", "core-switch", "SWITCH", "core", "zone-core", "CORE"));
        nodes.add(new NodeSuggestion("rtr-edge", "edge-router", "ROUTER", "edge", "zone-core", "GATEWAY"));

        if (hasOffice) {
            nodes.add(new NodeSuggestion("sw-office", "office-switch", "SWITCH", "access", "zone-office", "ACCESS"));
            links.add(new LinkSuggestion("link-core-office", "sw-core", "sw-office", "TRUNK"));
        }
        if (hasGuest) {
            nodes.add(new NodeSuggestion("sw-guest", "guest-switch", "SWITCH", "access", "zone-guest", "ACCESS"));
            links.add(new LinkSuggestion("link-core-guest", "sw-core", "sw-guest", "TRUNK"));
        }
        if (hasServer) {
            nodes.add(new NodeSuggestion("sw-server", "server-switch", "SWITCH", "access", "zone-server", "ACCESS"));
            links.add(new LinkSuggestion("link-core-server", "sw-core", "sw-server", "TRUNK"));
        }
        if (hasInternet) {
            links.add(new LinkSuggestion("link-edge-internet", "rtr-edge", null, "WAN"));
            nodes.add(new NodeSuggestion("node-internet", "internet-gateway", "EXTERNAL", "gateway", "zone-internet", "EXTERNAL"));
        }

        List<String> warnings = new ArrayList<>();
        if (nodes.size() <= 2) {
            warnings.add("Minimal topology generated. Provide more zone information for a richer topology.");
        }

        return new TopologyTemplateSuggestion(nodes, links, warnings);
    }

    /**
     * Tool result for suggested topology template.
     */
    public record TopologyTemplateSuggestion(
            List<NodeSuggestion> nodes,
            List<LinkSuggestion> links,
            List<String> warnings) {
    }

    /**
     * One suggested topology node.
     */
    public record NodeSuggestion(
            String id,
            String name,
            String deviceType,
            String role,
            String zoneId,
            String nodeType) {
    }

    /**
     * One suggested topology link.
     */
    public record LinkSuggestion(
            String id,
            String sourceNode,
            String targetNode,
            String linkType) {
    }
}
