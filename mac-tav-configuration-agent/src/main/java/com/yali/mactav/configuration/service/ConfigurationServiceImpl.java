package com.yali.mactav.configuration.service;

import com.yali.mactav.agent.core.context.AgentRunContext;
import com.yali.mactav.agent.core.parser.AgentResponseParser;
import com.yali.mactav.agent.core.validator.AgentOutputValidator;
import com.yali.mactav.configuration.schema.ConfigurationResponseSchema;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.config.ConfigurationAgentInvokePayload;
import com.yali.mactav.model.enums.WorkflowStage;

/**
 * Default ConfigurationService implementation that runs Parser -> Validator.
 */
public class ConfigurationServiceImpl implements ConfigurationService {

    private final AgentResponseParser<ConfigurationResponseSchema, ConfigSet> parser;

    private final AgentOutputValidator<ConfigSet> validator;

    public ConfigurationServiceImpl(AgentResponseParser<ConfigurationResponseSchema, ConfigSet> parser,
                                    AgentOutputValidator<ConfigSet> validator) {
        this.parser = parser;
        this.validator = validator;
    }

    @Override
    public ConfigSet parse(ConfigurationResponseSchema schema, ConfigurationAgentInvokePayload payload) {
        ConfigurationResponseSchema safeSchema = schema == null ? new ConfigurationResponseSchema() : schema;
        if (safeSchema.getPlanVersion() == null && payload != null) {
            safeSchema.setPlanVersion(payload.getPlanVersion());
        }
        if (safeSchema.getConfigVersion() == null && payload != null) {
            safeSchema.setConfigVersion(payload.getConfigVersion());
        }
        ConfigSet configSet = parser.parse(safeSchema, toContext(payload));
        return validator.validateAndReturn(configSet);
    }

    private AgentRunContext toContext(ConfigurationAgentInvokePayload payload) {
        return AgentRunContext.builder()
                .taskId(payload == null ? null : payload.getTaskId())
                .stage(WorkflowStage.CONFIGURATION)
                .version(payload == null ? null : payload.getConfigVersion())
                .traceId(payload == null ? null : payload.getTraceId())
                .userInput(payload == null ? null : payload.getRawText())
                .workspaceSnapshot(payload == null ? null : payload.getWorkspaceSnapshot())
                .createdBy(payload == null ? null : payload.getCreatedBy())
                .build();
    }
}
