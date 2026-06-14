package com.yali.mactav.configuration.config;

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
import com.yali.mactav.configuration.a2a.ConfigurationAgentA2aExecutor;
import com.yali.mactav.configuration.agent.ConfigurationAgent;
import com.yali.mactav.configuration.parser.ConfigurationResponseParser;
import com.yali.mactav.configuration.schema.ConfigurationResponseSchema;
import com.yali.mactav.configuration.service.ConfigurationService;
import com.yali.mactav.configuration.service.ConfigurationServiceImpl;
import com.yali.mactav.configuration.tool.ConfigTemplateTool;
import com.yali.mactav.configuration.tool.RagCommandSearchTool;
import com.yali.mactav.configuration.validator.ConfigurationOutputValidator;
import com.yali.mactav.model.config.ConfigSet;
import io.a2a.server.agentexecution.AgentExecutor;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring bean configuration for the mac-tav-configuration-agent module.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ConfigurationAgentProperties.class)
public class ConfigurationAgentConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationAgentConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public ConfigurationResponseParser configurationResponseParser() {
        return new ConfigurationResponseParser();
    }

    @Bean
    @ConditionalOnMissingBean
    public ConfigurationOutputValidator configurationOutputValidator() {
        return new ConfigurationOutputValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    public ConfigurationService configurationService(
            AgentResponseParser<ConfigurationResponseSchema, ConfigSet> parser,
            AgentOutputValidator<ConfigSet> validator) {
        return new ConfigurationServiceImpl(parser, validator);
    }

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean(name = ConfigurationAgent.REACT_AGENT_BEAN_NAME)
    @ConditionalOnMissingBean(name = ConfigurationAgent.REACT_AGENT_BEAN_NAME)
    public ReactAgent configurationReactAgent(ChatModel chatModel,
                                             ConfigTemplateTool configTemplateTool,
                                             RagCommandSearchTool ragCommandSearchTool,
                                             ConfigurationAgentProperties properties) {
        return AgentUtils.reactAgentBuilder(ConfigurationAgent.AGENT_NAME,
                        ConfigurationAgent.AGENT_DESCRIPTION,
                        chatModel)
                .instruction(AgentUtils.loadInstruction(properties.effectivePromptPath()))
                .methodTools(configTemplateTool, ragCommandSearchTool)
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
                .outputType(ConfigurationResponseSchema.class)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public ConfigurationAgent configurationAgent(
            @Qualifier(ConfigurationAgent.REACT_AGENT_BEAN_NAME) ReactAgent configurationReactAgent,
            ObjectMapper objectMapper,
            ConfigurationService configurationService) {
        return new ConfigurationAgent(configurationReactAgent, objectMapper, configurationService);
    }

    @Bean(name = "agentExecutor")
    public AgentExecutor configurationAgentA2aExecutor(ConfigurationAgent configurationAgent, ObjectMapper objectMapper) {
        return new ConfigurationAgentA2aExecutor(configurationAgent, objectMapper);
    }

    @Bean
    public ApplicationRunner agentExecutorInventoryLogger(Map<String, AgentExecutor> executors) {
        return args -> {
            boolean hasConfigurationExecutor = executors.values().stream()
                    .anyMatch(ConfigurationAgentA2aExecutor.class::isInstance);
            boolean hasGraphExecutor = executors.values().stream()
                    .anyMatch(executor -> "com.alibaba.cloud.ai.a2a.core.server.GraphAgentExecutor"
                            .equals(executor.getClass().getName()));
            LOGGER.info(
                    "AgentExecutor bean inventory count={}, hasConfigurationAgentA2aExecutor={}, hasGraphAgentExecutor={}",
                    executors.size(),
                    hasConfigurationExecutor,
                    hasGraphExecutor);
            executors.forEach((name, executor) -> LOGGER.info(
                    "AgentExecutor bean: name={}, class={}",
                    name,
                    executor.getClass().getName()));
        };
    }
}
