package com.yali.mactav.planning.service;

import com.yali.mactav.agent.core.context.AgentRunContext;
import com.yali.mactav.agent.core.parser.AgentResponseParser;
import com.yali.mactav.agent.core.validator.AgentOutputValidator;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.planning.parser.PlanningResponseParser;
import com.yali.mactav.planning.request.PlanningAgentRequest;
import com.yali.mactav.planning.schema.PlanningResponseSchema;
import com.yali.mactav.planning.validator.PlanningOutputValidator;

/**
 * Default PlanningServiceImpl that runs Parser -> Validator.
 *
 * <p>The implementation deliberately avoids model calls and Workspace writes.
 * A future real PlanningAgent or A2A endpoint can call it after obtaining a
 * PlanningResponseSchema from the framework boundary.</p>
 */
public class PlanningServiceImpl implements PlanningService {

    private final AgentResponseParser<PlanningResponseSchema, NetworkPlan> parser;

    private final AgentOutputValidator<NetworkPlan> validator;

    public PlanningServiceImpl() {
        this(new PlanningResponseParser(), new PlanningOutputValidator());
    }

    public PlanningServiceImpl(AgentResponseParser<PlanningResponseSchema, NetworkPlan> parser,
                               AgentOutputValidator<NetworkPlan> validator) {
        this.parser = parser;
        this.validator = validator;
    }

    @Override
    public NetworkPlan parse(PlanningResponseSchema schema, PlanningAgentRequest request) {
        NetworkPlan plan = parser.parse(schema, toContext(request));
        return validator.validateAndReturn(plan);
    }

    private AgentRunContext toContext(PlanningAgentRequest request) {
        return AgentRunContext.builder()
                .taskId(request == null ? null : request.getTaskId())
                .stage(WorkflowStage.PLANNING)
                .version(request == null ? null : request.getPlanVersion())
                .traceId(request == null ? null : request.getTraceId())
                .userInput(request == null ? null : request.getRawText())
                .workspaceSnapshot(request == null ? null : request.getWorkspaceSnapshot())
                .createdBy(request == null ? null : request.getCreatedBy())
                .build();
    }
}
