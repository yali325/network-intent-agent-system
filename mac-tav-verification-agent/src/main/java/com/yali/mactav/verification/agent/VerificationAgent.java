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
import com.yali.mactav.verification.service.ExecutionEvidenceValidationBuilder;
import com.yali.mactav.verification.service.VerificationService;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thin real VerificationAgent wrapper for Spring AI Alibaba ReactAgent.
 */
public class VerificationAgent {

    private static final Logger LOGGER = LoggerFactory.getLogger(VerificationAgent.class);

    public static final String AGENT_NAME = "VerificationAgent";

    public static final String REACT_AGENT_BEAN_NAME = "verificationReactAgent";

    public static final String AGENT_DESCRIPTION = "MAC-TAV intent satisfaction verification agent";

    private final ReactAgent reactAgent;

    private final ObjectMapper objectMapper;

    private final VerificationService verificationService;

    private final ExecutionEvidenceValidationBuilder executionEvidenceValidationBuilder;

    public VerificationAgent(ReactAgent reactAgent,
                             ObjectMapper objectMapper,
                             VerificationService verificationService) {
        this(reactAgent, objectMapper, verificationService, null);
    }

    public VerificationAgent(ReactAgent reactAgent,
                             ObjectMapper objectMapper,
                             VerificationService verificationService,
                             ExecutionEvidenceValidationBuilder executionEvidenceValidationBuilder) {
        this.reactAgent = Objects.requireNonNull(reactAgent, "reactAgent must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.verificationService = Objects.requireNonNull(verificationService, "verificationService must not be null");
        this.executionEvidenceValidationBuilder = executionEvidenceValidationBuilder;
    }

    public ValidationReport run(VerificationAgentRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "VerificationAgentRequest must not be null");
        }
        if (executionEvidenceValidationBuilder != null
                && request.getExecutionReportJson() != null
                && !request.getExecutionReportJson().isBlank()) {
            LOGGER.info(
                    "VerificationAgent deterministic evidence validation start taskId={}, traceId={}, executionReportLength={}",
                    request.getTaskId(),
                    request.getTraceId(),
                    request.getExecutionReportJson().length());
            return executionEvidenceValidationBuilder.build(request);
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
