package com.yali.mactav.agent.core.hook;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Propagates safe trace metadata through the agent graph state.
 *
 * <p>This hook only mirrors taskId, traceId, stage, and agentName when they are
 * already present in RunnableConfig context. It must not create workflow state
 * or write NetworkWorkspace.</p>
 */
public class TraceHook extends AgentHook {

    @Override
    public String getName() {
        return "mactav-trace";
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
        Map<String, Object> trace = new LinkedHashMap<>();
        String agentName = getAgentName();
        trace.put("agentName", agentName == null || agentName.isBlank() ? "unknown" : agentName);
        HookLogSupport.readContext(config, "taskId").ifPresent(value -> trace.put("taskId", value));
        HookLogSupport.readContext(config, "traceId").ifPresent(value -> trace.put("traceId", value));
        HookLogSupport.readContext(config, "stage").ifPresent(value -> trace.put("stage", value));
        return CompletableFuture.completedFuture(trace.isEmpty() ? Map.of() : Map.of("mactavTrace", trace));
    }
}
