package com.yali.mactav.orchestrator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yali.mactav.modelcore.service.AgentExecutionRecordService;
import com.yali.mactav.modelcore.service.NetworkWorkspaceService;
import com.yali.mactav.orchestrator.remote.card.AgentCardRegistryClient;
import com.yali.mactav.orchestrator.remote.card.NacosAgentCardRegistryClient;
import com.yali.mactav.orchestrator.remote.client.A2aClient;
import com.yali.mactav.orchestrator.remote.client.HttpA2aClient;
import com.yali.mactav.orchestrator.remote.discovery.AgentDiscoveryClient;
import com.yali.mactav.orchestrator.remote.discovery.RegistryAgentDiscoveryClient;
import com.yali.mactav.orchestrator.remote.invoker.A2aResponseValidator;
import com.yali.mactav.orchestrator.remote.invoker.RemoteAgentInvoker;
import com.yali.mactav.orchestrator.remote.invoker.RemoteAgentTool;
import com.yali.mactav.orchestrator.service.MacTavWorkflowOrchestrator;
import com.yali.mactav.orchestrator.service.WorkflowOrchestrator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Minimal Spring wiring for Orchestrator remote-agent invocation and workflow coordination.
 *
 * <p>The configuration assembles adapters and services only. It does not scan
 * or import concrete professional agent modules.</p>
 */
@Configuration(proxyBeanMethods = false)
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
    public AgentCardRegistryClient agentCardRegistryClient(
            @Value("${mactav.nacos.server-addr:http://127.0.0.1:8848}") String serverAddr,
            @Value("${mactav.agent-card.group:MAC_TAV_AGENT_CARDS}") String group,
            ObjectMapper objectMapper) {
        return new NacosAgentCardRegistryClient(serverAddr, group, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentDiscoveryClient agentDiscoveryClient(AgentCardRegistryClient registryClient) {
        return new RegistryAgentDiscoveryClient(registryClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public A2aClient a2aClient(ObjectMapper objectMapper) {
        return new HttpA2aClient(objectMapper);
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
    public WorkflowOrchestrator workflowOrchestrator(NetworkWorkspaceService workspaceService,
                                                     AgentExecutionRecordService executionRecordService,
                                                     RemoteAgentInvoker remoteAgentInvoker,
                                                     ObjectMapper objectMapper) {
        return new MacTavWorkflowOrchestrator(
                workspaceService,
                executionRecordService,
                remoteAgentInvoker,
                objectMapper);
    }
}
