package com.yali.mactav.agent.core.hook;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentLogHook extends AgentHook {

    private static final Logger log = LoggerFactory.getLogger(AgentLogHook.class);

    @Override
    public String getName() {
        return "mactav-agent-log";
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
        log.info("Agent {} started task={} stage={} traceId={}",
                getAgentName(),
                HookSupport.traceValue("taskId", state, config),
                HookSupport.traceValue("stage", state, config),
                HookSupport.traceValue("traceId", state, config));
        return HookSupport.emptyResult();
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterAgent(OverAllState state, RunnableConfig config) {
        log.info("Agent {} finished task={} stage={} traceId={}",
                getAgentName(),
                HookSupport.traceValue("taskId", state, config),
                HookSupport.traceValue("stage", state, config),
                HookSupport.traceValue("traceId", state, config));
        return HookSupport.emptyResult();
    }
}
