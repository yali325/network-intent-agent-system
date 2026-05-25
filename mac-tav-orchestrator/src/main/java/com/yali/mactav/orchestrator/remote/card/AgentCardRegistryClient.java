package com.yali.mactav.orchestrator.remote.card;

import com.yali.mactav.model.agent.AgentCard;
import java.util.List;
import java.util.Optional;

/**
 * Registry-facing lookup boundary for remote AgentCard metadata.
 *
 * <p>This orchestrator-side interface hides Nacos/config discovery details.
 * Implementations must not perform business orchestration or write
 * NetworkWorkspace state.</p>
 */
public interface AgentCardRegistryClient {

    Optional<AgentCard> findByAgentName(String agentName);

    List<AgentCard> listAvailableAgents();
}
