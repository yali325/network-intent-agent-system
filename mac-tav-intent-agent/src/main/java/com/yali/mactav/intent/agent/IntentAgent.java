package com.yali.mactav.intent.agent;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.agent.core.agent.AgentUtils;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.intent.request.IntentAgentRequest;
import com.yali.mactav.intent.schema.IntentResponseSchema;
import com.yali.mactav.intent.service.IntentService;
import com.yali.mactav.model.intent.NetworkIntent;
import java.util.Objects;

/**
 * Thin real IntentAgent wrapper for Spring AI Alibaba ReactAgent.
 *
 * <p>The wrapper owns model invocation and the IntentAgent-local tool list, then
 * delegates ResponseSchema -> Parser -> DTO -> Validator to IntentService. It
 * must not return raw model text, write NetworkWorkspace, advance task state, or
 * depend on Web/Orchestrator/Model Core.</p>
 */
public class IntentAgent {

    public static final String AGENT_NAME = "IntentAgent";

    public static final String AGENT_DESCRIPTION = "MAC-TAV business intent understanding agent";

    private final ReactAgent reactAgent;

    private final ObjectMapper objectMapper;

    private final IntentService intentService;

    public IntentAgent(ReactAgent reactAgent,
                       ObjectMapper objectMapper,
                       IntentService intentService) {
        this.reactAgent = Objects.requireNonNull(reactAgent, "reactAgent must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.intentService = Objects.requireNonNull(intentService, "intentService must not be null");
    }

    public NetworkIntent run(IntentAgentRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "IntentAgentRequest must not be null");
        }
        String input = serializeRequest(request);
        IntentResponseSchema schema = AgentUtils.callSchema(reactAgent, input, IntentResponseSchema.class);
        return intentService.parse(schema, request);
    }

    private String serializeRequest(IntentAgentRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        }
        catch (JsonProcessingException ex) {
            throw AgentUtils.wrapException(
                    ErrorCode.AGENT_PARSE_FAILED,
                    "Failed to serialize IntentAgent request",
                    ex
            );
        }
    }
}
