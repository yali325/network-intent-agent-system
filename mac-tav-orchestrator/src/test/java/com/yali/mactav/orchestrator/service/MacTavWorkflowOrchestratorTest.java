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
import com.yali.mactav.model.config.*;
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

    @Test
    void runPlanningStageShouldPersistNetworkPlanArtifactIntoWorkspace() {
        var om = om();
        var d = (AgentDiscoveryClient) t -> AgentCard.builder().agentName(t).serviceEndpoint("http://x/").healthStatus(AgentHealthStatus.UP).build();
        var c = (A2aClient) (r, a) -> {
            if (r.getStage() == WorkflowStage.INTENT) {
                return A2aResponse.builder().success(true).taskId(r.getTaskId()).sourceAgent("IntentAgent")
                        .targetAgent(r.getSourceAgent()).stage(WorkflowStage.INTENT).traceId(r.getTraceId())
                        .timestamp(LocalDateTime.now()).payloadJson(j(NetworkIntent.builder()
                                .taskId(r.getTaskId()).intentVersion(r.getArtifactVersion()).rawText("x")
                                .semanticIntentGraph(SemanticIntentGraph.builder()
                                        .nodes(java.util.List.of(IntentNode.builder().id("node-office").name("office").type("zone").build()))
                                        .relations(java.util.List.of(IntentRelation.builder().id("rel-office-server").type("access")
                                                .source("node-office").target("node-server").action("allow").build()))
                                        .build())
                                .stageStatus(StageStatus.SUCCESS).traceId(r.getTraceId())
                                .createTime(LocalDateTime.now()).build())).build();
            }
            var plan = NetworkPlan.builder()
                    .taskId(r.getTaskId())
                    .intentVersion(1)
                    .planVersion(r.getArtifactVersion())
                    .planSummary("NetworkPlan for planning stage")
                    .targetEnvironment(TargetEnvironment.builder().vendor("generic")
                            .configStyle("structured").adapterType("structured-validation").build())
                    .topology(Topology.builder()
                            .nodes(java.util.List.of(TopologyNode.builder().id("sw-core").name("core").nodeType("SWITCH").build()))
                            .links(java.util.List.of()).build())
                    .zones(java.util.List.of(NetworkZone.builder().id("zone-office").name("office").build()))
                    .addressPlan(java.util.List.of(AddressPlanItem.builder().id("addr-office").zoneId("zone-office")
                            .subnet("10.1.0.0/24").gateway("10.1.0.1")
                            .traceRefs(TraceRefs.builder().intentNodeIds(java.util.List.of("node-office")).build()).build()))
                    .vlanPlan(java.util.List.of(VlanPlanItem.builder().id("vlan-office").vlanId(100).zoneId("zone-office").build()))
                    .routingPlan(RoutingPlan.builder().id("routing-ospf").protocol("OSPF")
                            .routers(java.util.List.of(RoutingRouter.builder().id("router-edge").deviceId("rtr-edge").build()))
                            .traceRefs(TraceRefs.builder().intentNodeIds(java.util.List.of("node-office")).build()).build())
                    .traceRefs(TraceRefs.builder().intentNodeIds(java.util.List.of("node-office")).build())
                    .stageStatus(StageStatus.SUCCESS)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .createdBy("u")
                    .build();
            return A2aResponse.builder().success(true).taskId(r.getTaskId()).sourceAgent("PlanningAgent")
                    .targetAgent(r.getSourceAgent()).stage(WorkflowStage.PLANNING).traceId(r.getTraceId())
                    .timestamp(LocalDateTime.now()).payloadJson(j(plan)).build();
        };
        var w = ws(om); var rec = recS(rr, vv);
        var o = new MacTavWorkflowOrchestrator(w, rec, new RemoteAgentInvoker(d, c, new A2aResponseValidator()), om);
        var cr = o.createTask("x", "lab", "u");
        o.runIntentStage(cr.getTask().getTaskId());
        var res = o.runPlanningStage(cr.getTask().getTaskId());
        assertNotNull(res.getCurrentPlan());
        assertEquals(ArtifactType.NETWORK_PLAN, res.getArtifacts().get(1).getArtifactType());
        assertEquals(2, res.getAgentExecutionRecords().size());
        assertEquals("PlanningAgent", res.getAgentExecutionRecords().get(1).getTargetAgentName());
    }

    @Test
    void runConfigurationStageShouldFailWhenNetworkPlanIsMissing() {
        var om = om();
        var d = (AgentDiscoveryClient) t -> AgentCard.builder().agentName(t).serviceEndpoint("http://x/").healthStatus(AgentHealthStatus.UP).build();
        var c = (A2aClient) (r, a) -> A2aResponse.builder().success(true).build();
        var w = ws(om); var rec = recS(rr, vv);
        var o = new MacTavWorkflowOrchestrator(w, rec, new RemoteAgentInvoker(d, c, new A2aResponseValidator()), om);
        var cr = o.createTask("x", "lab", "u");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> o.runConfigurationStage(cr.getTask().getTaskId()));

        assertEquals(ErrorCode.STAGE_NOT_READY.name(), ex.getErrorCode());
    }

    @Test
    void runConfigurationStageShouldPersistConfigSetArtifactIntoWorkspace() {
        var om = om();
        var d = (AgentDiscoveryClient) t -> AgentCard.builder().agentName(t).serviceEndpoint("http://x/").healthStatus(AgentHealthStatus.UP).build();
        final java.util.List<com.yali.mactav.model.a2a.A2aRequest> requests = new java.util.ArrayList<>();
        var c = (A2aClient) (r, a) -> {
            requests.add(r);
            if (r.getStage() == WorkflowStage.INTENT) {
                return intentResponse(r);
            }
            if (r.getStage() == WorkflowStage.PLANNING) {
                return planningResponse(r);
            }
            return A2aResponse.builder().success(true).taskId(r.getTaskId()).sourceAgent("ConfigurationAgent")
                    .targetAgent(r.getSourceAgent()).stage(WorkflowStage.CONFIGURATION).traceId(r.getTraceId())
                    .timestamp(LocalDateTime.now()).payloadJson(j(configSet(r))).build();
        };
        var w = ws(om); var rec = recS(rr, vv);
        var o = new MacTavWorkflowOrchestrator(w, rec, new RemoteAgentInvoker(d, c, new A2aResponseValidator()), om);
        var cr = o.createTask("x", "lab", "u");
        o.runIntentStage(cr.getTask().getTaskId());
        o.runPlanningStage(cr.getTask().getTaskId());

        var res = o.runConfigurationStage(cr.getTask().getTaskId());

        assertEquals(WorkflowStage.CONFIGURATION, requests.get(2).getStage());
        assertEquals("ConfigurationAgent", requests.get(2).getTargetAgent());
        assertNotNull(res.getCurrentConfigSet());
        assertEquals(1, res.getCurrentConfigVersion());
        assertEquals(ArtifactType.CONFIG_SET, res.getArtifacts().get(2).getArtifactType());
        assertEquals(res.getArtifacts().get(2).getArtifactId(), res.getCurrentArtifactRefs().get(ArtifactType.CONFIG_SET));
        assertEquals(3, res.getAgentExecutionRecords().size());
        assertEquals("ConfigurationAgent", res.getAgentExecutionRecords().get(2).getTargetAgentName());
        assertEquals(WorkflowStage.CONFIGURATION, res.getAgentExecutionRecords().get(2).getStage());
        assertTrue(res.getArtifacts().get(2).getPayloadSummary().contains("commandBlocks=2"));
    }

    private static A2aResponse intentResponse(com.yali.mactav.model.a2a.A2aRequest r) {
        return A2aResponse.builder().success(true).taskId(r.getTaskId()).sourceAgent("IntentAgent")
                .targetAgent(r.getSourceAgent()).stage(WorkflowStage.INTENT).traceId(r.getTraceId())
                .timestamp(LocalDateTime.now()).payloadJson(j(NetworkIntent.builder()
                        .taskId(r.getTaskId()).intentVersion(r.getArtifactVersion()).rawText("x")
                        .semanticIntentGraph(SemanticIntentGraph.builder()
                                .nodes(java.util.List.of(IntentNode.builder().id("node-office").name("office").type("zone").build()))
                                .relations(java.util.List.of(IntentRelation.builder().id("rel-office-server").type("access")
                                        .source("node-office").target("node-server").action("allow").build()))
                                .build())
                        .stageStatus(StageStatus.SUCCESS).traceId(r.getTraceId())
                        .createTime(LocalDateTime.now()).build())).build();
    }

    private static A2aResponse planningResponse(com.yali.mactav.model.a2a.A2aRequest r) {
        return A2aResponse.builder().success(true).taskId(r.getTaskId()).sourceAgent("PlanningAgent")
                .targetAgent(r.getSourceAgent()).stage(WorkflowStage.PLANNING).traceId(r.getTraceId())
                .timestamp(LocalDateTime.now()).payloadJson(j(networkPlan(r))).build();
    }

    private static NetworkPlan networkPlan(com.yali.mactav.model.a2a.A2aRequest r) {
        return NetworkPlan.builder()
                .taskId(r.getTaskId())
                .intentVersion(1)
                .planVersion(r.getArtifactVersion())
                .planSummary("NetworkPlan for configuration stage")
                .targetEnvironment(TargetEnvironment.builder().vendor("generic")
                        .configStyle("structured").adapterType("structured-validation").build())
                .topology(Topology.builder()
                        .nodes(java.util.List.of(TopologyNode.builder().id("sw-core").name("core").nodeType("SWITCH").build()))
                        .links(java.util.List.of()).build())
                .zones(java.util.List.of(NetworkZone.builder().id("zone-office").name("office").build()))
                .addressPlan(java.util.List.of(AddressPlanItem.builder().id("addr-office").zoneId("zone-office")
                        .subnet("10.1.0.0/24").gateway("10.1.0.1")
                        .traceRefs(TraceRefs.builder().intentNodeIds(java.util.List.of("node-office")).build()).build()))
                .vlanPlan(java.util.List.of(VlanPlanItem.builder().id("vlan-office").vlanId(100).zoneId("zone-office").build()))
                .routingPlan(RoutingPlan.builder().id("routing-ospf").protocol("OSPF")
                        .routers(java.util.List.of(RoutingRouter.builder().id("router-edge").deviceId("rtr-edge").build()))
                        .traceRefs(TraceRefs.builder().intentNodeIds(java.util.List.of("node-office")).build()).build())
                .traceRefs(TraceRefs.builder().intentNodeIds(java.util.List.of("node-office"))
                        .planElementIds(java.util.List.of("vlan-office", "addr-office")).build())
                .stageStatus(StageStatus.SUCCESS)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .createdBy("u")
                .build();
    }

    private static ConfigSet configSet(com.yali.mactav.model.a2a.A2aRequest r) {
        TraceRefs traceRefs = TraceRefs.builder()
                .intentNodeIds(java.util.List.of("node-office"))
                .intentRelationIds(java.util.List.of("rel-office-server"))
                .planElementIds(java.util.List.of("vlan-office", "addr-office"))
                .build();
        return ConfigSet.builder()
                .taskId(r.getTaskId())
                .planVersion(1)
                .configVersion(r.getArtifactVersion())
                .targetEnvironment(TargetEnvironment.builder().vendor("generic")
                        .configStyle("structured").adapterType("structured-validation").build())
                .generationSummary("Structured configuration for office access")
                .generationSources(java.util.List.of(
                        GenerationSource.builder().sourceType(GenerationSourceType.LLM).sourceId("llm-qwen").build(),
                        GenerationSource.builder().sourceType(GenerationSourceType.TEMPLATE).sourceId("tpl-vlan").build()))
                .deviceConfigs(java.util.List.of(DeviceConfig.builder()
                        .deviceName("core")
                        .deviceType("SWITCH")
                        .commandBlocks(java.util.List.of(
                                CommandBlock.builder().blockId("cfg-vlan-office")
                                        .commands(java.util.List.of("vlan 100", "description office"))
                                        .explanation("Create office VLAN")
                                        .rollbackCommands(java.util.List.of("undo vlan 100"))
                                        .traceRefs(traceRefs)
                                        .riskLevel("LOW")
                                        .isIdempotent(true)
                                        .build(),
                                CommandBlock.builder().blockId("cfg-interface-office")
                                        .commands(java.util.List.of("interface GigabitEthernet0/0/1", "port default vlan 100"))
                                        .explanation("Bind office interface to VLAN")
                                        .rollbackCommands(java.util.List.of("interface GigabitEthernet0/0/1", "undo port default vlan"))
                                        .traceRefs(traceRefs)
                                        .riskLevel("MEDIUM")
                                        .isIdempotent(false)
                                        .build()))
                        .traceRefs(traceRefs)
                        .build()))
                .traceRefs(traceRefs)
                .stageStatus(StageStatus.SUCCESS)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .createdBy("ConfigurationAgent")
                .build();
    }

}
