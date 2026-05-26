package com.yali.mactav.intent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yali.mactav.agent.core.parser.AgentResponseParser;
import com.yali.mactav.agent.core.validator.AgentOutputValidator;
import com.yali.mactav.intent.agent.IntentAgent;
import com.yali.mactav.intent.parser.IntentResponseParser;
import com.yali.mactav.intent.schema.IntentResponseSchema;
import com.yali.mactav.intent.service.IntentService;
import com.yali.mactav.intent.service.IntentServiceImpl;
import com.yali.mactav.intent.tool.IntentExtractTool;
import com.yali.mactav.intent.validator.IntentOutputValidator;
import com.yali.mactav.model.intent.NetworkIntent;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring bean configuration for the mac-tav-intent-agent module.
 *
 * <p>This configuration wires the local IntentAgent chain only. It deliberately
 * avoids Web controllers, A2A/Nacos service exposure, Orchestrator dependencies,
 * and Workspace writes.</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IntentAgentProperties.class)
public class IntentAgentConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IntentResponseParser intentResponseParser() {
        return new IntentResponseParser();
    }

    @Bean
    @ConditionalOnMissingBean
    public IntentOutputValidator intentOutputValidator() {
        return new IntentOutputValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    public IntentService intentService(
            AgentResponseParser<IntentResponseSchema, NetworkIntent> parser,
            AgentOutputValidator<NetworkIntent> validator) {
        return new IntentServiceImpl(parser, validator);
    }

    @Bean
    @ConditionalOnMissingBean
    public IntentExtractTool intentExtractTool() {
        return new IntentExtractTool();
    }

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    @ConditionalOnBean(ChatModel.class)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mactav.agents.intent", name = "enabled", havingValue = "true", matchIfMissing = true)
    public IntentAgent intentAgent(ChatModel chatModel,
                                   IntentExtractTool intentExtractTool,
                                   ObjectMapper objectMapper,
                                   IntentService intentService,
                                   IntentAgentProperties properties) {
        return new IntentAgent(chatModel, intentExtractTool, objectMapper, intentService, properties);
    }
}
