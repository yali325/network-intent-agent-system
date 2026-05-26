package com.yali.mactav.orchestrator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yali.mactav.model.a2a.A2aRequest;
import com.yali.mactav.model.a2a.A2aResponse;
import com.yali.mactav.model.agent.AgentCard;
import com.yali.mactav.model.agent.AgentHealthStatus;
import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.enums.StageStatus;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.intent.IntentNode;
import com.yali.mactav.model.intent.IntentRelation;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.intent.SemanticIntentGraph;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.modelcore.artifact.ArtifactPayloadSerializer;
import com.yali.mactav.modelcore.artifact.NetworkArtifactFactory;
import com.yali.mactav.modelcore.repository.InMemoryAgentExecutionRecordRepository;
import com.yali.mactav.modelcore.repository.InMemoryNetworkArtifactRepository;
import com.yali.mactav.modelcore.repository.InMemoryNetworkWorkspaceRepository;
import com.yali.mactav.modelcore.repository.InMemoryWorkspaceEventRepository;
import com.yali.mactav.modelcore.service.AgentExecutionRecordService;
import com.yali.mactav.modelcore.service.InMemoryAgentExecutionRecordService;
import com.yali.mactav.modelcore.service.InMemoryNetworkArtifactService;
import com.yali.mactav.modelcore.service.InMemoryNetworkWorkspaceService;
import com.yali.mactav.modelcore.service.InMemoryWorkspaceEventService;
import com.yali.mactav.modelcore.service.NetworkArtifactService;
import com.yali.mactav.modelcore.service.NetworkWorkspaceService;
import com.yali.mactav.modelcore.service.WorkspaceEventService;
import com.yali.mactav.modelcore.validator.ArtifactValidator;
import com.yali.mactav.modelcore.validator.WorkspaceStateValidator;
import com.yali.mactav.orchestrator.remote.client.A2aClient;
import com.yali.mactav.orchestrator.remote.discovery.AgentDiscoveryClient;
import com.yali.mactav.orchestrator.remote.invoker.A2aResponseValidator;
import com.yali.mactav.orchestrator.remote.invoker.RemoteAgentInvoker;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Offline tests for the Intent-stage Orchestrator workspace closure.
 *
 * <p>The test replaces only the remote A2A transport boundary with a fixed
 * response fixture and does not create concrete agent beans or call models.</p>
 */
class MacTavWorkflowOrchestratorTest {

    @Test
    void runIntentStageShouldPersistNetworkIntentArtifactIntoWorkspace() throws Exception {
        ObjectMapper objectMapper = objectMapper();
        AgentDiscoveryClient discoveryClient = targetAgentName -> AgentCard.builder()
                .agentName(targetAgentName)
                .serviceEndpoint("http://127.0.0.1/internal/a2a/intent/invoke")
                .healthStatus(AgentHealthStatus.UP)
                .build();
        A2aClient a2aClient = (request, agentCard) -> A2aResponse.builder()
                .success(true)
                .taskId(request.getTaskId())
                .sourceAgent("IntentAgent")
                .targetAgent(request.getSourceAgent())
                .stage(WorkflowStage.INTENT)
                .payloadJson(serialize(objectMapper, intentFor(request)))
                .traceId(request.getTraceId())
                .timestamp(LocalDateTime.now())
                .build();
        NetworkWorkspaceService workspaceService = workspaceService(objectMapper);
        AgentExecutionRecordService recordService = recordService(workspaceRepository, workspaceStateValidator);
        MacTavWorkflowOrchestrator orchestrator = new MacTavWorkflowOrchestrator(
                workspaceService,
                recordService,
                new RemoteAgentInvoker(discoveryClient, a2aClient, new A2aResponseValidator()),
                objectMapper);
        NetworkWorkspace created = orchestrator.createTask("office can access server", "lab", "unit-test");

        NetworkWorkspace workspace = orchestrator.runIntentStage(created.getTask().getTaskId());

        assertNotNull(workspace.getCurrentIntent());
        assertEquals(ArtifactType.NETWORK_INTENT, workspace.getArtifacts().get(0).getArtifactType());
        assertEquals(1, workspace.getCurrentIntentVersion());
        assertEquals(1, workspace.getAgentExecutionRecords().size());
        assertEquals(StageStatus.SUCCESS, workspace.getAgentExecutionRecords().get(0).getStageStatus());
    }

    private InMemoryNetworkWorkspaceRepository workspaceRepository;

    private WorkspaceStateValidator workspaceStateValidator;

    private NetworkWorkspaceService workspaceService(ObjectMapper objectMapper) {
        workspaceRepository = new InMemoryNetworkWorkspaceRepository();
        workspaceStateValidator = new WorkspaceStateValidator();
        ArtifactValidator artifactValidator = new ArtifactValidator();
        NetworkArtifactService artifactService = new InMemoryNetworkArtifactService(
                new InMemoryNetworkArtifactRepository(),
                new NetworkArtifactFactory(new ArtifactPayloadSerializer(objectMapper)),
                artifactValidator,
                workspaceStateValidator);
        WorkspaceEventService eventService = new InMemoryWorkspaceEventService(
                new InMemoryWorkspaceEventRepository(),
                workspaceStateValidator);
        return new InMemoryNetworkWorkspaceService(
                workspaceRepository,
                artifactService,
                eventService,
                workspaceStateValidator,
                artifactValidator);
    }

    private AgentExecutionRecordService recordService(InMemoryNetworkWorkspaceRepository repository,
                                                      WorkspaceStateValidator validator) {
        return new InMemoryAgentExecutionRecordService(
                new InMemoryAgentExecutionRecordRepository(),
                repository,
                validator);
    }

    private NetworkIntent intentFor(A2aRequest request) {
        return NetworkIntent.builder()
                .taskId(request.getTaskId())
                .intentVersion(request.getArtifactVersion())
                .rawText("office can access server")
                .semanticIntentGraph(SemanticIntentGraph.builder()
                        .nodes(List.of(IntentNode.builder()
                                .id("node-office")
                                .name("office")
                                .type("zone")
                                .build()))
                        .relations(List.of(IntentRelation.builder()
                                .id("rel-office-server")
                                .type("access")
                                .source("node-office")
                                .target("node-server")
                                .action("allow")
                                .build()))
                        .build())
                .stageStatus(StageStatus.SUCCESS)
                .traceId(request.getTraceId())
                .createTime(LocalDateTime.now())
                .build();
    }

    private String serialize(ObjectMapper mapper, Object value) {
        try {
            return mapper.writeValueAsString(value);
        }
        catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
