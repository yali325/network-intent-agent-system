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
            @ToolParam(required = true, description = "Template lookup request with deviceType, feature, and targetEnvironment.") ConfigTemplateRequest request) {

        ConfigTemplateRequest safeRequest = request == null
                ? new ConfigTemplateRequest(null, null, null, 5)
                : request;
        int limit = safeRequest.limit() == null || safeRequest.limit() <= 0 ? 5 : Math.min(safeRequest.limit(), 10);

        List<ConfigTemplate> matched = TEMPLATES.stream()
                .filter(template -> matches(template.deviceType(), safeRequest.deviceType()))
                .filter(template -> matches(template.feature(), safeRequest.feature()))
                .filter(template -> matches(template.targetEnvironment(), safeRequest.targetEnvironment()))
                .limit(limit)
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
     * Request for configuration template lookup.
     */
    public record ConfigTemplateRequest(
            String deviceType,
            String feature,
            String targetEnvironment,
            Integer limit) {
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
