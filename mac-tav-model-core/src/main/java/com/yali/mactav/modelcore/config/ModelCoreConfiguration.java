package com.yali.mactav.modelcore.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.modelcore.artifact.ArtifactPayloadSerializer;
import com.yali.mactav.modelcore.artifact.NetworkArtifactFactory;
import com.yali.mactav.modelcore.repository.InMemoryAgentExecutionRecordRepository;
import com.yali.mactav.modelcore.repository.InMemoryNetworkArtifactRepository;
import com.yali.mactav.modelcore.repository.InMemoryNetworkWorkspaceRepository;
import com.yali.mactav.modelcore.repository.InMemoryWorkspaceChangeRecordRepository;
import com.yali.mactav.modelcore.repository.InMemoryWorkspaceEventRepository;
import com.yali.mactav.modelcore.service.AgentExecutionRecordService;
import com.yali.mactav.modelcore.service.InMemoryAgentExecutionRecordService;
import com.yali.mactav.modelcore.service.InMemoryNetworkArtifactService;
import com.yali.mactav.modelcore.service.InMemoryNetworkWorkspaceService;
import com.yali.mactav.modelcore.service.InMemoryWorkspaceChangeRecordService;
import com.yali.mactav.modelcore.service.InMemoryWorkspaceEventService;
import com.yali.mactav.modelcore.service.NetworkArtifactService;
import com.yali.mactav.modelcore.service.NetworkWorkspaceService;
import com.yali.mactav.modelcore.service.WorkspaceChangeRecordService;
import com.yali.mactav.modelcore.service.WorkspaceEventService;
import com.yali.mactav.modelcore.validator.ArtifactValidator;
import com.yali.mactav.modelcore.validator.WorkspaceStateValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Minimal Spring wiring for the in-memory Model Core implementation.
 *
 * <p>The configuration exposes state-management services as beans without
 * changing their current in-memory semantics or adding persistence adapters.</p>
 */
@Configuration(proxyBeanMethods = false)
public class ModelCoreConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public WorkspaceStateValidator workspaceStateValidator() {
        return new WorkspaceStateValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    public ArtifactValidator artifactValidator() {
        return new ArtifactValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    public ArtifactPayloadSerializer artifactPayloadSerializer(ObjectMapper objectMapper) {
        return new ArtifactPayloadSerializer(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public NetworkArtifactFactory networkArtifactFactory(ArtifactPayloadSerializer serializer) {
        return new NetworkArtifactFactory(serializer);
    }

    @Bean
    @ConditionalOnMissingBean
    public InMemoryNetworkWorkspaceRepository inMemoryNetworkWorkspaceRepository() {
        return new InMemoryNetworkWorkspaceRepository();
    }

    @Bean
    @ConditionalOnMissingBean
    public InMemoryNetworkArtifactRepository inMemoryNetworkArtifactRepository() {
        return new InMemoryNetworkArtifactRepository();
    }

    @Bean
    @ConditionalOnMissingBean
    public InMemoryWorkspaceEventRepository inMemoryWorkspaceEventRepository() {
        return new InMemoryWorkspaceEventRepository();
    }

    @Bean
    @ConditionalOnMissingBean
    public InMemoryWorkspaceChangeRecordRepository inMemoryWorkspaceChangeRecordRepository() {
        return new InMemoryWorkspaceChangeRecordRepository();
    }

    @Bean
    @ConditionalOnMissingBean
    public InMemoryAgentExecutionRecordRepository inMemoryAgentExecutionRecordRepository() {
        return new InMemoryAgentExecutionRecordRepository();
    }

    @Bean
    @ConditionalOnMissingBean
    public WorkspaceEventService workspaceEventService(InMemoryWorkspaceEventRepository repository,
                                                       WorkspaceStateValidator workspaceStateValidator) {
        return new InMemoryWorkspaceEventService(repository, workspaceStateValidator);
    }

    @Bean
    @ConditionalOnMissingBean
    public WorkspaceChangeRecordService workspaceChangeRecordService(
            InMemoryWorkspaceChangeRecordRepository changeRepository,
            InMemoryNetworkWorkspaceRepository workspaceRepository,
            WorkspaceStateValidator workspaceStateValidator) {
        return new InMemoryWorkspaceChangeRecordService(changeRepository, workspaceRepository, workspaceStateValidator);
    }

    @Bean
    @ConditionalOnMissingBean
    public NetworkArtifactService networkArtifactService(InMemoryNetworkArtifactRepository repository,
                                                         NetworkArtifactFactory artifactFactory,
                                                         ArtifactValidator artifactValidator,
                                                         WorkspaceStateValidator workspaceStateValidator) {
        return new InMemoryNetworkArtifactService(
                repository,
                artifactFactory,
                artifactValidator,
                workspaceStateValidator);
    }

    @Bean
    @ConditionalOnMissingBean
    public NetworkWorkspaceService networkWorkspaceService(
            InMemoryNetworkWorkspaceRepository workspaceRepository,
            NetworkArtifactService artifactService,
            WorkspaceEventService eventService,
            WorkspaceStateValidator workspaceStateValidator,
            ArtifactValidator artifactValidator) {
        return new InMemoryNetworkWorkspaceService(
                workspaceRepository,
                artifactService,
                eventService,
                workspaceStateValidator,
                artifactValidator);
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentExecutionRecordService agentExecutionRecordService(
            InMemoryAgentExecutionRecordRepository recordRepository,
            InMemoryNetworkWorkspaceRepository workspaceRepository,
            WorkspaceStateValidator workspaceStateValidator) {
        return new InMemoryAgentExecutionRecordService(recordRepository, workspaceRepository, workspaceStateValidator);
    }
}
