package com.yali.mactav.healing.config;

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
import com.yali.mactav.healing.agent.HealingAgent;
import com.yali.mactav.healing.parser.HealingResponseParser;
import com.yali.mactav.healing.schema.HealingResponseSchema;
import com.yali.mactav.healing.service.HealingService;
import com.yali.mactav.healing.service.HealingServiceImpl;
import com.yali.mactav.healing.tool.HealingDiagnosisTool;
import com.yali.mactav.healing.validator.HealingOutputValidator;
import com.yali.mactav.model.healing.RepairPlan;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring bean configuration for the mac-tav-healing-agent module.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HealingAgentProperties.class)
public class HealingAgentConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public HealingResponseParser healingResponseParser() {
        return new HealingResponseParser();
    }

    @Bean
    @ConditionalOnMissingBean
    public HealingOutputValidator healingOutputValidator() {
        return new HealingOutputValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    public HealingService healingService(
            AgentResponseParser<HealingResponseSchema, RepairPlan> parser,
            AgentOutputValidator<RepairPlan> validator,
            HealingDiagnosisTool healingDiagnosisTool,
            ObjectMapper objectMapper) {
        return new HealingServiceImpl(parser, validator, healingDiagnosisTool, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public HealingDiagnosisTool healingDiagnosisTool() {
        return new HealingDiagnosisTool();
    }

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean(name = {HealingAgent.AGENT_NAME, HealingAgent.REACT_AGENT_BEAN_NAME})
    @ConditionalOnMissingBean(name = HealingAgent.AGENT_NAME)
    public ReactAgent healingReactAgent(ChatModel chatModel,
                                        HealingDiagnosisTool healingDiagnosisTool,
                                        HealingAgentProperties properties) {
        return AgentUtils.reactAgentBuilder(HealingAgent.AGENT_NAME,
                        HealingAgent.AGENT_DESCRIPTION,
                        chatModel)
                .instruction(AgentUtils.loadInstruction(properties.effectivePromptPath()))
                .methodTools(healingDiagnosisTool)
                .hooks(
                        new PlanHook(),
                        new AgentLogHook(),
                        new TraceHook(),
                        new ErrorHandlingHook(),
                        ModelCallLimitHook.builder()
                                .runLimit(properties.effectiveRunLimit())
                                .build())
                .outputKey("output")
                .outputType(HealingResponseSchema.class)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public HealingAgent healingAgent(
            @Qualifier(HealingAgent.REACT_AGENT_BEAN_NAME) ReactAgent healingReactAgent,
            ObjectMapper objectMapper,
            HealingService healingService) {
        return new HealingAgent(healingReactAgent, objectMapper, healingService);
    }
}
