package com.yali.mactav.orchestrator.service;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.execution.config.ExecutionProperties;
import com.yali.mactav.execution.registry.ExecutionAdapterRegistryFactory;
import com.yali.mactav.execution.service.DefaultExecutionService;
import com.yali.mactav.model.a2a.A2aResponse;
import com.yali.mactav.model.agent.AgentCard;
import com.yali.mactav.model.agent.AgentHealthStatus;
import com.yali.mactav.model.config.*;
import com.yali.mactav.model.enums.*;
import com.yali.mactav.model.healing.*;
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
        var ch = new InMemoryWorkspaceChangeRecordService(new InMemoryWorkspaceChangeRecordRepository(), rr, vv);
        return new InMemoryNetworkWorkspaceService(rr, a, e, ch, vv, av);
    }

    private AgentExecutionRecordService recS(InMemoryNetworkWorkspaceRepository r, WorkspaceStateValidator v) {
        return new InMemoryAgentExecutionRecordService(new InMemoryAgentExecutionRecordRepository(), r, v);
    }

    private WorkspaceChangeRecordService changeS() {
        return new InMemoryWorkspaceChangeRecordService(new InMemoryWorkspaceChangeRecordRepository(), rr, vv);
    }

    private MacTavWorkflowOrchestrator orchestratorWithChanges(
            NetworkWorkspaceService w,
            AgentExecutionRecordService rec,
            RemoteAgentInvoker invoker,
            ObjectMapper om) {
        return new MacTavWorkflowOrchestrator(
                w,
                rec,
                changeS(),
                invoker,
                om,
                new DefaultExecutionService(ExecutionAdapterRegistryFactory.structureValidationRegistry()),
                new ExecutionProperties());
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
        assertNotNull(res.getCurrentIntent().getSemanticIntentGraph());
        assertEquals(ArtifactType.NETWORK_INTENT, res.getArtifacts().get(0).getArtifactType());
        assertTrue(res.getArtifacts().get(0).getPayloadJson().contains("semanticIntentGraph"));
        assertFalse(res.getArtifacts().get(0).getPayloadJson().contains("semanticSummary"));
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

    @Test
    void runExecutionStageShouldPersistExecutionReportArtifactIntoWorkspace() {
        var om = om();
        var d = (AgentDiscoveryClient) t -> AgentCard.builder().agentName(t).serviceEndpoint("http://x/").healthStatus(AgentHealthStatus.UP).build();
        var c = (A2aClient) (r, a) -> A2aResponse.builder().success(true).build();
        var w = ws(om); var rec = recS(rr, vv);
        var o = new MacTavWorkflowOrchestrator(w, rec, new RemoteAgentInvoker(d, c, new A2aResponseValidator()), om);
        var created = o.createTask("x", "lab", "u");
        String taskId = created.getTask().getTaskId();
        var planArtifact = w.saveStageArtifact(
                taskId,
                ArtifactType.NETWORK_PLAN,
                WorkflowStage.PLANNING,
                executablePlan(taskId),
                "NetworkPlan for execution",
                "PlanningAgent",
                executionTraceRefs());
        var configArtifact = w.saveStageArtifact(
                taskId,
                ArtifactType.CONFIG_SET,
                WorkflowStage.CONFIGURATION,
                executableConfigSet(taskId),
                "ConfigSet for execution",
                "ConfigurationAgent",
                executionTraceRefs());

        var res = o.runExecutionStage(taskId);

        assertNotNull(res.getCurrentExecutionReport());
        assertEquals(1, res.getCurrentExecutionVersion());
        assertEquals(ArtifactType.EXECUTION_REPORT, res.getArtifacts().get(2).getArtifactType());
        assertEquals(res.getArtifacts().get(2).getArtifactId(), res.getCurrentArtifactRefs().get(ArtifactType.EXECUTION_REPORT));
        assertEquals(planArtifact.getArtifactId(), res.getCurrentArtifactRefs().get(ArtifactType.NETWORK_PLAN));
        assertEquals(configArtifact.getArtifactId(), res.getCurrentArtifactRefs().get(ArtifactType.CONFIG_SET));
        assertEquals(1, res.getAgentExecutionRecords().size());
        assertEquals("ExecutionModule", res.getAgentExecutionRecords().get(0).getTargetAgentName());
        assertEquals(WorkflowStage.EXECUTION, res.getAgentExecutionRecords().get(0).getStage());
        assertEquals(com.yali.mactav.model.execution.ExecutionEnvironmentType.STRUCTURE_VALIDATION,
                res.getCurrentExecutionReport().getEnvironmentType());
    }

    @Test
    void runExecutionStageShouldFailWhenNetworkPlanIsMissing() {
        var om = om();
        var d = (AgentDiscoveryClient) t -> AgentCard.builder().agentName(t).serviceEndpoint("http://x/").healthStatus(AgentHealthStatus.UP).build();
        var c = (A2aClient) (r, a) -> A2aResponse.builder().success(true).build();
        var w = ws(om); var rec = recS(rr, vv);
        var o = new MacTavWorkflowOrchestrator(w, rec, new RemoteAgentInvoker(d, c, new A2aResponseValidator()), om);
        var created = o.createTask("x", "lab", "u");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> o.runExecutionStage(created.getTask().getTaskId()));

        assertEquals(ErrorCode.STAGE_NOT_READY.getErrorCode(), ex.getErrorCode());
    }

    @Test
    void runExecutionStageShouldFailWhenConfigSetIsMissing() {
        var om = om();
        var d = (AgentDiscoveryClient) t -> AgentCard.builder().agentName(t).serviceEndpoint("http://x/").healthStatus(AgentHealthStatus.UP).build();
        var c = (A2aClient) (r, a) -> A2aResponse.builder().success(true).build();
        var w = ws(om); var rec = recS(rr, vv);
        var o = new MacTavWorkflowOrchestrator(w, rec, new RemoteAgentInvoker(d, c, new A2aResponseValidator()), om);
        var created = o.createTask("x", "lab", "u");
        String taskId = created.getTask().getTaskId();
        w.saveStageArtifact(
                taskId,
                ArtifactType.NETWORK_PLAN,
                WorkflowStage.PLANNING,
                executablePlan(taskId),
                "NetworkPlan for execution",
                "PlanningAgent",
                executionTraceRefs());

        BusinessException ex = assertThrows(BusinessException.class, () -> o.runExecutionStage(taskId));

        assertEquals(ErrorCode.STAGE_NOT_READY.getErrorCode(), ex.getErrorCode());
    }

    @Test
    void runVerificationStageShouldPersistValidationReportArtifactIntoWorkspace() {
        var om = om();
        var d = (AgentDiscoveryClient) t -> AgentCard.builder().agentName(t).serviceEndpoint("http://x/").healthStatus(AgentHealthStatus.UP).build();
        final java.util.List<com.yali.mactav.model.a2a.A2aRequest> requests = new java.util.ArrayList<>();
        var c = (A2aClient) (r, a) -> {
            requests.add(r);
            return A2aResponse.builder().success(true).taskId(r.getTaskId()).sourceAgent("VerificationAgent")
                    .targetAgent(r.getSourceAgent()).stage(WorkflowStage.VERIFICATION).traceId(r.getTraceId())
                    .timestamp(LocalDateTime.now()).payloadJson(j(validationReport(r))).build();
        };
        var w = ws(om); var rec = recS(rr, vv);
        var o = new MacTavWorkflowOrchestrator(w, rec, new RemoteAgentInvoker(d, c, new A2aResponseValidator()), om);
        var created = o.createTask("x", "lab", "u");
        String taskId = created.getTask().getTaskId();
        w.saveStageArtifact(taskId, ArtifactType.NETWORK_INTENT, WorkflowStage.INTENT,
                verificationIntent(taskId), "NetworkIntent for verification", "IntentAgent", executionTraceRefs());
        w.saveStageArtifact(taskId, ArtifactType.NETWORK_PLAN, WorkflowStage.PLANNING,
                executablePlan(taskId), "NetworkPlan for verification", "PlanningAgent", executionTraceRefs());
        w.saveStageArtifact(taskId, ArtifactType.CONFIG_SET, WorkflowStage.CONFIGURATION,
                executableConfigSet(taskId), "ConfigSet for verification", "ConfigurationAgent", executionTraceRefs());
        w.saveStageArtifact(taskId, ArtifactType.EXECUTION_REPORT, WorkflowStage.EXECUTION,
                executionReport(taskId), "ExecutionReport for verification", "ExecutionModule", executionTraceRefs());

        var res = o.runVerificationStage(taskId);

        assertEquals(WorkflowStage.VERIFICATION, requests.get(0).getStage());
        assertEquals("VerificationAgent", requests.get(0).getTargetAgent());
        assertNotNull(res.getCurrentValidationReport());
        assertEquals(1, res.getCurrentValidationVersion());
        assertEquals(ArtifactType.VALIDATION_REPORT, res.getArtifacts().get(4).getArtifactType());
        assertEquals(res.getArtifacts().get(4).getArtifactId(), res.getCurrentArtifactRefs().get(ArtifactType.VALIDATION_REPORT));
        assertEquals(1, res.getAgentExecutionRecords().size());
        assertEquals("VerificationAgent", res.getAgentExecutionRecords().get(0).getTargetAgentName());
        assertEquals(WorkflowStage.VERIFICATION, res.getAgentExecutionRecords().get(0).getStage());
        assertTrue(res.getArtifacts().get(4).getPayloadSummary().contains("items=1"));
    }

    @Test
    void runVerificationStageShouldFailWhenExecutionReportIsMissing() {
        var om = om();
        var d = (AgentDiscoveryClient) t -> AgentCard.builder().agentName(t).serviceEndpoint("http://x/").healthStatus(AgentHealthStatus.UP).build();
        var c = (A2aClient) (r, a) -> A2aResponse.builder().success(true).build();
        var w = ws(om); var rec = recS(rr, vv);
        var o = new MacTavWorkflowOrchestrator(w, rec, new RemoteAgentInvoker(d, c, new A2aResponseValidator()), om);
        var created = o.createTask("x", "lab", "u");
        String taskId = created.getTask().getTaskId();
        w.saveStageArtifact(taskId, ArtifactType.NETWORK_INTENT, WorkflowStage.INTENT,
                verificationIntent(taskId), "NetworkIntent for verification", "IntentAgent", executionTraceRefs());
        w.saveStageArtifact(taskId, ArtifactType.NETWORK_PLAN, WorkflowStage.PLANNING,
                executablePlan(taskId), "NetworkPlan for verification", "PlanningAgent", executionTraceRefs());
        w.saveStageArtifact(taskId, ArtifactType.CONFIG_SET, WorkflowStage.CONFIGURATION,
                executableConfigSet(taskId), "ConfigSet for verification", "ConfigurationAgent", executionTraceRefs());

        BusinessException ex = assertThrows(BusinessException.class, () -> o.runVerificationStage(taskId));

        assertEquals(ErrorCode.STAGE_NOT_READY.getErrorCode(), ex.getErrorCode());
    }

    @Test
    void runHealingStageShouldPersistRepairPlanArtifactIntoWorkspace() {
        var om = om();
        var d = (AgentDiscoveryClient) t -> AgentCard.builder().agentName(t).serviceEndpoint("http://x/").healthStatus(AgentHealthStatus.UP).build();
        final java.util.List<com.yali.mactav.model.a2a.A2aRequest> requests = new java.util.ArrayList<>();
        var c = (A2aClient) (r, a) -> {
            requests.add(r);
            return A2aResponse.builder().success(true).taskId(r.getTaskId()).sourceAgent("HealingAgent")
                    .targetAgent(r.getSourceAgent()).stage(WorkflowStage.HEALING).traceId(r.getTraceId())
                    .timestamp(LocalDateTime.now()).payloadJson(j(repairPlan(r))).build();
        };
        var w = ws(om); var rec = recS(rr, vv);
        var o = new MacTavWorkflowOrchestrator(w, rec, new RemoteAgentInvoker(d, c, new A2aResponseValidator()), om);
        var created = o.createTask("x", "lab", "u");
        String taskId = created.getTask().getTaskId();
        saveFailedValidationFixture(w, taskId);

        var res = o.runHealingStage(taskId);

        assertEquals(WorkflowStage.HEALING, requests.get(0).getStage());
        assertEquals("HealingAgent", requests.get(0).getTargetAgent());
        assertNotNull(res.getCurrentRepairPlan());
        assertEquals(1, res.getCurrentRepairVersion());
        assertEquals(ArtifactType.REPAIR_PLAN, res.getArtifacts().get(5).getArtifactType());
        assertEquals(res.getArtifacts().get(5).getArtifactId(), res.getCurrentArtifactRefs().get(ArtifactType.REPAIR_PLAN));
        assertEquals(1, res.getAgentExecutionRecords().size());
        assertEquals("HealingAgent", res.getAgentExecutionRecords().get(0).getTargetAgentName());
        assertEquals(WorkflowStage.HEALING, res.getAgentExecutionRecords().get(0).getStage());
        assertEquals(TaskStatus.WAITING_USER, res.getTask().getTaskStatus());
        assertTrue(res.getArtifacts().get(5).getPayloadSummary().contains("actions=1"));
    }

    @Test
    void runHealingStageShouldFailWhenValidationReportIsMissing() {
        var om = om();
        var d = (AgentDiscoveryClient) t -> AgentCard.builder().agentName(t).serviceEndpoint("http://x/").healthStatus(AgentHealthStatus.UP).build();
        var c = (A2aClient) (r, a) -> A2aResponse.builder().success(true).build();
        var w = ws(om); var rec = recS(rr, vv);
        var o = new MacTavWorkflowOrchestrator(w, rec, new RemoteAgentInvoker(d, c, new A2aResponseValidator()), om);
        var created = o.createTask("x", "lab", "u");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> o.runHealingStage(created.getTask().getTaskId()));

        assertEquals(ErrorCode.STAGE_NOT_READY.getErrorCode(), ex.getErrorCode());
    }

    @Test
    void runHealingStageShouldRejectPassedValidationReport() {
        var om = om();
        var d = (AgentDiscoveryClient) t -> AgentCard.builder().agentName(t).serviceEndpoint("http://x/").healthStatus(AgentHealthStatus.UP).build();
        final int[] remoteCalls = {0};
        var c = (A2aClient) (r, a) -> {
            remoteCalls[0]++;
            return A2aResponse.builder().success(true).build();
        };
        var w = ws(om); var rec = recS(rr, vv);
        var o = new MacTavWorkflowOrchestrator(w, rec, new RemoteAgentInvoker(d, c, new A2aResponseValidator()), om);
        var created = o.createTask("x", "lab", "u");
        String taskId = created.getTask().getTaskId();
        w.saveStageArtifact(taskId, ArtifactType.VALIDATION_REPORT, WorkflowStage.VERIFICATION,
                passedValidationReport(taskId), "passed validation", "VerificationAgent", executionTraceRefs());

        BusinessException ex = assertThrows(BusinessException.class, () -> o.runHealingStage(taskId));

        assertEquals(ErrorCode.STAGE_NOT_READY.getErrorCode(), ex.getErrorCode());
        assertEquals(0, remoteCalls[0]);
    }

    @Test
    void runHealingStageShouldRecordFailureWhenRemoteHealingAgentFails() {
        var om = om();
        var d = (AgentDiscoveryClient) t -> AgentCard.builder().agentName(t).serviceEndpoint("http://x/").healthStatus(AgentHealthStatus.UP).build();
        var c = (A2aClient) (r, a) -> A2aResponse.builder()
                .success(false)
                .taskId(r.getTaskId())
                .sourceAgent("HealingAgent")
                .targetAgent(r.getSourceAgent())
                .stage(WorkflowStage.HEALING)
                .traceId(r.getTraceId())
                .timestamp(LocalDateTime.now())
                .errorCode(ErrorCode.AGENT_EXECUTION_FAILED.getErrorCode())
                .message("HealingAgent rejected invalid output")
                .build();
        var w = ws(om); var rec = recS(rr, vv);
        var o = new MacTavWorkflowOrchestrator(w, rec, new RemoteAgentInvoker(d, c, new A2aResponseValidator()), om);
        var created = o.createTask("x", "lab", "u");
        String taskId = created.getTask().getTaskId();
        saveFailedValidationFixture(w, taskId);

        BusinessException ex = assertThrows(BusinessException.class, () -> o.runHealingStage(taskId));

        assertEquals(ErrorCode.AGENT_EXECUTION_FAILED.getErrorCode(), ex.getErrorCode());
        NetworkWorkspace workspace = w.getWorkspaceOrThrow(taskId);
        assertEquals(TaskStatus.ERROR, workspace.getTask().getTaskStatus());
        assertEquals(1, workspace.getAgentExecutionRecords().size());
        assertEquals(StageStatus.FAILED, workspace.getAgentExecutionRecords().get(0).getStageStatus());
        assertEquals("HealingAgent", workspace.getAgentExecutionRecords().get(0).getTargetAgentName());
        assertEquals(WorkflowStage.HEALING, workspace.getAgentExecutionRecords().get(0).getStage());
    }

    @Test
    void approveRepairActionShouldUpdateActionAndRecordChangeEvent() {
        var om = om();
        var w = ws(om); var rec = recS(rr, vv);
        var o = orchestratorWithChanges(w, rec, noOpInvoker(), om);
        var created = o.createTask("x", "lab", "u");
        String taskId = created.getTask().getTaskId();
        saveRepairPlanFixture(w, taskId, repairPlanForAction(taskId, "PATCH_CONFIG", WorkflowStage.CONFIGURATION,
                "HIGH", true, RepairStatus.PROPOSED));

        var res = o.approveRepairAction(taskId, "repair-acl-direction", "alice", "approved for lab retry");

        RepairAction action = res.getCurrentRepairPlan().getActions().get(0);
        assertEquals(RepairStatus.APPROVED, action.getStatus());
        assertEquals("APPROVED", action.getApprovalStatus());
        assertEquals("alice", action.getApprovedBy());
        assertEquals(1, res.getChangeHistory().size());
        assertEquals("REPAIR_APPROVED", res.getChangeHistory().get(0).getChangeType());
        assertTrue(res.getEvents().stream().anyMatch(event -> "repair.approved".equals(event.getEventType())));
    }

    @Test
    void rejectRepairActionShouldUpdateActionAndRecordChangeEvent() {
        var om = om();
        var w = ws(om); var rec = recS(rr, vv);
        var o = orchestratorWithChanges(w, rec, noOpInvoker(), om);
        var created = o.createTask("x", "lab", "u");
        String taskId = created.getTask().getTaskId();
        saveRepairPlanFixture(w, taskId, repairPlanForAction(taskId, "PATCH_CONFIG", WorkflowStage.CONFIGURATION,
                "HIGH", true, RepairStatus.PROPOSED));

        var res = o.rejectRepairAction(taskId, "repair-acl-direction", "bob", "too risky");

        RepairAction action = res.getCurrentRepairPlan().getActions().get(0);
        assertEquals(RepairStatus.REJECTED, action.getStatus());
        assertEquals("REJECTED", action.getApprovalStatus());
        assertEquals("bob", action.getRejectedBy());
        assertEquals(1, res.getChangeHistory().size());
        assertEquals("REPAIR_REJECTED", res.getChangeHistory().get(0).getChangeType());
        assertTrue(res.getEvents().stream().anyMatch(event -> "repair.rejected".equals(event.getEventType())));
    }

    @Test
    void applyRepairActionShouldRejectUnapprovedHighRiskAction() {
        var om = om();
        var w = ws(om); var rec = recS(rr, vv);
        var o = orchestratorWithChanges(w, rec, noOpInvoker(), om);
        var created = o.createTask("x", "lab", "u");
        String taskId = created.getTask().getTaskId();
        saveRepairPlanFixture(w, taskId, repairPlanForAction(taskId, "PATCH_CONFIG", WorkflowStage.CONFIGURATION,
                "HIGH", true, RepairStatus.PROPOSED));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> o.applyRepairAction(taskId, "repair-acl-direction"));

        assertEquals(ErrorCode.REPAIR_ACTION_NOT_APPROVED.getErrorCode(), ex.getErrorCode());
        assertEquals(RepairStatus.PROPOSED,
                w.getWorkspaceOrThrow(taskId).getCurrentRepairPlan().getActions().get(0).getStatus());
    }

    @Test
    void applyRepairActionShouldDispatchApprovedConfigurationRepairWithGuidance() {
        var om = om();
        var d = (AgentDiscoveryClient) t -> AgentCard.builder().agentName(t).serviceEndpoint("http://x/").healthStatus(AgentHealthStatus.UP).build();
        final java.util.List<com.yali.mactav.model.a2a.A2aRequest> requests = new java.util.ArrayList<>();
        var c = (A2aClient) (r, a) -> {
            requests.add(r);
            return A2aResponse.builder().success(true).taskId(r.getTaskId()).sourceAgent("ConfigurationAgent")
                    .targetAgent(r.getSourceAgent()).stage(WorkflowStage.CONFIGURATION).traceId(r.getTraceId())
                    .timestamp(LocalDateTime.now()).payloadJson(j(configSet(r))).build();
        };
        var w = ws(om); var rec = recS(rr, vv);
        var o = orchestratorWithChanges(w, rec, new RemoteAgentInvoker(d, c, new A2aResponseValidator()), om);
        var created = o.createTask("x", "lab", "u");
        String taskId = created.getTask().getTaskId();
        w.saveStageArtifact(taskId, ArtifactType.NETWORK_PLAN, WorkflowStage.PLANNING,
                executablePlan(taskId), "NetworkPlan for repair apply", "PlanningAgent", executionTraceRefs());
        saveRepairPlanFixture(w, taskId, repairPlanForAction(taskId, "PATCH_CONFIG", WorkflowStage.CONFIGURATION,
                "HIGH", true, RepairStatus.APPROVED));

        var res = o.applyRepairAction(taskId, "repair-acl-direction");

        assertEquals(1, requests.size());
        assertEquals(WorkflowStage.CONFIGURATION, requests.get(0).getStage());
        assertTrue(requests.get(0).getPayloadJson().contains("Repair guidance actionId=repair-acl-direction"));
        assertNotNull(res.getCurrentConfigSet());
        assertEquals(RepairStatus.APPLIED, res.getCurrentRepairPlan().getActions().get(0).getStatus());
        assertTrue(res.getChangeHistory().stream().anyMatch(change -> "REPAIR_APPLIED".equals(change.getChangeType())));
        assertTrue(res.getEvents().stream().anyMatch(event -> "repair.applied".equals(event.getEventType())));
    }

    @Test
    void applyRepairActionShouldWaitForUserForAskUserAction() {
        var om = om();
        var w = ws(om); var rec = recS(rr, vv);
        var o = orchestratorWithChanges(w, rec, noOpInvoker(), om);
        var created = o.createTask("x", "lab", "u");
        String taskId = created.getTask().getTaskId();
        saveRepairPlanFixture(w, taskId, repairPlanForAction(taskId, "ASK_USER", null,
                "LOW", false, RepairStatus.PROPOSED));

        var res = o.applyRepairAction(taskId, "repair-acl-direction");

        assertEquals(TaskStatus.WAITING_USER, res.getTask().getTaskStatus());
        assertEquals(RepairStatus.APPLIED, res.getCurrentRepairPlan().getActions().get(0).getStatus());
        assertTrue(res.getEvents().stream().anyMatch(event -> "repair.waiting_user".equals(event.getEventType())));
    }

    private static RemoteAgentInvoker noOpInvoker() {
        AgentDiscoveryClient d = t -> AgentCard.builder()
                .agentName(t)
                .serviceEndpoint("http://x/")
                .healthStatus(AgentHealthStatus.UP)
                .build();
        A2aClient c = (r, a) -> A2aResponse.builder().success(true).build();
        return new RemoteAgentInvoker(d, c, new A2aResponseValidator());
    }

    private static void saveRepairPlanFixture(NetworkWorkspaceService w, String taskId, RepairPlan repairPlan) {
        w.saveStageArtifact(taskId, ArtifactType.REPAIR_PLAN, WorkflowStage.HEALING,
                repairPlan, "repair plan fixture", "HealingAgent", executionTraceRefs());
    }

    private static RepairPlan repairPlanForAction(String taskId,
                                                  String actionType,
                                                  WorkflowStage targetStage,
                                                  String riskLevel,
                                                  boolean requiresApproval,
                                                  RepairStatus status) {
        return RepairPlan.builder()
                .taskId(taskId)
                .validationVersion(1)
                .repairVersion(1)
                .overallRepairStrategy("Repair action fixture")
                .failureAnalysis(java.util.List.of(FailureAnalysis.builder()
                        .analysisId("failure-acl-direction")
                        .failureType("ACL_DIRECTION_MISMATCH")
                        .rootCauseSummary("ACL direction is likely wrong.")
                        .relatedValidationItemIds(java.util.List.of("val-office-server"))
                        .build()))
                .actions(java.util.List.of(RepairAction.builder()
                        .actionId("repair-acl-direction")
                        .actionType(actionType)
                        .targetStage(targetStage)
                        .description("Use selected repair action to recover validation failure.")
                        .relatedFailureAnalysisId("failure-acl-direction")
                        .inputArtifactIds(java.util.List.of("val-office-server"))
                        .expectedOutputArtifactType(ArtifactType.CONFIG_SET)
                        .riskLevel(riskLevel)
                        .riskReason("Unit test repair action")
                        .requiresApproval(requiresApproval)
                        .status(status)
                        .traceRefs(TraceRefs.builder()
                                .validationItemIds(java.util.List.of("val-office-server"))
                                .repairActionIds(java.util.List.of("repair-acl-direction"))
                                .build())
                        .build()))
                .requiresUserConfirmation(requiresApproval)
                .stageStatus(StageStatus.SUCCESS)
                .createTime(LocalDateTime.now())
                .build();
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

    private static NetworkPlan executablePlan(String taskId) {
        return NetworkPlan.builder()
                .taskId(taskId)
                .planId("plan-execution")
                .planVersion(1)
                .targetEnvironment(TargetEnvironment.builder()
                        .adapterType("STRUCTURE_VALIDATION")
                        .simulationTarget("STRUCTURE_VALIDATION")
                        .build())
                .topology(Topology.builder()
                        .nodes(java.util.List.of(
                                TopologyNode.builder().id("h1").nodeType("host").traceRefs(executionTraceRefs()).build(),
                                TopologyNode.builder().id("h2").nodeType("host").traceRefs(executionTraceRefs()).build()))
                        .links(java.util.List.of(TopologyLink.builder()
                                .id("l1")
                                .sourceNode("h1")
                                .targetNode("h2")
                                .traceRefs(executionTraceRefs())
                                .build()))
                        .build())
                .traceRefs(executionTraceRefs())
                .stageStatus(StageStatus.SUCCESS)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .createdBy("PlanningAgent")
                .build();
    }

    private static ConfigSet executableConfigSet(String taskId) {
        return ConfigSet.builder()
                .taskId(taskId)
                .planId("plan-execution")
                .configSetId("config-set-execution")
                .planVersion(1)
                .configVersion(1)
                .traceRefs(executionTraceRefs())
                .stageStatus(StageStatus.SUCCESS)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .createdBy("ConfigurationAgent")
                .build();
    }

    private static TraceRefs executionTraceRefs() {
        return TraceRefs.builder()
                .planElementIds(java.util.List.of("plan-execution-node"))
                .configBlockIds(java.util.List.of("config-execution-block"))
                .testIds(java.util.List.of("test-ping"))
                .build();
    }

    private static NetworkIntent verificationIntent(String taskId) {
        return NetworkIntent.builder()
                .taskId(taskId)
                .intentVersion(1)
                .rawText("office can access server")
                .semanticIntentGraph(SemanticIntentGraph.builder()
                        .nodes(java.util.List.of(
                                IntentNode.builder().id("node-office").name("office").type("ZONE").build(),
                                IntentNode.builder().id("node-server").name("server").type("SERVICE").build()))
                        .relations(java.util.List.of(IntentRelation.builder()
                                .id("rel-office-server")
                                .type("ACCESS")
                                .source("node-office")
                                .target("node-server")
                                .action("ALLOW")
                                .build()))
                        .build())
                .stageStatus(StageStatus.SUCCESS)
                .createTime(LocalDateTime.now())
                .build();
    }

    private static com.yali.mactav.model.execution.ExecutionReport executionReport(String taskId) {
        return com.yali.mactav.model.execution.ExecutionReport.builder()
                .executionId("execution-verification")
                .taskId(taskId)
                .planId("plan-execution")
                .configSetId("config-set-execution")
                .planVersion(1)
                .configVersion(1)
                .executionVersion(1)
                .overallStatus(com.yali.mactav.model.execution.ExecutionStatus.SUCCESS)
                .testResults(java.util.List.of(com.yali.mactav.model.execution.TestResult.builder()
                        .testId("test-ping")
                        .status(com.yali.mactav.model.execution.TestResultStatus.PASSED)
                        .expectedResult("REACHABLE")
                        .actualResult("REACHABLE")
                        .traceRefs(executionTraceRefs())
                        .build()))
                .traceRefs(executionTraceRefs())
                .createTime(LocalDateTime.now())
                .build();
    }

    private static void saveFailedValidationFixture(NetworkWorkspaceService w, String taskId) {
        w.saveStageArtifact(taskId, ArtifactType.NETWORK_INTENT, WorkflowStage.INTENT,
                verificationIntent(taskId), "NetworkIntent for healing", "IntentAgent", executionTraceRefs());
        w.saveStageArtifact(taskId, ArtifactType.NETWORK_PLAN, WorkflowStage.PLANNING,
                executablePlan(taskId), "NetworkPlan for healing", "PlanningAgent", executionTraceRefs());
        w.saveStageArtifact(taskId, ArtifactType.CONFIG_SET, WorkflowStage.CONFIGURATION,
                executableConfigSet(taskId), "ConfigSet for healing", "ConfigurationAgent", executionTraceRefs());
        w.saveStageArtifact(taskId, ArtifactType.EXECUTION_REPORT, WorkflowStage.EXECUTION,
                executionReport(taskId), "ExecutionReport for healing", "ExecutionModule", executionTraceRefs());
        w.saveStageArtifact(taskId, ArtifactType.VALIDATION_REPORT, WorkflowStage.VERIFICATION,
                failedValidationReport(taskId), "failed validation", "VerificationAgent", executionTraceRefs());
    }

    private static RepairPlan repairPlan(com.yali.mactav.model.a2a.A2aRequest r) {
        TraceRefs traceRefs = TraceRefs.builder()
                .validationItemIds(java.util.List.of("val-office-server"))
                .repairActionIds(java.util.List.of("repair-acl-direction"))
                .build();
        return RepairPlan.builder()
                .taskId(r.getTaskId())
                .validationVersion(1)
                .repairVersion(r.getArtifactVersion())
                .overallRepairStrategy("Propose correcting the ACL direction and rerunning verification after approval.")
                .failureAnalysis(java.util.List.of(FailureAnalysis.builder()
                        .analysisId("failure-acl-direction")
                        .failureType("ACL_DIRECTION_MISMATCH")
                        .rootCauseSummary("The ACL appears to block the intended office to server path.")
                        .confidence(0.91)
                        .evidenceIds(java.util.List.of("evidence-test-ping"))
                        .build()))
                .actions(java.util.List.of(RepairAction.builder()
                        .actionId("repair-acl-direction")
                        .actionType("PATCH_CONFIG")
                        .targetStage(WorkflowStage.CONFIGURATION)
                        .description("Regenerate the affected ACL block with the intended source and destination direction.")
                        .relatedFailureAnalysisId("failure-acl-direction")
                        .inputArtifactIds(java.util.List.of("val-office-server"))
                        .expectedOutputArtifactType(ArtifactType.CONFIG_SET)
                        .riskLevel("HIGH")
                        .riskReason("Configuration change should be reviewed before application.")
                        .requiresApproval(true)
                        .traceRefs(traceRefs)
                        .build()))
                .requiresUserConfirmation(true)
                .stageStatus(StageStatus.SUCCESS)
                .createTime(LocalDateTime.now())
                .build();
    }

    private static com.yali.mactav.model.verification.ValidationReport passedValidationReport(String taskId) {
        return com.yali.mactav.model.verification.ValidationReport.builder()
                .validationId("validation-passed")
                .taskId(taskId)
                .validationVersion(1)
                .overallStatus(ValidationStatus.PASSED)
                .summary("Execution facts satisfy the intent.")
                .items(java.util.List.of(com.yali.mactav.model.verification.ValidationItem.builder()
                        .itemId("val-office-server")
                        .expected("REACHABLE")
                        .actual("REACHABLE")
                        .passed(true)
                        .relatedIntentRelationId("rel-office-server")
                        .relatedPlanElementIds(java.util.List.of("plan-execution-node"))
                        .relatedConfigBlockIds(java.util.List.of("config-execution-block"))
                        .relatedTestId("test-ping")
                        .build()))
                .traceRefs(TraceRefs.builder()
                        .validationItemIds(java.util.List.of("val-office-server"))
                        .testIds(java.util.List.of("test-ping"))
                        .build())
                .stageStatus(StageStatus.SUCCESS)
                .createTime(LocalDateTime.now())
                .build();
    }

    private static com.yali.mactav.model.verification.ValidationReport failedValidationReport(String taskId) {
        return com.yali.mactav.model.verification.ValidationReport.builder()
                .validationId("validation-failed")
                .taskId(taskId)
                .validationVersion(1)
                .overallStatus(ValidationStatus.FAILED)
                .summary("Office to server connectivity failed.")
                .items(java.util.List.of(com.yali.mactav.model.verification.ValidationItem.builder()
                        .itemId("val-office-server")
                        .expected("REACHABLE")
                        .actual("UNREACHABLE")
                        .passed(false)
                        .relatedIntentRelationId("rel-office-server")
                        .relatedPlanElementIds(java.util.List.of("plan-execution-node"))
                        .relatedConfigBlockIds(java.util.List.of("config-execution-block"))
                        .relatedTestId("test-ping")
                        .build()))
                .evidences(java.util.List.of(com.yali.mactav.model.verification.ValidationEvidence.builder()
                        .evidenceId("evidence-test-ping")
                        .evidenceType("PING_RESULT")
                        .source("verification-agent")
                        .rawValue("100% packet loss")
                        .normalizedValue("UNREACHABLE")
                        .relatedTestId("test-ping")
                        .build()))
                .suggestions(java.util.List.of("Inspect ACL direction for office to server access."))
                .traceRefs(TraceRefs.builder()
                        .validationItemIds(java.util.List.of("val-office-server"))
                        .intentRelationIds(java.util.List.of("rel-office-server"))
                        .planElementIds(java.util.List.of("plan-execution-node"))
                        .configBlockIds(java.util.List.of("config-execution-block"))
                        .testIds(java.util.List.of("test-ping"))
                        .build())
                .stageStatus(StageStatus.SUCCESS)
                .createTime(LocalDateTime.now())
                .build();
    }

    private static com.yali.mactav.model.verification.ValidationReport validationReport(
            com.yali.mactav.model.a2a.A2aRequest r) {
        return com.yali.mactav.model.verification.ValidationReport.builder()
                .validationId("validation-test")
                .taskId(r.getTaskId())
                .validationVersion(r.getArtifactVersion())
                .overallStatus(ValidationStatus.PASSED)
                .summary("Execution facts satisfy the intent.")
                .items(java.util.List.of(com.yali.mactav.model.verification.ValidationItem.builder()
                        .itemId("val-office-server")
                        .expected("REACHABLE")
                        .actual("REACHABLE")
                        .passed(true)
                        .relatedIntentRelationId("rel-office-server")
                        .relatedPlanElementIds(java.util.List.of("plan-execution-node"))
                        .relatedConfigBlockIds(java.util.List.of("config-execution-block"))
                        .relatedTestId("test-ping")
                        .build()))
                .traceRefs(TraceRefs.builder()
                        .validationItemIds(java.util.List.of("val-office-server"))
                        .intentRelationIds(java.util.List.of("rel-office-server"))
                        .planElementIds(java.util.List.of("plan-execution-node"))
                        .configBlockIds(java.util.List.of("config-execution-block"))
                        .testIds(java.util.List.of("test-ping"))
                        .build())
                .stageStatus(StageStatus.SUCCESS)
                .createTime(LocalDateTime.now())
                .build();
    }

}
