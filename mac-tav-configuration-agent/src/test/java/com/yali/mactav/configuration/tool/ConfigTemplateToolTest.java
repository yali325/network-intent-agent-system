package com.yali.mactav.configuration.tool;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Offline tests for ConfigTemplateTool.
 */
class ConfigTemplateToolTest {

    private final ConfigTemplateTool tool = new ConfigTemplateTool();

    @Test
    void shouldFindTemplateByFeature() {
        var response = tool.suggestConfigTemplate(
                new ConfigTemplateTool.ConfigTemplateRequest("SWITCH", "VLAN", "generic", 5));

        assertFalse(response.templates().isEmpty());
        assertTrue(response.templates().stream().anyMatch(template -> template.feature().equals("VLAN")));
        assertTrue(response.warnings().isEmpty());
    }

    @Test
    void shouldReturnWarningWhenNoTemplateMatches() {
        var response = tool.suggestConfigTemplate(
                new ConfigTemplateTool.ConfigTemplateRequest("FIREWALL", "VPN", "generic", 5));

        assertTrue(response.templates().isEmpty());
        assertFalse(response.warnings().isEmpty());
    }

    @Test
    void shouldNotContainExecutionActions() {
        var response = tool.suggestConfigTemplate(
                new ConfigTemplateTool.ConfigTemplateRequest(null, "ACL", null, 5));

        String allText = response.templates().toString().toLowerCase();
        assertFalse(allText.contains("execute"));
        assertFalse(allText.contains("apply configuration"));
        assertFalse(allText.contains("workspace"));
    }
}
