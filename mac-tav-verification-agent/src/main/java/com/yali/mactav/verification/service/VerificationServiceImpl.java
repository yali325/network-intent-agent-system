package com.yali.mactav.verification.service;

import com.yali.mactav.agent.core.context.AgentRunContext;
import com.yali.mactav.agent.core.parser.AgentResponseParser;
import com.yali.mactav.agent.core.validator.AgentOutputValidator;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.verification.parser.VerificationResponseParser;
import com.yali.mactav.verification.request.VerificationAgentRequest;
import com.yali.mactav.verification.schema.VerificationResponseSchema;
import com.yali.mactav.verification.validator.VerificationOutputValidator;

/**
 * Default VerificationService implementation that runs Parser -> Validator.
 */
public class VerificationServiceImpl implements VerificationService {

    private final AgentResponseParser<VerificationResponseSchema, ValidationReport> parser;

    private final AgentOutputValidator<ValidationReport> validator;

    public VerificationServiceImpl() {
        this(new VerificationResponseParser(), new VerificationOutputValidator());
    }

    public VerificationServiceImpl(AgentResponseParser<VerificationResponseSchema, ValidationReport> parser,
                                   AgentOutputValidator<ValidationReport> validator) {
        this.parser = parser;
        this.validator = validator;
    }

    @Override
    public ValidationReport parse(VerificationResponseSchema schema, VerificationAgentRequest request) {
        ValidationReport report = parser.parse(schema, toContext(request));
        normalizeReport(report, request);
        return validator.validateAndReturn(report);
    }

    private AgentRunContext toContext(VerificationAgentRequest request) {
        return AgentRunContext.builder()
                .taskId(request == null ? null : request.getTaskId())
                .stage(WorkflowStage.VERIFICATION)
                .version(request == null ? null : request.getValidationVersion())
                .traceId(request == null ? null : request.getTraceId())
                .userInput(request == null ? null : request.getRawText())
                .workspaceSnapshot(request == null ? null : request.getWorkspaceSnapshot())
                .build();
    }

    private void normalizeReport(ValidationReport report, VerificationAgentRequest request) {
        if (report == null || request == null) {
            return;
        }
        report.setIntentVersion(request.getIntentVersion());
        report.setPlanVersion(request.getPlanVersion());
        report.setConfigVersion(request.getConfigVersion());
        report.setExecutionVersion(request.getExecutionVersion());
    }
}
