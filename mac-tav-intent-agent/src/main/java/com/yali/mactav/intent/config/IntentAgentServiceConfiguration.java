package com.yali.mactav.intent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.intent.a2a.IntentAgentInvokePayloadMapper;
import com.yali.mactav.intent.card.IntentAgentCardFactory;
import com.yali.mactav.intent.card.IntentAgentCardPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for IntentAgent service exposure and discovery metadata.
 *
 * <p>It wires A2A support and AgentCard publication only. It does not introduce
 * frontend APIs, Orchestrator dependencies, or Workspace state writes.</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IntentAgentCardProperties.class)
public class IntentAgentServiceConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IntentAgentInvokePayloadMapper intentAgentInvokePayloadMapper() {
        return new IntentAgentInvokePayloadMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public IntentAgentCardFactory intentAgentCardFactory() {
        return new IntentAgentCardFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public IntentAgentCardPublisher intentAgentCardPublisher(IntentAgentCardFactory cardFactory,
                                                             IntentAgentCardProperties properties,
                                                             ObjectMapper objectMapper) {
        return new IntentAgentCardPublisher(cardFactory, properties, objectMapper);
    }
}
