package com.yali.mactav.agent.core.hook;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PlanHook extends AgentHook {

    @Override
    public String getName() {
        return "mactav-plan";
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
        return HookSupport.emptyResult();
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterAgent(OverAllState state, RunnableConfig config) {
        return HookSupport.emptyResult();
    }
}
