package com.yali.mactav.orchestrator.remote.card;

import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardWrapper;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.agent.AgentCard;
import com.yali.mactav.model.agent.AgentHealthStatus;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AgentCard registry adapter backed by Spring AI Alibaba's official provider.
 *
 * <p>This Orchestrator-side adapter converts SAA AgentCard metadata into the
 * MAC-TAV shared DTO used by RemoteAgentInvoker. It does not query Nacos through
 * custom HTTP, call model APIs, or depend on any concrete agent module.</p>
 */
public class OfficialAgentCardRegistryClient implements AgentCardRegistryClient {

    private final AgentCardProvider agentCardProvider;

    public OfficialAgentCardRegistryClient(AgentCardProvider agentCardProvider) {
        this.agentCardProvider = agentCardProvider;
    }

    @Override
    public Optional<AgentCard> findByAgentName(String agentName) {
        if (agentName == null || agentName.isBlank()) {
            return Optional.empty();
        }
        try {
            AgentCardWrapper wrapper = agentCardProvider.supportGetAgentCardByName()
                    ? agentCardProvider.getAgentCard(agentName)
                    : agentCardProvider.getAgentCard();
            if (wrapper == null || wrapper.getAgentCard() == null) {
                return Optional.empty();
            }
            io.a2a.spec.AgentCard officialCard = wrapper.getAgentCard();
            if (officialCard.name() != null && !officialCard.name().equals(agentName)) {
                return Optional.empty();
            }
            return Optional.of(toModelCard(officialCard));
        }
        catch (BusinessException ex) {
            throw ex;
        }
        catch (RuntimeException ex) {
            throw new BusinessException(
                    ErrorCode.AGENT_DISCOVERY_FAILED,
                    "Spring AI Alibaba AgentCard discovery failed",
                    ex);
        }
    }

    @Override
    public List<AgentCard> listAvailableAgents() {
        return findByAgentName("IntentAgent").stream().toList();
    }

    private AgentCard toModelCard(io.a2a.spec.AgentCard officialCard) {
        LocalDateTime now = LocalDateTime.now();
        return AgentCard.builder()
                .agentName(officialCard.name())
                .description(officialCard.description())
                .serviceEndpoint(officialCard.url())
                .protocol("SAA_A2A_JSONRPC")
                .version(officialCard.version())
                .healthStatus(AgentHealthStatus.UNKNOWN)
                .metadata(metadata(officialCard))
                .createTime(now)
                .updateTime(now)
                .build();
    }

    private Map<String, String> metadata(io.a2a.spec.AgentCard officialCard) {
        Map<String, String> metadata = new HashMap<>();
        putIfPresent(metadata, "preferredTransport", officialCard.preferredTransport());
        putIfPresent(metadata, "protocolVersion", officialCard.protocolVersion());
        if (officialCard.provider() != null) {
            putIfPresent(metadata, "providerOrganization", officialCard.provider().organization());
            putIfPresent(metadata, "providerUrl", officialCard.provider().url());
        }
        return metadata;
    }

    private void putIfPresent(Map<String, String> metadata, String key, String value) {
        if (value != null && !value.isBlank()) {
            metadata.put(key, value);
        }
    }
}
