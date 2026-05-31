package com.yali.mactav.configuration.service;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.agent.core.validator.AgentValidationException;
import com.yali.mactav.configuration.ConfigurationTestFixtures;
import com.yali.mactav.configuration.parser.ConfigurationResponseParser;
import com.yali.mactav.configuration.schema.ConfigurationResponseSchema;
import com.yali.mactav.configuration.validator.ConfigurationOutputValidator;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.config.ConfigurationAgentInvokePayload;
import org.junit.jupiter.api.Test;

/**
 * Offline tests for ConfigurationService parser and validator orchestration.
 */
class ConfigurationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ConfigurationService service = new ConfigurationServiceImpl(
            new ConfigurationResponseParser(),
            new ConfigurationOutputValidator());

    @Test
    void parseShouldReturnValidatedConfigSet() {
        ConfigSet configSet = service.parse(ConfigurationTestFixtures.sampleSchema(objectMapper), payload());

        assertEquals(ConfigurationTestFixtures.TASK_ID, configSet.getTaskId());
        assertEquals(1, configSet.getPlanVersion());
        assertEquals(1, configSet.getConfigVersion());
        assertFalse(configSet.getDeviceConfigs().isEmpty());
        assertFalse(configSet.getGenerationSources().isEmpty());
    }

    @Test
    void parseShouldThrowWhenValidatorFails() {
        ConfigurationResponseSchema schema = ConfigurationTestFixtures.sampleSchema(objectMapper);
        schema.setDeviceConfigs(null);

        assertThrows(AgentValidationException.class, () -> service.parse(schema, payload()));
    }

    private ConfigurationAgentInvokePayload payload() {
        return ConfigurationAgentInvokePayload.builder()
                .taskId(ConfigurationTestFixtures.TASK_ID)
                .rawText("Generate configuration")
                .planVersion(1)
                .configVersion(1)
                .traceId(ConfigurationTestFixtures.TRACE_ID)
                .workspaceSnapshot("{}")
                .createdBy("unit-test")
                .build();
    }
}
