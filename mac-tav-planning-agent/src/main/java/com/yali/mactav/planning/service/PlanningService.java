package com.yali.mactav.planning.service;

import com.yali.mactav.planning.request.PlanningAgentRequest;
import com.yali.mactav.planning.schema.PlanningResponseSchema;
import com.yali.mactav.model.plan.NetworkPlan;

/**
 * Internal PlanningAgent module service for schema parsing and output validation.
 *
 * <p>This service belongs to mac-tav-planning-agent. It is not a Web controller,
 * not an Orchestrator local-agent shortcut, and must not write Workspace state.</p>
 */
public interface PlanningService {

    NetworkPlan parse(PlanningResponseSchema schema, PlanningAgentRequest request);
}
