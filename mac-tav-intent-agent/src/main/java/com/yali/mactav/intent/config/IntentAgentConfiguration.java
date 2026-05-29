package com.yali.mactav.intent.config;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yali.mactav.agent.core.agent.AgentUtils;
import com.yali.mactav.agent.core.hook.AgentLogHook;
import com.yali.mactav.agent.core.hook.ErrorHandlingHook;
import com.yali.mactav.agent.core.hook.PlanHook;
import com.yali.mactav.agent.core.hook.TraceHook;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring bean configuration for the mac-tav-intent-agent module.
 *
 * <p>This configuration wires the local IntentAgent chain and registers the
 * named ReactAgent Bean expected by Spring AI Alibaba official A2A server
 * auto-configuration. A2A routes, Agent Card exposure, and Nacos registry are
 * provided by the SAA starter and application.yml, not by custom controllers.</p>
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

    @Bean(name = IntentAgent.AGENT_NAME)
    @ConditionalOnMissingBean(name = IntentAgent.AGENT_NAME)
    public ReactAgent intentReactAgent(ChatModel chatModel,
                                       IntentExtractTool intentExtractTool,
                                       IntentAgentProperties properties) {
        return AgentUtils.reactAgentBuilder(IntentAgent.AGENT_NAME, IntentAgent.AGENT_DESCRIPTION, chatModel)
                .instruction(AgentUtils.loadInstruction(properties.effectivePromptPath()))
                .methodTools(intentExtractTool)
                .hooks(
                        new PlanHook(),
                        new AgentLogHook(),
                        new TraceHook(),
                        new ErrorHandlingHook(),
                        ModelCallLimitHook.builder()
                                .runLimit(properties.effectiveRunLimit())
                                .build()
                )
                .outputKey("output")
                .outputType(IntentResponseSchema.class)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public IntentAgent intentAgent(@Qualifier(IntentAgent.AGENT_NAME) ReactAgent intentReactAgent,
                                   ObjectMapper objectMapper,
                                   IntentService intentService) {
        return new IntentAgent(intentReactAgent, objectMapper, intentService);
    }
}
