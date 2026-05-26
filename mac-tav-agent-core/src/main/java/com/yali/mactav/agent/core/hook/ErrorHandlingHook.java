package com.yali.mactav.agent.core.hook;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.yali.mactav.agent.core.agent.AgentUtils;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Hook-side marker for the common MAC-TAV error handling boundary.
 *
 * <p>The confirmed Spring AI Alibaba hook API exposes before/after callbacks
 * but no dedicated on-error callback. Runtime exception conversion therefore
 * remains in AgentUtils.callSchema while this hook provides a shared wrapper
 * method for agent modules that need explicit conversion.</p>
 */
public class ErrorHandlingHook extends AgentHook {

    @Override
    public String getName() {
        return "mactav-error-handling";
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
        return CompletableFuture.completedFuture(Map.of());
    }

    public BusinessException wrapModelError(String message, Throwable cause) {
        return AgentUtils.wrapException(ErrorCode.MODEL_CALL_FAILED, message, cause);
    }
}
