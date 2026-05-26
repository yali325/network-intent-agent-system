package com.yali.mactav.agent.core.hook;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Safe execution log hook for real Spring AI Alibaba agents.
 *
 * <p>The hook records lifecycle summaries only. It belongs to agent-core and
 * must not persist workspace state or log raw model requests, API keys, or
 * external credentials.</p>
 */
public class AgentLogHook extends AgentHook {

    private static final Logger LOGGER = Logger.getLogger(AgentLogHook.class.getName());

    @Override
    public String getName() {
        return "mactav-agent-log";
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
        LOGGER.log(Level.FINE, () -> "Agent started: " + HookLogSupport.safeSummary(getAgentName(), state, config));
        return CompletableFuture.completedFuture(Map.of());
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterAgent(OverAllState state, RunnableConfig config) {
        LOGGER.log(Level.FINE, () -> "Agent finished: " + HookLogSupport.safeSummary(getAgentName(), state, config));
        return CompletableFuture.completedFuture(Map.of());
    }
}
