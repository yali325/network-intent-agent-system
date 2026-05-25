package com.yali.mactav.orchestrator.remote.discovery;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.agent.AgentCard;
import com.yali.mactav.orchestrator.remote.card.AgentCardRegistryClient;
import java.util.Objects;

/**
 * AgentDiscoveryClient implementation backed by an AgentCardRegistryClient.
 *
 * <p>It converts missing registry entries into common errors. It does not query
 * concrete agent modules through Maven dependencies and does not manage task
 * state.</p>
 */
public class RegistryAgentDiscoveryClient implements AgentDiscoveryClient {

    private final AgentCardRegistryClient registryClient;

    public RegistryAgentDiscoveryClient(AgentCardRegistryClient registryClient) {
        this.registryClient = Objects.requireNonNull(registryClient, "registryClient must not be null");
    }

    @Override
    public AgentCard discover(String targetAgentName) {
        if (targetAgentName == null || targetAgentName.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Target agent name must not be blank");
        }
        return registryClient.findByAgentName(targetAgentName)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.AGENT_CARD_NOT_FOUND,
                        "Agent card not found: " + targetAgentName
                ));
    }
}
