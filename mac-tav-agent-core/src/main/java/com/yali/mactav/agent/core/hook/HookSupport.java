package com.yali.mactav.agent.core.hook;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.yali.mactav.agent.core.context.AgentRunContext;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

final class HookSupport {

    private HookSupport() {
    }

    static CompletableFuture<Map<String, Object>> emptyResult() {
        return CompletableFuture.completedFuture(Map.of());
    }

    static Object traceValue(String key, OverAllState state, RunnableConfig config) {
        Object contextValue = traceValueFromContext(key, config);
        if (contextValue != null) {
            return contextValue;
        }
        if (state == null || state.data() == null) {
            return null;
        }
        return state.data().get(key);
    }

    private static Object traceValueFromContext(String key, RunnableConfig config) {
        if (config == null || config.context() == null) {
            return null;
        }
        Object direct = config.context().get(key);
        if (direct != null) {
            return direct;
        }
        Object runContext = config.context().get(AgentRunContext.CONTEXT_KEY);
        if (!(runContext instanceof AgentRunContext agentRunContext)) {
            return null;
        }
        return switch (key) {
            case TraceHook.TASK_ID_KEY -> agentRunContext.getTaskId();
            case TraceHook.STAGE_KEY -> agentRunContext.getStage();
            case TraceHook.TRACE_ID_KEY -> agentRunContext.getTraceId();
            default -> agentRunContext.getAttributes() == null ? null : agentRunContext.getAttributes().get(key);
        };
    }
}
