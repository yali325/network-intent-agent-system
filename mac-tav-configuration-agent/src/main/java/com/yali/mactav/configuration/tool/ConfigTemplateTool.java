package com.yali.mactav.configuration.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Spring AI method tool that suggests configuration templates for ConfigurationAgent.
 */
@Component
public class ConfigTemplateTool {

    private static final List<ConfigTemplate> TEMPLATES = List.of(
            new ConfigTemplate("tpl-vlan-access", "SWITCH", "VLAN", "generic",
                    "VLAN access binding template",
                    List.of(
                            "Define VLAN identifier and display name from NetworkPlan.",
                            "Bind selected access ports to the VLAN in a later ExecutionAdapter mapping step."),
                    "LOW"),
            new ConfigTemplate("tpl-l3-routing", "SWITCH_L3", "ROUTING", "generic",
                    "Layer-3 routing template",
                    List.of(
                            "Create routed interfaces from planned subnets.",
                            "Emit rollback commands for each generated routing block."),
                    "MEDIUM"),
            new ConfigTemplate("tpl-zone-acl", "SWITCH_L3", "ACL", "generic",
                    "Zone policy ACL template",
                    List.of(
                            "Translate securityPolicyPlan entries into named policy blocks.",
                            "Keep each commandBlock traceable to a plan policy id or intent relation id."),
                    "MEDIUM"));

    @Tool(name = "suggestConfigTemplate",
            description = "Suggest structured configuration templates by device type, feature, and target environment. Does not execute commands or write workspace state.")
    public ConfigTemplateResponse suggestConfigTemplate(
            @ToolParam(required = false, description = "Optional device type keyword, for example SWITCH or SWITCH_L3.")
            String deviceType,
            @ToolParam(required = false, description = "Optional feature keyword, for example VLAN, ROUTING, or ACL.")
            String feature,
            @ToolParam(required = false, description = "Optional target environment keyword, for example generic.")
            String targetEnvironment,
            @ToolParam(required = false, description = "Optional maximum number of templates to return. Defaults to 5 and is capped at 10.")
            Integer limit) {

        int safeLimit = limit == null || limit <= 0 ? 5 : Math.min(limit, 10);

        List<ConfigTemplate> matched = TEMPLATES.stream()
                .filter(template -> matches(template.deviceType(), deviceType))
                .filter(template -> matches(template.feature(), feature))
                .filter(template -> matches(template.targetEnvironment(), targetEnvironment))
                .limit(safeLimit)
                .toList();

        List<String> warnings = new ArrayList<>();
        if (matched.isEmpty()) {
            warnings.add("No configuration template matched the requested deviceType, feature, and targetEnvironment.");
        }
        return new ConfigTemplateResponse(matched, warnings);
    }

    private boolean matches(String candidate, String requested) {
        if (requested == null || requested.isBlank()) {
            return true;
        }
        return candidate != null && candidate.toLowerCase(Locale.ROOT).contains(requested.toLowerCase(Locale.ROOT));
    }

    /**
     * Response containing matched templates and warnings.
     */
    public record ConfigTemplateResponse(
            List<ConfigTemplate> templates,
            List<String> warnings) {
    }

    /**
     * One structured configuration template suggestion.
     */
    public record ConfigTemplate(
            String templateId,
            String deviceType,
            String feature,
            String targetEnvironment,
            String title,
            List<String> templateSteps,
            String riskLevel) {
    }
}
