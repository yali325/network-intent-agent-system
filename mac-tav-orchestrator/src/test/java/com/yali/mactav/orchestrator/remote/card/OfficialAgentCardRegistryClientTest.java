package com.yali.mactav.orchestrator.remote.card;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardWrapper;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentProvider;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Offline contract tests for the official SAA AgentCardProvider adapter.
 *
 * <p>The provider is a local fixture only; the test does not start Nacos or
 * invoke remote agents.</p>
 */
class OfficialAgentCardRegistryClientTest {

    @Test
    void findByAgentNameShouldMapOfficialAgentCardToProjectDto() {
        OfficialAgentCardRegistryClient client = new OfficialAgentCardRegistryClient(provider());

        com.yali.mactav.model.agent.AgentCard found = client.findByAgentName("IntentAgent").orElseThrow();

        assertEquals("IntentAgent", found.getAgentName());
        assertEquals("SAA_A2A_JSONRPC", found.getProtocol());
        assertEquals("http://127.0.0.1:18081", found.getServiceEndpoint());
        assertEquals("yali", found.getMetadata().get("providerOrganization"));
    }

    private AgentCardProvider provider() {
        return new AgentCardProvider() {
            @Override
            public AgentCardWrapper getAgentCard() {
                return new AgentCardWrapper(agentCard());
            }

            @Override
            public AgentCardWrapper getAgentCard(String name) {
                return new AgentCardWrapper(agentCard());
            }

            @Override
            public boolean supportGetAgentCardByName() {
                return true;
            }
        };
    }

    private AgentCard agentCard() {
        return new AgentCard(
                "IntentAgent",
                "MAC-TAV business intent extraction agent",
                "http://127.0.0.1:18081",
                new AgentProvider("yali", "http://127.0.0.1:18081"),
                "1.0.0",
                null,
                new AgentCapabilities(false, false, false, List.of()),
                List.of("text"),
                List.of("text"),
                List.of(),
                false,
                Map.of(),
                List.of(),
                null,
                List.of(),
                "JSONRPC",
                "0.2.5");
    }
}
