package com.yali.mactav.orchestrator.remote.client;

import com.yali.mactav.model.a2a.A2aRequest;
import com.yali.mactav.model.a2a.A2aResponse;
import com.yali.mactav.model.agent.AgentCard;

/**
 * Transport boundary for issuing A2A calls to a remote professional agent.
 *
 * <p>The client adapts protocol details only. It must not construct prompts,
 * invoke ChatModel/ReactAgent, parse business DTOs, or update workspace state.</p>
 */
public interface A2aClient {

    A2aResponse call(A2aRequest request, AgentCard agentCard);
}
