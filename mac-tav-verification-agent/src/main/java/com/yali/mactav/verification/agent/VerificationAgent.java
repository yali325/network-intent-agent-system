package com.yali.mactav.verification.agent;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.agent.core.agent.AgentUtils;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.verification.request.VerificationAgentRequest;
import com.yali.mactav.verification.schema.VerificationResponseSchema;
import com.yali.mactav.verification.service.VerificationService;
import java.util.Objects;

/**
 * Thin real VerificationAgent wrapper for Spring AI Alibaba ReactAgent.
 */
public class VerificationAgent {

    public static final String AGENT_NAME = "VerificationAgent";

    public static final String REACT_AGENT_BEAN_NAME = "verificationReactAgent";

    public static final String AGENT_DESCRIPTION = "MAC-TAV intent satisfaction verification agent";

    private final ReactAgent reactAgent;

    private final ObjectMapper objectMapper;

    private final VerificationService verificationService;

    public VerificationAgent(ReactAgent reactAgent,
                             ObjectMapper objectMapper,
                             VerificationService verificationService) {
        this.reactAgent = Objects.requireNonNull(reactAgent, "reactAgent must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.verificationService = Objects.requireNonNull(verificationService, "verificationService must not be null");
    }

    public ValidationReport run(VerificationAgentRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "VerificationAgentRequest must not be null");
        }
        VerificationResponseSchema schema = AgentUtils.callSchema(
                reactAgent,
                serializeRequest(request),
                VerificationResponseSchema.class);
        return verificationService.parse(schema, request);
    }

    private String serializeRequest(VerificationAgentRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        }
        catch (JsonProcessingException ex) {
            throw AgentUtils.wrapException(
                    ErrorCode.AGENT_PARSE_FAILED,
                    "Failed to serialize VerificationAgent request",
                    ex);
        }
    }
}
