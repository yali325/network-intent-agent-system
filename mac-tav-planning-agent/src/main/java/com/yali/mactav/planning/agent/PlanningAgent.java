package com.yali.mactav.planning.agent;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.agent.core.agent.AgentUtils;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.planning.request.PlanningAgentRequest;
import com.yali.mactav.planning.schema.PlanningResponseSchema;
import com.yali.mactav.planning.service.PlanningService;
import java.util.Objects;

/**
 * Thin real PlanningAgent wrapper for Spring AI Alibaba ReactAgent.
 *
 * <p>The wrapper owns model invocation and the PlanningAgent-local tool list, then
 * delegates ResponseSchema -> Parser -> DTO -> Validator to PlanningService. It
 * must not return raw model text, write NetworkWorkspace, advance task state, or
 * depend on Web/Orchestrator/Model Core.</p>
 */
public class PlanningAgent {

    public static final String AGENT_NAME = "PlanningAgent";

    public static final String AGENT_DESCRIPTION = "MAC-TAV network planning and topology design agent";

    private final ReactAgent reactAgent;

    private final ObjectMapper objectMapper;

    private final PlanningService planningService;

    public PlanningAgent(ReactAgent reactAgent,
                         ObjectMapper objectMapper,
                         PlanningService planningService) {
        this.reactAgent = Objects.requireNonNull(reactAgent, "reactAgent must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.planningService = Objects.requireNonNull(planningService, "planningService must not be null");
    }

    public NetworkPlan run(PlanningAgentRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "PlanningAgentRequest must not be null");
        }
        String input = serializeRequest(request);
        PlanningResponseSchema schema = AgentUtils.callSchema(reactAgent, input, PlanningResponseSchema.class);
        return planningService.parse(schema, request);
    }

    private String serializeRequest(PlanningAgentRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        }
        catch (JsonProcessingException ex) {
            throw AgentUtils.wrapException(
                    ErrorCode.AGENT_PARSE_FAILED,
                    "Failed to serialize PlanningAgent request",
                    ex
            );
        }
    }
}
