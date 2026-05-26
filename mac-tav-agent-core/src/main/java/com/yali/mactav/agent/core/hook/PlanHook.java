package com.yali.mactav.agent.core.hook;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Records lightweight agent lifecycle markers for later execution summaries.
 *
 * <p>PlanHook does not create network plans and is not related to
 * PlanningAgent. It only marks the start and finish time of an agent run inside
 * the graph state.</p>
 */
public class PlanHook extends AgentHook {

    @Override
    public String getName() {
        return "mactav-plan-marker";
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
        return CompletableFuture.completedFuture(Map.of(
                "mactavAgentStartedAt", LocalDateTime.now().toString()
        ));
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterAgent(OverAllState state, RunnableConfig config) {
        return CompletableFuture.completedFuture(Map.of(
                "mactavAgentFinishedAt", LocalDateTime.now().toString()
        ));
    }
}
