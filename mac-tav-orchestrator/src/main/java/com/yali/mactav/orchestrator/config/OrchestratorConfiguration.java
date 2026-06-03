package com.yali.mactav.orchestrator.config;

import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yali.mactav.execution.config.ExecutionProperties;
import com.yali.mactav.execution.registry.ExecutionAdapterRegistry;
import com.yali.mactav.execution.registry.ExecutionAdapterRegistryFactory;
import com.yali.mactav.execution.service.DefaultExecutionService;
import com.yali.mactav.execution.service.ExecutionService;
import com.yali.mactav.modelcore.service.AgentExecutionRecordService;
import com.yali.mactav.modelcore.service.NetworkWorkspaceService;
import com.yali.mactav.modelcore.service.WorkspaceChangeRecordService;
import com.yali.mactav.orchestrator.remote.card.AgentCardRegistryClient;
import com.yali.mactav.orchestrator.remote.card.OfficialAgentCardRegistryClient;
import com.yali.mactav.orchestrator.remote.client.A2aClient;
import com.yali.mactav.orchestrator.remote.client.OfficialA2aClient;
import com.yali.mactav.orchestrator.remote.discovery.AgentDiscoveryClient;
import com.yali.mactav.orchestrator.remote.discovery.RegistryAgentDiscoveryClient;
import com.yali.mactav.orchestrator.remote.invoker.A2aResponseValidator;
import com.yali.mactav.orchestrator.remote.invoker.RemoteAgentInvoker;
import com.yali.mactav.orchestrator.remote.invoker.RemoteAgentTool;
import com.yali.mactav.orchestrator.service.MacTavWorkflowOrchestrator;
import com.yali.mactav.orchestrator.service.WorkflowOrchestrator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Minimal Spring wiring for Orchestrator remote-agent invocation and workflow coordination.
 *
 * <p>When Spring AI Alibaba's Nacos AgentCardProvider is available, official
 * SAA A2A adapters are preferred. Legacy HTTP JSON adapters remain fallback
 * only. This configuration does not scan or import concrete professional agent
 * modules.</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ExecutionProperties.class)
public class OrchestratorConfiguration {

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(name = "nacosAgentCardProvider")
    public AgentCardRegistryClient officialAgentCardRegistryClient(
            @Qualifier("nacosAgentCardProvider") AgentCardProvider agentCardProvider) {
        return new OfficialAgentCardRegistryClient(agentCardProvider);
    }

    @Bean
    public AgentDiscoveryClient agentDiscoveryClient(AgentCardRegistryClient registryClient) {
        return new RegistryAgentDiscoveryClient(registryClient);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(name = "nacosAgentCardProvider")
    public A2aClient officialA2aClient(@Qualifier("nacosAgentCardProvider") AgentCardProvider agentCardProvider,
                                       ObjectMapper objectMapper) {
        return new OfficialA2aClient(agentCardProvider, objectMapper);
    }


    @Bean
    @ConditionalOnMissingBean
    public A2aResponseValidator a2aResponseValidator() {
        return new A2aResponseValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    public RemoteAgentInvoker remoteAgentInvoker(AgentDiscoveryClient discoveryClient,
                                                 A2aClient a2aClient,
                                                 A2aResponseValidator responseValidator) {
        return new RemoteAgentInvoker(discoveryClient, a2aClient, responseValidator);
    }

    @Bean
    @ConditionalOnMissingBean
    public RemoteAgentTool remoteAgentTool(RemoteAgentInvoker invoker) {
        return new RemoteAgentTool(invoker);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExecutionAdapterRegistry executionAdapterRegistry(ExecutionProperties executionProperties) {
        return ExecutionAdapterRegistryFactory.defaultRegistry(executionProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExecutionService executionService(ExecutionAdapterRegistry executionAdapterRegistry) {
        return new DefaultExecutionService(executionAdapterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public WorkflowOrchestrator workflowOrchestrator(NetworkWorkspaceService workspaceService,
                                                     AgentExecutionRecordService executionRecordService,
                                                     WorkspaceChangeRecordService changeRecordService,
                                                     RemoteAgentInvoker remoteAgentInvoker,
                                                     ObjectMapper objectMapper,
                                                     ExecutionService executionService,
                                                     ExecutionProperties executionProperties) {
        return new MacTavWorkflowOrchestrator(
                workspaceService,
                executionRecordService,
                changeRecordService,
                remoteAgentInvoker,
                objectMapper,
                executionService,
                executionProperties);
    }
}
