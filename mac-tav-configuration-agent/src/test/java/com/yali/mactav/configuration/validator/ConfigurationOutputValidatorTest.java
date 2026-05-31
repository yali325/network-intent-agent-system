package com.yali.mactav.configuration.validator;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.agent.core.validator.ValidationResult;
import com.yali.mactav.configuration.ConfigurationTestFixtures;
import com.yali.mactav.model.config.ConfigSet;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Offline validator tests for ConfigurationAgent domain and boundary rules.
 */
class ConfigurationOutputValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ConfigurationOutputValidator validator = new ConfigurationOutputValidator();

    @Test
    void validateShouldPassSampleConfigSet() {
        ValidationResult result = validator.validate(ConfigurationTestFixtures.validConfigSet(objectMapper));

        assertTrue(result.isValid(), () -> String.join("; ", result.getMessages()));
    }

    @Test
    void validateShouldFailWhenDeviceConfigsAreEmpty() {
        ConfigSet configSet = ConfigurationTestFixtures.validConfigSet(objectMapper);
        configSet.setDeviceConfigs(List.of());

        ValidationResult result = validator.validate(configSet);

        assertFalse(result.isValid());
        assertMessageContains(result, "deviceConfigs");
    }

    @Test
    void validateShouldFailWhenCommandBlocksAreEmpty() {
        ConfigSet configSet = ConfigurationTestFixtures.validConfigSet(objectMapper);
        configSet.getDeviceConfigs().get(0).setCommandBlocks(List.of());

        ValidationResult result = validator.validate(configSet);

        assertFalse(result.isValid());
        assertMessageContains(result, "commandBlocks");
    }

    @Test
    void validateShouldFailWhenCommandsAreEmpty() {
        ConfigSet configSet = ConfigurationTestFixtures.validConfigSet(objectMapper);
        configSet.getDeviceConfigs().get(0).getCommandBlocks().get(0).setCommands(List.of());

        ValidationResult result = validator.validate(configSet);

        assertFalse(result.isValid());
        assertMessageContains(result, "commands");
    }

    @Test
    void validateShouldFailWhenRollbackCommandsAreEmpty() {
        ConfigSet configSet = ConfigurationTestFixtures.validConfigSet(objectMapper);
        configSet.getDeviceConfigs().get(0).getCommandBlocks().get(0).setRollbackCommands(new ArrayList<>());

        ValidationResult result = validator.validate(configSet);

        assertFalse(result.isValid());
        assertMessageContains(result, "rollbackCommands");
    }

    @Test
    void validateShouldFailWhenCommandBlockTraceRefsAreMissing() {
        ConfigSet configSet = ConfigurationTestFixtures.validConfigSet(objectMapper);
        configSet.getDeviceConfigs().get(0).getCommandBlocks().get(0).setTraceRefs(null);

        ValidationResult result = validator.validate(configSet);

        assertFalse(result.isValid());
        assertMessageContains(result, "traceRefs");
    }

    @Test
    void validateShouldFailWhenCommandBlockTraceRefsCannotLinkPlanOrIntentRelation() {
        ConfigSet configSet = ConfigurationTestFixtures.validConfigSet(objectMapper);
        var refs = configSet.getDeviceConfigs().get(0).getCommandBlocks().get(0).getTraceRefs();
        refs.setPlanElementIds(List.of());
        refs.setIntentRelationIds(List.of());

        ValidationResult result = validator.validate(configSet);

        assertFalse(result.isValid());
        assertMessageContains(result, "planElementIds or intentRelationIds");
    }

    @Test
    void validateShouldFailWhenGenerationSourcesAreEmpty() {
        ConfigSet configSet = ConfigurationTestFixtures.validConfigSet(objectMapper);
        configSet.setGenerationSources(List.of());

        ValidationResult result = validator.validate(configSet);

        assertFalse(result.isValid());
        assertMessageContains(result, "generationSources");
    }

    @Test
    void validateShouldFailWhenSourceTypeIsNull() {
        ConfigSet configSet = ConfigurationTestFixtures.validConfigSet(objectMapper);
        configSet.getGenerationSources().get(0).setSourceType(null);

        ValidationResult result = validator.validate(configSet);

        assertFalse(result.isValid());
        assertMessageContains(result, "sourceType");
    }

    @Test
    void validateShouldFailWhenGenerationSummaryContainsNonStructuredConfigText() {
        ConfigSet configSet = ConfigurationTestFixtures.validConfigSet(objectMapper);
        configSet.setGenerationSummary("configText: interface Gi0/1\n ip address 10.1.0.1 255.255.255.0");

        ValidationResult result = validator.validate(configSet);

        assertFalse(result.isValid());
        assertMessageContains(result, "non-structured config text");
    }

    @Test
    void validateShouldFailWhenOutputClaimsExecutionOrVerificationBoundary() {
        ConfigSet configSet = ConfigurationTestFixtures.validConfigSet(objectMapper);
        configSet.getDeviceConfigs().get(0).getCommandBlocks().get(0)
                .setExplanation("配置已下发命令，并且验证通过。");

        ValidationResult result = validator.validate(configSet);

        assertFalse(result.isValid());
        assertMessageContains(result, "boundary content");
    }

    private void assertMessageContains(ValidationResult result, String expected) {
        assertTrue(result.getMessages().stream().anyMatch(message -> message.contains(expected)),
                () -> String.join("; ", result.getMessages()));
    }
}
