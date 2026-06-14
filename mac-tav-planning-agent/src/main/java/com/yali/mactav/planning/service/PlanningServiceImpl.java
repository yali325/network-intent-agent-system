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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default PlanningServiceImpl that runs Parser -> Validator.
 *
 * <p>The implementation deliberately avoids model calls and Workspace writes.
 * A future real PlanningAgent or A2A endpoint can call it after obtaining a
 * PlanningResponseSchema from the framework boundary.</p>
 */
public class PlanningServiceImpl implements PlanningService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlanningServiceImpl.class);

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
        LOGGER.info(
                "Planning parser start taskId={}, traceId={}",
                request == null ? null : request.getTaskId(),
                request == null ? null : request.getTraceId());
        long parserStart = System.nanoTime();
        NetworkPlan plan = parser.parse(schema, toContext(request));
        LOGGER.info(
                "Planning parser completed taskId={}, traceId={}, durationMs={}, nodeCount={}, linkCount={}",
                request == null ? null : request.getTaskId(),
                request == null ? null : request.getTraceId(),
                elapsedMillis(parserStart),
                topologyNodeCount(plan),
                topologyLinkCount(plan));
        LOGGER.info(
                "Planning validator start taskId={}, traceId={}",
                request == null ? null : request.getTaskId(),
                request == null ? null : request.getTraceId());
        long validatorStart = System.nanoTime();
        NetworkPlan validated = validator.validateAndReturn(plan);
        LOGGER.info(
                "Planning validator completed taskId={}, traceId={}, durationMs={}, zoneCount={}, policyCount={}",
                request == null ? null : request.getTaskId(),
                request == null ? null : request.getTraceId(),
                elapsedMillis(validatorStart),
                validated == null || validated.getZones() == null ? 0 : validated.getZones().size(),
                validated == null || validated.getSecurityPolicyPlan() == null
                        ? 0
                        : validated.getSecurityPolicyPlan().size());
        return validated;
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

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private int topologyNodeCount(NetworkPlan plan) {
        return plan == null || plan.getTopology() == null || plan.getTopology().getNodes() == null
                ? 0
                : plan.getTopology().getNodes().size();
    }

    private int topologyLinkCount(NetworkPlan plan) {
        return plan == null || plan.getTopology() == null || plan.getTopology().getLinks() == null
                ? 0
                : plan.getTopology().getLinks().size();
    }
}
