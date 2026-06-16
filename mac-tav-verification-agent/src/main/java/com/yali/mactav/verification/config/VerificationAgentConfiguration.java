package com.yali.mactav.verification.config;

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
import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.verification.a2a.VerificationAgentA2aExecutor;
import com.yali.mactav.verification.agent.VerificationAgent;
import com.yali.mactav.verification.parser.VerificationResponseParser;
import com.yali.mactav.verification.schema.VerificationResponseSchema;
import com.yali.mactav.verification.service.ExecutionEvidenceValidationBuilder;
import com.yali.mactav.verification.service.VerificationService;
import com.yali.mactav.verification.service.VerificationServiceImpl;
import com.yali.mactav.verification.tool.VerificationFactTool;
import com.yali.mactav.verification.validator.VerificationOutputValidator;
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
 * Spring bean configuration for the mac-tav-verification-agent module.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(VerificationAgentProperties.class)
public class VerificationAgentConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(VerificationAgentConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public VerificationResponseParser verificationResponseParser() {
        return new VerificationResponseParser();
    }

    @Bean
    @ConditionalOnMissingBean
    public VerificationOutputValidator verificationOutputValidator() {
        return new VerificationOutputValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    public VerificationService verificationService(
            AgentResponseParser<VerificationResponseSchema, ValidationReport> parser,
            AgentOutputValidator<ValidationReport> validator) {
        return new VerificationServiceImpl(parser, validator);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExecutionEvidenceValidationBuilder executionEvidenceValidationBuilder(
            ObjectMapper objectMapper,
            AgentOutputValidator<ValidationReport> validator) {
        return new ExecutionEvidenceValidationBuilder(objectMapper, validator);
    }

    @Bean
    @ConditionalOnMissingBean
    public VerificationFactTool verificationFactTool() {
        return new VerificationFactTool();
    }

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean(name = {VerificationAgent.AGENT_NAME, VerificationAgent.REACT_AGENT_BEAN_NAME})
    @ConditionalOnMissingBean(name = VerificationAgent.AGENT_NAME)
    public ReactAgent verificationReactAgent(ChatModel chatModel,
                                             VerificationFactTool verificationFactTool,
                                             VerificationAgentProperties properties) {
        return AgentUtils.reactAgentBuilder(VerificationAgent.AGENT_NAME,
                        VerificationAgent.AGENT_DESCRIPTION,
                        chatModel)
                .instruction(AgentUtils.loadInstruction(properties.effectivePromptPath()))
                .methodTools(verificationFactTool)
                .hooks(
                        new PlanHook(),
                        new AgentLogHook(),
                        new TraceHook(),
                        new ErrorHandlingHook(),
                        ModelCallLimitHook.builder()
                                .runLimit(properties.effectiveRunLimit())
                                .build())
                .outputKey("output")
                .outputType(VerificationResponseSchema.class)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public VerificationAgent verificationAgent(
            @Qualifier(VerificationAgent.REACT_AGENT_BEAN_NAME) ReactAgent verificationReactAgent,
            ObjectMapper objectMapper,
            VerificationService verificationService,
            ExecutionEvidenceValidationBuilder executionEvidenceValidationBuilder) {
        return new VerificationAgent(
                verificationReactAgent, objectMapper, verificationService, executionEvidenceValidationBuilder);
    }

    @Bean(name = "agentExecutor")
    public AgentExecutor verificationAgentA2aExecutor(VerificationAgent verificationAgent, ObjectMapper objectMapper) {
        return new VerificationAgentA2aExecutor(verificationAgent, objectMapper);
    }

    @Bean
    public ApplicationRunner agentExecutorInventoryLogger(Map<String, AgentExecutor> executors) {
        return args -> {
            boolean hasVerificationExecutor = executors.values().stream()
                    .anyMatch(VerificationAgentA2aExecutor.class::isInstance);
            boolean hasGraphExecutor = executors.values().stream()
                    .anyMatch(executor -> "com.alibaba.cloud.ai.a2a.core.server.GraphAgentExecutor"
                            .equals(executor.getClass().getName()));
            LOGGER.info(
                    "AgentExecutor bean inventory count={}, hasVerificationAgentA2aExecutor={}, hasGraphAgentExecutor={}",
                    executors.size(),
                    hasVerificationExecutor,
                    hasGraphExecutor);
            executors.forEach((name, executor) -> LOGGER.info(
                    "AgentExecutor bean: name={}, class={}",
                    name,
                    executor.getClass().getName()));
        };
    }
}
