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
import com.yali.mactav.modelcore.service.NetworkArtifactService;
import com.yali.mactav.modelcore.service.NetworkWorkspaceService;
import com.yali.mactav.modelcore.service.WorkspaceChangeRecordService;
import com.yali.mactav.modelcore.service.WorkspaceEventService;
import com.yali.mactav.modelcore.service.WorkflowJobService;
import com.yali.mactav.orchestrator.job.InMemoryTaskRunLockService;
import com.yali.mactav.orchestrator.job.NoopTaskRunLockService;
import com.yali.mactav.orchestrator.job.RedisTaskRunLockService;
import com.yali.mactav.orchestrator.job.TaskRunLockService;
import com.yali.mactav.orchestrator.job.DefaultWorkflowJobRecoveryService;
import com.yali.mactav.orchestrator.job.WorkflowJobRecoveryService;
import com.yali.mactav.orchestrator.job.WorkflowAsyncExecutor;
import com.yali.mactav.orchestrator.remote.card.AgentCardRegistryClient;
import com.yali.mactav.orchestrator.remote.card.OfficialAgentCardRegistryClient;
import com.yali.mactav.orchestrator.remote.client.A2aClient;
import com.yali.mactav.orchestrator.remote.client.OfficialA2aClient;
import com.yali.mactav.orchestrator.remote.discovery.AgentDiscoveryClient;
import com.yali.mactav.orchestrator.remote.discovery.RegistryAgentDiscoveryClient;
import com.yali.mactav.orchestrator.remote.invoker.A2aResponseValidator;
import com.yali.mactav.orchestrator.remote.invoker.RemoteAgentInvoker;
import com.yali.mactav.orchestrator.remote.invoker.RemoteAgentTool;
import com.yali.mactav.orchestrator.service.ArtifactVersionSwitchService;
import com.yali.mactav.orchestrator.service.DefaultArtifactVersionSwitchService;
import com.yali.mactav.orchestrator.service.DefaultWorkflowQueryService;
import com.yali.mactav.orchestrator.service.DefaultWorkflowAsyncService;
import com.yali.mactav.orchestrator.service.MacTavWorkflowOrchestrator;
import com.yali.mactav.orchestrator.service.WorkflowAsyncService;
import com.yali.mactav.orchestrator.service.WorkflowQueryService;
import com.yali.mactav.orchestrator.service.WorkflowOrchestrator;
import java.time.Duration;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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
    public AgentCardRegistryClient officialAgentCardRegistryClient(
            AgentCardProvider agentCardProvider) {
        return new OfficialAgentCardRegistryClient(agentCardProvider);
    }

    @Bean
    public AgentDiscoveryClient agentDiscoveryClient(AgentCardRegistryClient registryClient) {
        return new RegistryAgentDiscoveryClient(registryClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public A2aClient officialA2aClient(AgentCardProvider agentCardProvider,
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

    @Bean
    @ConditionalOnMissingBean
    public WorkflowQueryService workflowQueryService(NetworkWorkspaceService workspaceService,
                                                     NetworkArtifactService artifactService,
                                                     WorkspaceEventService eventService,
                                                     WorkspaceChangeRecordService changeRecordService) {
        return new DefaultWorkflowQueryService(
                workspaceService,
                artifactService,
                eventService,
                changeRecordService);
    }

    @Bean
    @ConditionalOnMissingBean
    public ArtifactVersionSwitchService artifactVersionSwitchService(
            NetworkWorkspaceService workspaceService,
            NetworkArtifactService artifactService) {
        return new DefaultArtifactVersionSwitchService(workspaceService, artifactService);
    }

    @Bean
    @ConditionalOnMissingBean(name = "workflowTaskExecutor")
    public Executor workflowTaskExecutor(
            @Value("${mactav.async.core-pool-size:4}") int corePoolSize,
            @Value("${mactav.async.max-pool-size:16}") int maxPoolSize,
            @Value("${mactav.async.queue-capacity:200}") int queueCapacity,
            @Value("${mactav.async.thread-name-prefix:mactav-workflow-}") String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.initialize();
        return executor;
    }

    @Bean
    @Profile("!test & !inmemory")
    @ConditionalOnProperty(name = "mactav.task-lock.type", havingValue = "redis", matchIfMissing = true)
    public TaskRunLockService redisTaskRunLockService(
            StringRedisTemplate redisTemplate,
            @Value("${mactav.async.task-lock-ttl-ms:3600000}") long lockTtlMs) {
        return new RedisTaskRunLockService(redisTemplate, Duration.ofMillis(lockTtlMs));
    }

    @Bean
    @ConditionalOnMissingBean(TaskRunLockService.class)
    @ConditionalOnExpression(
            "'${mactav.task-lock.type:}' == 'inmemory' || "
                    + "'${spring.profiles.active:}'.contains('test') || "
                    + "'${spring.profiles.active:}'.contains('inmemory')")
    public TaskRunLockService inMemoryTaskRunLockService() {
        return new InMemoryTaskRunLockService();
    }

    @Bean
    @ConditionalOnMissingBean(TaskRunLockService.class)
    @ConditionalOnProperty(name = "mactav.task-lock.type", havingValue = "noop")
    public TaskRunLockService noopTaskRunLockService() {
        return new NoopTaskRunLockService();
    }

    @Bean
    @ConditionalOnMissingBean
    public WorkflowAsyncExecutor workflowAsyncExecutor(
            @Qualifier("workflowTaskExecutor") Executor executor,
            WorkflowOrchestrator workflowOrchestrator,
            WorkflowJobService workflowJobService,
            WorkspaceEventService eventService,
            TaskRunLockService lockService) {
        return new WorkflowAsyncExecutor(executor, workflowOrchestrator, workflowJobService, eventService, lockService);
    }

    @Bean
    @ConditionalOnMissingBean
    public WorkflowAsyncService workflowAsyncService(WorkflowQueryService workflowQueryService,
                                                     WorkflowJobService workflowJobService,
                                                     TaskRunLockService lockService,
                                                     WorkflowAsyncExecutor asyncExecutor) {
        return new DefaultWorkflowAsyncService(workflowQueryService, workflowJobService, lockService, asyncExecutor);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "mactav.async.recovery.enabled", havingValue = "true", matchIfMissing = true)
    public WorkflowJobRecoveryService workflowJobRecoveryService(
            WorkflowJobService workflowJobService,
            TaskRunLockService lockService,
            WorkspaceEventService eventService) {
        return new DefaultWorkflowJobRecoveryService(workflowJobService, lockService, eventService);
    }
}
