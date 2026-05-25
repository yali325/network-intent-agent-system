package com.yali.mactav.orchestrator.remote.invoker;

import com.yali.mactav.model.a2a.A2aRequest;
import com.yali.mactav.model.a2a.A2aResponse;
import java.util.Objects;

/**
 * Thin tool-style facade over RemoteAgentInvoker for orchestrator workflows.
 *
 * <p>This class is not a concrete business agent and must not own orchestration
 * policy, workspace writes, prompt construction, or framework-specific model
 * calls.</p>
 */
public class RemoteAgentTool {

    private final RemoteAgentInvoker remoteAgentInvoker;

    public RemoteAgentTool(RemoteAgentInvoker remoteAgentInvoker) {
        this.remoteAgentInvoker = Objects.requireNonNull(remoteAgentInvoker, "remoteAgentInvoker must not be null");
    }

    public A2aResponse call(A2aRequest request) {
        return remoteAgentInvoker.invoke(request);
    }
}
