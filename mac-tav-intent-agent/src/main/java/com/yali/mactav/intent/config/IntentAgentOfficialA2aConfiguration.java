package com.yali.mactav.intent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.intent.a2a.IntentAgentInvokePayloadMapper;
import com.yali.mactav.intent.a2a.OfficialIntentA2aExecutor;
import com.yali.mactav.intent.agent.IntentAgent;
import io.a2a.server.agentexecution.AgentExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the official Spring AI Alibaba A2A executor for IntentAgent.
 *
 * <p>The SAA starter owns the JSON-RPC route, Agent Card, and Nacos registry.
 * This configuration only supplies the stage-specific executor that invokes the
 * validated IntentAgent chain and must not publish custom Agent Cards.</p>
 */
@Configuration(proxyBeanMethods = false)
public class IntentAgentOfficialA2aConfiguration {

    @Bean
    @ConditionalOnBean(IntentAgent.class)
    @ConditionalOnMissingBean(AgentExecutor.class)
    public AgentExecutor officialIntentA2aExecutor(IntentAgent intentAgent,
                                                   IntentAgentInvokePayloadMapper payloadMapper,
                                                   ObjectMapper objectMapper) {
        return new OfficialIntentA2aExecutor(intentAgent, payloadMapper, objectMapper);
    }
}
