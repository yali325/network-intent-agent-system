package com.yali.mactav.healing.agent;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.agent.core.agent.AgentUtils;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.healing.request.HealingAgentRequest;
import com.yali.mactav.healing.schema.HealingResponseSchema;
import com.yali.mactav.healing.service.HealingService;
import com.yali.mactav.model.healing.RepairPlan;
import java.util.Objects;

/**
 * Thin real HealingAgent wrapper for Spring AI Alibaba ReactAgent.
 */
public class HealingAgent {

    public static final String AGENT_NAME = "HealingAgent";

    public static final String REACT_AGENT_BEAN_NAME = "healingReactAgent";

    public static final String AGENT_DESCRIPTION = "MAC-TAV validation failure diagnosis and repair planning agent";

    private final ReactAgent reactAgent;

    private final ObjectMapper objectMapper;

    private final HealingService healingService;

    public HealingAgent(ReactAgent reactAgent, ObjectMapper objectMapper, HealingService healingService) {
        this.reactAgent = Objects.requireNonNull(reactAgent, "reactAgent must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.healingService = Objects.requireNonNull(healingService, "healingService must not be null");
    }

    public RepairPlan run(HealingAgentRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "HealingAgentRequest must not be null");
        }
        HealingResponseSchema schema = AgentUtils.callSchema(
                reactAgent,
                serializeRequest(request),
                HealingResponseSchema.class);
        return healingService.parse(schema, request);
    }

    private String serializeRequest(HealingAgentRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        }
        catch (JsonProcessingException ex) {
            throw AgentUtils.wrapException(
                    ErrorCode.AGENT_PARSE_FAILED,
                    "Failed to serialize HealingAgent request",
                    ex);
        }
    }
}
