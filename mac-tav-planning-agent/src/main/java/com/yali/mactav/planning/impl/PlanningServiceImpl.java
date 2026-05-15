package com.yali.mactav.planning.impl;

import com.yali.mactav.agent.core.context.AgentContext;
import com.yali.mactav.agent.core.result.AgentResult;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.planning.agent.MockPlanningAgent;
import com.yali.mactav.planning.agent.PlanningAgent;
import com.yali.mactav.planning.service.PlanningService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class PlanningServiceImpl implements PlanningService {

    private final PlanningAgent planningAgent;

    public PlanningServiceImpl() {
        this(new MockPlanningAgent());
    }

    public PlanningServiceImpl(PlanningAgent planningAgent) {
        this.planningAgent = planningAgent;
    }

    @Override
    public NetworkPlan createPlan(NetworkIntent intent) {
        AgentContext context = AgentContext.of(intent == null ? null : intent.getTaskId(), null);
        AgentResult<NetworkPlan> result = planningAgent.execute(context, intent);
        if (!result.isSuccess()) {
            throw new IllegalStateException(result.getMessage());
        }
        return result.getData();
    }
}
