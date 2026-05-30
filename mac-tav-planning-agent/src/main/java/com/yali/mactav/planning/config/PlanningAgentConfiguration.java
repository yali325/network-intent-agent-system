package com.yali.mactav.planning.config;

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
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.planning.agent.PlanningAgent;
import com.yali.mactav.planning.parser.PlanningResponseParser;
import com.yali.mactav.planning.schema.PlanningResponseSchema;
import com.yali.mactav.planning.service.PlanningService;
import com.yali.mactav.planning.service.PlanningServiceImpl;
import com.yali.mactav.planning.tool.AddressPlanningTool;
import com.yali.mactav.planning.tool.PlanningPlaybookTool;
import com.yali.mactav.planning.tool.TopologyTemplateTool;
import com.yali.mactav.planning.tool.VlanPlanningTool;
import com.yali.mactav.planning.validator.PlanningOutputValidator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring bean configuration for the mac-tav-planning-agent module.
 *
 * <p>This configuration wires the local PlanningAgent chain and registers the
 * named ReactAgent Bean expected by Spring AI Alibaba official A2A server
 * auto-configuration. A2A routes, Agent Card exposure, and Nacos registry are
 * provided by the SAA starter and application.yml, not by custom controllers.</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PlanningAgentProperties.class)
public class PlanningAgentConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PlanningResponseParser planningResponseParser() {
        return new PlanningResponseParser();
    }

    @Bean
    @ConditionalOnMissingBean
    public PlanningOutputValidator planningOutputValidator() {
        return new PlanningOutputValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    public PlanningService planningService(
            AgentResponseParser<PlanningResponseSchema, NetworkPlan> parser,
            AgentOutputValidator<NetworkPlan> validator) {
        return new PlanningServiceImpl(parser, validator);
    }

    @Bean
    @ConditionalOnMissingBean
    public AddressPlanningTool addressPlanningTool() {
        return new AddressPlanningTool();
    }

    @Bean
    @ConditionalOnMissingBean
    public VlanPlanningTool vlanPlanningTool() {
        return new VlanPlanningTool();
    }

    @Bean
    @ConditionalOnMissingBean
    public TopologyTemplateTool topologyTemplateTool() {
        return new TopologyTemplateTool();
    }

    @Bean
    @ConditionalOnMissingBean
    public PlanningPlaybookTool planningPlaybookTool() {
        return new PlanningPlaybookTool();
    }

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean(name = PlanningAgent.AGENT_NAME)
    @ConditionalOnMissingBean(name = PlanningAgent.AGENT_NAME)
    public ReactAgent planningReactAgent(ChatModel chatModel,
                                         AddressPlanningTool addressPlanningTool,
                                         VlanPlanningTool vlanPlanningTool,
                                         TopologyTemplateTool topologyTemplateTool,
                                         PlanningPlaybookTool planningPlaybookTool,
                                         PlanningAgentProperties properties) {
        return AgentUtils.reactAgentBuilder(PlanningAgent.AGENT_NAME, PlanningAgent.AGENT_DESCRIPTION, chatModel)
                .instruction(AgentUtils.loadInstruction(properties.effectivePromptPath()))
                .methodTools(addressPlanningTool, vlanPlanningTool,
                        topologyTemplateTool, planningPlaybookTool)
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
                .outputType(PlanningResponseSchema.class)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public PlanningAgent planningAgent(@Qualifier(PlanningAgent.AGENT_NAME) ReactAgent planningReactAgent,
                                       ObjectMapper objectMapper,
                                       PlanningService planningService) {
        return new PlanningAgent(planningReactAgent, objectMapper, planningService);
    }
}
