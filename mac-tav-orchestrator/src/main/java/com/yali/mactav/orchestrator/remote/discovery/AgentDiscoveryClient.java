package com.yali.mactav.orchestrator.remote.discovery;

import com.yali.mactav.model.agent.AgentCard;

/**
 * Discovers the AgentCard for a target remote agent name.
 *
 * <p>This is an orchestrator remote-adapter boundary, not business workflow
 * selection logic and not an Agent implementation.</p>
 */
public interface AgentDiscoveryClient {

    AgentCard discover(String targetAgentName);
}
