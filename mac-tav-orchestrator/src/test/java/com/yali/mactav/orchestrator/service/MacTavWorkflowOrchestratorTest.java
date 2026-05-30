package com.yali.mactav.orchestrator.service;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.a2a.A2aResponse;
import com.yali.mactav.model.agent.AgentCard;
import com.yali.mactav.model.agent.AgentHealthStatus;
import com.yali.mactav.model.enums.*;
import com.yali.mactav.model.intent.*;
import com.yali.mactav.model.plan.*;
import com.yali.mactav.model.workspace.*;
import com.yali.mactav.modelcore.artifact.*;
import com.yali.mactav.modelcore.repository.*;
import com.yali.mactav.modelcore.service.*;
import com.yali.mactav.modelcore.validator.*;
import com.yali.mactav.orchestrator.remote.client.A2aClient;
import com.yali.mactav.orchestrator.remote.discovery.AgentDiscoveryClient;
import com.yali.mactav.orchestrator.remote.invoker.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class MacTavWorkflowOrchestratorTest {

    private InMemoryNetworkWorkspaceRepository rr;
    private WorkspaceStateValidator vv;

    private static ObjectMapper om() { var o = new ObjectMapper(); o.registerModule(new JavaTimeModule()); o.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); return o; }
    private static String j(Object o) { try { return om().writeValueAsString(o); } catch (Exception e) { throw new RuntimeException(e); } }

    private NetworkWorkspaceService ws(ObjectMapper om) {
        rr = new InMemoryNetworkWorkspaceRepository(); vv = new WorkspaceStateValidator();
        var av = new ArtifactValidator();
        var a = new InMemoryNetworkArtifactService(new InMemoryNetworkArtifactRepository(), new NetworkArtifactFactory(new ArtifactPayloadSerializer(om)), av, vv);
        var e = new InMemoryWorkspaceEventService(new InMemoryWorkspaceEventRepository(), vv);
        return new InMemoryNetworkWorkspaceService(rr, a, e, vv, av);
    }

    private AgentExecutionRecordService recS(InMemoryNetworkWorkspaceRepository r, WorkspaceStateValidator v) {
        return new InMemoryAgentExecutionRecordService(new InMemoryAgentExecutionRecordRepository(), r, v);
    }


    @Test
    void runIntentStageShouldPersistNetworkIntentArtifactIntoWorkspace() {
        var om = om();
        var d = (AgentDiscoveryClient) t -> AgentCard.builder().agentName(t).serviceEndpoint("http://x/").healthStatus(AgentHealthStatus.UP).build();
        var c = (A2aClient) (r, a) -> A2aResponse.builder().success(true).taskId(r.getTaskId()).sourceAgent("IntentAgent").targetAgent(r.getSourceAgent()).stage(WorkflowStage.INTENT).traceId(r.getTraceId()).timestamp(LocalDateTime.now()).payloadJson(j(NetworkIntent.builder().taskId(r.getTaskId()).intentVersion(r.getArtifactVersion()).rawText("x").semanticIntentGraph(SemanticIntentGraph.builder().nodes(java.util.List.of(IntentNode.builder().id("a").name("a").type("z").build())).relations(java.util.List.of(IntentRelation.builder().id("r").type("a").source("a").target("b").action("allow").build())).build()).stageStatus(StageStatus.SUCCESS).traceId(r.getTraceId()).createTime(LocalDateTime.now()).build())).build();
        var w = ws(om); var rec = recS(rr, vv);
        var o = new MacTavWorkflowOrchestrator(w, rec, new RemoteAgentInvoker(d, c, new A2aResponseValidator()), om);
        var cr = o.createTask("x", "lab", "u"); var res = o.runIntentStage(cr.getTask().getTaskId());
        assertNotNull(res.getCurrentIntent());
        assertEquals(ArtifactType.NETWORK_INTENT, res.getArtifacts().get(0).getArtifactType());
        assertEquals(1, res.getAgentExecutionRecords().size());
    }

}
