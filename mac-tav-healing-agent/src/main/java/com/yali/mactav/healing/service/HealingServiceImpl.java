package com.yali.mactav.healing.service;

import com.yali.mactav.agent.core.context.AgentRunContext;
import com.yali.mactav.agent.core.parser.AgentResponseParser;
import com.yali.mactav.agent.core.validator.AgentOutputValidator;
import com.yali.mactav.healing.parser.HealingResponseParser;
import com.yali.mactav.healing.request.HealingAgentRequest;
import com.yali.mactav.healing.schema.HealingResponseSchema;
import com.yali.mactav.healing.validator.HealingOutputValidator;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.healing.RepairPlan;

/**
 * Default HealingService implementation that runs Parser -> Validator.
 */
public class HealingServiceImpl implements HealingService {

    private final AgentResponseParser<HealingResponseSchema, RepairPlan> parser;

    private final AgentOutputValidator<RepairPlan> validator;

    public HealingServiceImpl() {
        this(new HealingResponseParser(), new HealingOutputValidator());
    }

    public HealingServiceImpl(AgentResponseParser<HealingResponseSchema, RepairPlan> parser,
                              AgentOutputValidator<RepairPlan> validator) {
        this.parser = parser;
        this.validator = validator;
    }

    @Override
    public RepairPlan parse(HealingResponseSchema schema, HealingAgentRequest request) {
        RepairPlan plan = parser.parse(schema, toContext(request));
        normalizePlan(plan, request);
        return validator.validateAndReturn(plan);
    }

    private AgentRunContext toContext(HealingAgentRequest request) {
        return AgentRunContext.builder()
                .taskId(request == null ? null : request.getTaskId())
                .stage(WorkflowStage.HEALING)
                .version(request == null ? null : request.getRepairVersion())
                .traceId(request == null ? null : request.getTraceId())
                .userInput(request == null ? null : request.getRawText())
                .workspaceSnapshot(request == null ? null : request.getWorkspaceSnapshot())
                .createdBy(request == null ? null : request.getCreatedBy())
                .build();
    }

    private void normalizePlan(RepairPlan plan, HealingAgentRequest request) {
        if (plan == null || request == null) {
            return;
        }
        plan.setValidationVersion(request.getValidationVersion());
    }
}
