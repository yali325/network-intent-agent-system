package com.yali.mactav.configuration.parser;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.configuration.ConfigurationTestFixtures;
import com.yali.mactav.configuration.schema.ConfigurationResponseSchema;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.config.GenerationSourceType;
import com.yali.mactav.model.enums.StageStatus;
import org.junit.jupiter.api.Test;

/**
 * Offline parser tests for ConfigurationResponseSchema -> ConfigSet conversion.
 */
class ConfigurationResponseParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ConfigurationResponseParser parser = new ConfigurationResponseParser();

    @Test
    void parseShouldConvertSampleJsonToConfigSet() {
        ConfigurationResponseSchema schema = ConfigurationTestFixtures.sampleSchema(objectMapper);
        ConfigSet configSet = parser.parse(schema, ConfigurationTestFixtures.context());

        assertEquals(ConfigurationTestFixtures.TASK_ID, configSet.getTaskId());
        assertEquals(1, configSet.getPlanVersion());
        assertEquals(1, configSet.getConfigVersion());
        assertEquals(StageStatus.SUCCESS, configSet.getStageStatus());
        assertNotNull(configSet.getCreateTime());
        assertNotNull(configSet.getUpdateTime());
        assertEquals("ConfigurationAgent", configSet.getCreatedBy());

        assertNotNull(configSet.getTargetEnvironment());
        assertEquals("generic", configSet.getTargetEnvironment().getVendor());
        assertEquals(3, configSet.getGenerationSources().size());
        assertEquals(GenerationSourceType.LLM, configSet.getGenerationSources().get(0).getSourceType());

        assertEquals(2, configSet.getDeviceConfigs().size());
        assertEquals("core-switch", configSet.getDeviceConfigs().get(0).getDeviceName());
        assertEquals(2, configSet.getDeviceConfigs().get(0).getCommandBlocks().size());
        assertFalse(configSet.getDeviceConfigs().get(0).getCommandBlocks().get(0).getRollbackCommands().isEmpty());
        assertFalse(configSet.getDeviceConfigs().get(0).getCommandBlocks().get(1)
                .getTraceRefs().getIntentRelationIds().isEmpty());

        assertEquals(1, configSet.getEndpointConfigs().size());
        assertNotNull(configSet.getRollbackPlan());
        assertEquals(2, configSet.getRollbackPlan().getRollbackBlocks().size());
        assertNotNull(configSet.getTraceRefs());
        assertFalse(configSet.getTraceRefs().getPlanElementIds().isEmpty());
    }

    @Test
    void parseShouldSetCreatedByFromContext() {
        var context = ConfigurationTestFixtures.context();
        context.setCreatedBy("unit-test-user");

        ConfigSet configSet = parser.parse(ConfigurationTestFixtures.sampleSchema(objectMapper), context);

        assertEquals("unit-test-user", configSet.getCreatedBy());
    }

    @Test
    void parseShouldNormalizeNullListsToEmpty() {
        ConfigurationResponseSchema schema = ConfigurationResponseSchema.builder()
                .generationSources(null)
                .deviceConfigs(null)
                .endpointConfigs(null)
                .warnings(null)
                .build();

        ConfigSet configSet = parser.parse(schema, ConfigurationTestFixtures.context());

        assertTrue(configSet.getGenerationSources().isEmpty());
        assertTrue(configSet.getDeviceConfigs().isEmpty());
        assertTrue(configSet.getEndpointConfigs().isEmpty());
        assertTrue(configSet.getWarnings().isEmpty());
    }

    @Test
    void parseShouldFailDuringDeserializationForUnknownSourceType() {
        String json = "{\"generationSources\":[{\"sourceType\":\"MODEL_GENERATED\"}]}";

        assertThrows(JsonMappingException.class,
                () -> objectMapper.readValue(json, ConfigurationResponseSchema.class));
    }
}
