package com.yali.mactav.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.agent.core.context.AgentRunContext;
import com.yali.mactav.configuration.parser.ConfigurationResponseParser;
import com.yali.mactav.configuration.schema.ConfigurationResponseSchema;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.enums.WorkflowStage;
import java.io.IOException;

/**
 * Fixed offline fixtures for ConfigurationAgent parser and validator tests.
 */
public final class ConfigurationTestFixtures {

    public static final String TASK_ID = "task-enterprise-office-guest";

    public static final String TRACE_ID = "trace-enterprise-office-guest";

    public static final String SAMPLE_JSON = "/samples/configuration-response-enterprise-office-guest.json";

    private ConfigurationTestFixtures() {
    }

    public static AgentRunContext context() {
        return AgentRunContext.builder()
                .taskId(TASK_ID)
                .stage(WorkflowStage.CONFIGURATION)
                .version(1)
                .traceId(TRACE_ID)
                .userInput("Generate structured configuration from the current NetworkPlan.")
                .workspaceSnapshot("{}")
                .build();
    }

    public static ConfigurationResponseSchema sampleSchema(ObjectMapper mapper) {
        try (var stream = ConfigurationTestFixtures.class.getResourceAsStream(SAMPLE_JSON)) {
            if (stream == null) {
                throw new IllegalStateException("Missing sample JSON: " + SAMPLE_JSON);
            }
            return mapper.readValue(stream, ConfigurationResponseSchema.class);
        }
        catch (IOException ex) {
            throw new IllegalStateException("Failed to read sample JSON", ex);
        }
    }

    public static ConfigSet validConfigSet(ObjectMapper mapper) {
        return new ConfigurationResponseParser().parse(sampleSchema(mapper), context());
    }
}
