package com.yali.mactav.configuration.agent;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.agent.core.agent.AgentUtils;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.configuration.schema.ConfigurationResponseSchema;
import com.yali.mactav.configuration.service.ConfigurationService;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.config.ConfigurationAgentInvokePayload;
import java.util.Objects;

/**
 * Thin real ConfigurationAgent wrapper for Spring AI Alibaba ReactAgent.
 */
public class ConfigurationAgent {

    public static final String AGENT_NAME = "ConfigurationAgent";

    public static final String REACT_AGENT_BEAN_NAME = "configurationReactAgent";

    public static final String AGENT_DESCRIPTION = "MAC-TAV structured network configuration generation agent";

    private final ReactAgent reactAgent;

    private final ObjectMapper objectMapper;

    private final ConfigurationService configurationService;

    public ConfigurationAgent(ReactAgent reactAgent,
                              ObjectMapper objectMapper,
                              ConfigurationService configurationService) {
        this.reactAgent = Objects.requireNonNull(reactAgent, "reactAgent must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.configurationService = Objects.requireNonNull(configurationService, "configurationService must not be null");
    }

    public ConfigSet run(ConfigurationAgentInvokePayload payload) {
        if (payload == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "ConfigurationAgentInvokePayload must not be null");
        }
        String input = serializePayload(payload);
        ConfigurationResponseSchema schema = AgentUtils.callSchema(reactAgent, input, ConfigurationResponseSchema.class);
        return configurationService.parse(schema, payload);
    }

    private String serializePayload(ConfigurationAgentInvokePayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        }
        catch (JsonProcessingException ex) {
            throw AgentUtils.wrapException(
                    ErrorCode.AGENT_PARSE_FAILED,
                    "Failed to serialize ConfigurationAgent payload",
                    ex
            );
        }
    }
}
