package com.yali.mactav.agent.core.hook;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TraceHook extends AgentHook {

    public static final String TASK_ID_KEY = "taskId";
    public static final String STAGE_KEY = "stage";
    public static final String TRACE_ID_KEY = "traceId";

    @Override
    public String getName() {
        return "mactav-trace";
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
        Map<String, Object> trace = new LinkedHashMap<>();
        putIfPresent(trace, TASK_ID_KEY, HookSupport.traceValue(TASK_ID_KEY, state, config));
        putIfPresent(trace, STAGE_KEY, HookSupport.traceValue(STAGE_KEY, state, config));
        putIfPresent(trace, TRACE_ID_KEY, HookSupport.traceValue(TRACE_ID_KEY, state, config));
        return CompletableFuture.completedFuture(trace);
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }
}
