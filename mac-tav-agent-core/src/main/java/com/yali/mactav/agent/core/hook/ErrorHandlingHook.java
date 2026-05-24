package com.yali.mactav.agent.core.hook;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.yali.mactav.agent.core.agent.AgentUtils;
import com.yali.mactav.common.exception.BusinessException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ErrorHandlingHook extends AgentHook {

    @Override
    public String getName() {
        return "mactav-error-handling";
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
        return HookSupport.emptyResult();
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterAgent(OverAllState state, RunnableConfig config) {
        return HookSupport.emptyResult();
    }

    public BusinessException wrap(String errorCode, String message, Throwable cause) {
        return AgentUtils.wrapException(errorCode, message, cause);
    }
}
