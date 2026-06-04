package com.yali.mactav.modelcore.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.modelcore.artifact.ArtifactPayloadSerializer;
import com.yali.mactav.modelcore.artifact.NetworkArtifactFactory;
import com.yali.mactav.modelcore.assembler.MyBatisModelCoreAssembler;
import com.yali.mactav.modelcore.event.NoopWorkspaceEventPublisher;
import com.yali.mactav.modelcore.event.WorkspaceEventPublisher;
import com.yali.mactav.modelcore.event.redis.RedisWorkspaceEventPublisher;
import com.yali.mactav.modelcore.mapper.AgentExecutionRecordMapper;
import com.yali.mactav.modelcore.mapper.NetworkArtifactMapper;
import com.yali.mactav.modelcore.mapper.NetworkTaskMapper;
import com.yali.mactav.modelcore.mapper.NetworkWorkspaceStateMapper;
import com.yali.mactav.modelcore.mapper.WorkflowJobMapper;
import com.yali.mactav.modelcore.mapper.WorkspaceChangeRecordMapper;
import com.yali.mactav.modelcore.mapper.WorkspaceEventMapper;
import com.yali.mactav.modelcore.repository.InMemoryAgentExecutionRecordRepository;
import com.yali.mactav.modelcore.repository.InMemoryNetworkArtifactRepository;
import com.yali.mactav.modelcore.repository.InMemoryNetworkWorkspaceRepository;
import com.yali.mactav.modelcore.repository.InMemoryWorkspaceChangeRecordRepository;
import com.yali.mactav.modelcore.repository.InMemoryWorkspaceEventRepository;
import com.yali.mactav.modelcore.repository.MyBatisAgentExecutionRecordRepository;
import com.yali.mactav.modelcore.repository.MyBatisNetworkArtifactRepository;
import com.yali.mactav.modelcore.repository.MyBatisNetworkTaskRepository;
import com.yali.mactav.modelcore.repository.MyBatisNetworkWorkspaceStateRepository;
import com.yali.mactav.modelcore.repository.MyBatisWorkflowJobRepository;
import com.yali.mactav.modelcore.repository.MyBatisWorkspaceChangeRecordRepository;
import com.yali.mactav.modelcore.repository.MyBatisWorkspaceEventRepository;
import com.yali.mactav.modelcore.service.AgentExecutionRecordService;
import com.yali.mactav.modelcore.service.InMemoryAgentExecutionRecordService;
import com.yali.mactav.modelcore.service.InMemoryNetworkArtifactService;
import com.yali.mactav.modelcore.service.InMemoryNetworkWorkspaceService;
import com.yali.mactav.modelcore.service.InMemoryWorkspaceChangeRecordService;
import com.yali.mactav.modelcore.service.InMemoryWorkspaceEventService;
import com.yali.mactav.modelcore.service.MyBatisAgentExecutionRecordService;
import com.yali.mactav.modelcore.service.MyBatisNetworkArtifactService;
import com.yali.mactav.modelcore.service.MyBatisNetworkWorkspaceService;
import com.yali.mactav.modelcore.service.MyBatisWorkspaceChangeRecordService;
import com.yali.mactav.modelcore.service.MyBatisWorkspaceEventService;
import com.yali.mactav.modelcore.service.NetworkArtifactService;
import com.yali.mactav.modelcore.service.NetworkWorkspaceService;
import com.yali.mactav.modelcore.service.WorkspaceChangeRecordService;
import com.yali.mactav.modelcore.service.WorkspaceEventService;
import com.yali.mactav.modelcore.validator.ArtifactValidator;
import com.yali.mactav.modelcore.validator.WorkspaceStateValidator;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Spring wiring for Model Core validators plus persistence-specific services.
 *
 * <p>MySQL/MyBatis is the default production path. In-memory services are
 * available only through an explicit in-memory property or test/inmemory
 * profile; they are never a missing-DataSource fallback.</p>
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
    @Profile("!test & !inmemory")
    @ConditionalOnProperty(name = "mactav.events.publisher", havingValue = "redis", matchIfMissing = true)
    public WorkspaceEventPublisher redisWorkspaceEventPublisher(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${mactav.sse.redis-channel-prefix:${mactav.sse.redis-topic-prefix:mactav:events:}}")
            String channelPrefix) {
        return new RedisWorkspaceEventPublisher(redisTemplate, objectMapper, channelPrefix);
    }

    @Bean
    @ConditionalOnMissingBean(WorkspaceEventPublisher.class)
    @ConditionalOnExpression(
            "'${mactav.events.publisher:}' == 'noop' || "
                    + "'${spring.profiles.active:}'.contains('test') || "
                    + "'${spring.profiles.active:}'.contains('inmemory')")
    public WorkspaceEventPublisher noopWorkspaceEventPublisher() {
        return new NoopWorkspaceEventPublisher();
    }

    @Bean
    @ConditionalOnMissingBean
    public MyBatisModelCoreAssembler myBatisModelCoreAssembler(ObjectMapper objectMapper) {
        return new MyBatisModelCoreAssembler(objectMapper);
    }

    /**
     * Default durable MySQL/MyBatis Model Core wiring.
     */
    @Configuration(proxyBeanMethods = false)
    @Profile("!test & !inmemory")
    @ConditionalOnProperty(name = "mactav.persistence.type", havingValue = "mysql", matchIfMissing = true)
    @MapperScan("com.yali.mactav.modelcore.mapper")
    static class MyBatisPersistenceConfiguration {

        @Bean
        public MyBatisNetworkTaskRepository myBatisNetworkTaskRepository(NetworkTaskMapper mapper) {
            return new MyBatisNetworkTaskRepository(mapper);
        }

        @Bean
        public MyBatisNetworkWorkspaceStateRepository myBatisNetworkWorkspaceStateRepository(
                NetworkWorkspaceStateMapper mapper) {
            return new MyBatisNetworkWorkspaceStateRepository(mapper);
        }

        @Bean
        public MyBatisNetworkArtifactRepository myBatisNetworkArtifactRepository(NetworkArtifactMapper mapper) {
            return new MyBatisNetworkArtifactRepository(mapper);
        }

        @Bean
        public MyBatisWorkspaceEventRepository myBatisWorkspaceEventRepository(WorkspaceEventMapper mapper) {
            return new MyBatisWorkspaceEventRepository(mapper);
        }

        @Bean
        public MyBatisWorkspaceChangeRecordRepository myBatisWorkspaceChangeRecordRepository(
                WorkspaceChangeRecordMapper mapper) {
            return new MyBatisWorkspaceChangeRecordRepository(mapper);
        }

        @Bean
        public MyBatisAgentExecutionRecordRepository myBatisAgentExecutionRecordRepository(
                AgentExecutionRecordMapper mapper) {
            return new MyBatisAgentExecutionRecordRepository(mapper);
        }

        @Bean
        public MyBatisWorkflowJobRepository myBatisWorkflowJobRepository(WorkflowJobMapper mapper) {
            return new MyBatisWorkflowJobRepository(mapper);
        }

        @Bean
        public WorkspaceEventService workspaceEventService(MyBatisWorkspaceEventRepository repository,
                                                           WorkspaceStateValidator workspaceStateValidator,
                                                           MyBatisModelCoreAssembler assembler,
                                                           WorkspaceEventPublisher eventPublisher) {
            return new MyBatisWorkspaceEventService(repository, workspaceStateValidator, assembler, eventPublisher);
        }

        @Bean
        public WorkspaceChangeRecordService workspaceChangeRecordService(
                MyBatisWorkspaceChangeRecordRepository changeRepository,
                MyBatisNetworkTaskRepository taskRepository,
                WorkspaceStateValidator workspaceStateValidator,
                MyBatisModelCoreAssembler assembler) {
            return new MyBatisWorkspaceChangeRecordService(
                    changeRepository,
                    taskRepository,
                    workspaceStateValidator,
                    assembler);
        }

        @Bean
        public AgentExecutionRecordService agentExecutionRecordService(
                MyBatisAgentExecutionRecordRepository recordRepository,
                MyBatisNetworkTaskRepository taskRepository,
                WorkspaceStateValidator workspaceStateValidator,
                MyBatisModelCoreAssembler assembler) {
            return new MyBatisAgentExecutionRecordService(
                    recordRepository,
                    taskRepository,
                    workspaceStateValidator,
                    assembler);
        }

        @Bean
        public NetworkArtifactService networkArtifactService(MyBatisNetworkArtifactRepository repository,
                                                             NetworkArtifactFactory artifactFactory,
                                                             ArtifactValidator artifactValidator,
                                                             WorkspaceStateValidator workspaceStateValidator,
                                                             MyBatisModelCoreAssembler assembler) {
            return new MyBatisNetworkArtifactService(
                    repository,
                    artifactFactory,
                    artifactValidator,
                    workspaceStateValidator,
                    assembler);
        }

        @Bean
        public NetworkWorkspaceService networkWorkspaceService(
                MyBatisNetworkTaskRepository taskRepository,
                MyBatisNetworkWorkspaceStateRepository stateRepository,
                MyBatisNetworkArtifactRepository artifactRepository,
                NetworkArtifactService artifactService,
                WorkspaceEventService eventService,
                WorkspaceChangeRecordService changeRecordService,
                AgentExecutionRecordService executionRecordService,
                WorkspaceStateValidator workspaceStateValidator,
                ArtifactValidator artifactValidator,
                MyBatisModelCoreAssembler assembler,
                ArtifactPayloadSerializer payloadSerializer) {
            return new MyBatisNetworkWorkspaceService(
                    taskRepository,
                    stateRepository,
                    artifactRepository,
                    artifactService,
                    eventService,
                    changeRecordService,
                    executionRecordService,
                    workspaceStateValidator,
                    artifactValidator,
                    assembler,
                    payloadSerializer);
        }
    }

    /**
     * Explicit in-memory wiring for unit tests or local opt-in only.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnExpression(
            "'${mactav.persistence.type:}' == 'inmemory' || "
                    + "'${spring.profiles.active:}'.contains('test') || "
                    + "'${spring.profiles.active:}'.contains('inmemory')")
    static class InMemoryPersistenceConfiguration {

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
            return new InMemoryWorkspaceChangeRecordService(
                    changeRepository,
                    workspaceRepository,
                    workspaceStateValidator);
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
            return new InMemoryAgentExecutionRecordService(
                    recordRepository,
                    workspaceRepository,
                    workspaceStateValidator);
        }
    }
}
