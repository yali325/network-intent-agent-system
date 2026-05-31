package com.yali.mactav.execution.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.execution.safety.ExecutionSafetyPolicy;
import com.yali.mactav.model.config.CommandBlock;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.config.DeviceConfig;
import com.yali.mactav.model.execution.ExecutionAction;
import com.yali.mactav.model.execution.ExecutionActionType;
import com.yali.mactav.model.execution.ExecutionEnvironmentType;
import com.yali.mactav.model.execution.ExecutionMode;
import com.yali.mactav.model.execution.ExecutionPlan;
import com.yali.mactav.model.execution.TestResultType;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.plan.TargetEnvironment;
import com.yali.mactav.model.plan.Topology;
import com.yali.mactav.model.plan.TopologyLink;
import com.yali.mactav.model.plan.TopologyNode;
import com.yali.mactav.model.workspace.TraceRefs;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests structure-only conversion from plan/config DTOs into ExecutionPlan.
 */
class NetworkExecutionPlanConverterTest {

    private final NetworkExecutionPlanConverter converter = new NetworkExecutionPlanConverter();

    @Test
    void convertsPlanAndConfigToSafeExecutionPlan() {
        ExecutionPlan executionPlan = converter.convert(
                networkPlan(),
                configSet(),
                1,
                ExecutionMode.STRUCTURE_VALIDATION,
                ExecutionEnvironmentType.STRUCTURE_VALIDATION,
                traceRefs(),
                Map.of("NETWORK_PLAN", "artifact-plan-1"));

        assertEquals("task-1", executionPlan.getTaskId());
        assertEquals("plan-1", executionPlan.getPlanId());
        assertEquals("config-set-1", executionPlan.getConfigSetId());
        assertEquals(ExecutionMode.STRUCTURE_VALIDATION, executionPlan.getExecutionMode());
        assertEquals(ExecutionEnvironmentType.STRUCTURE_VALIDATION, executionPlan.getTargetEnvironment());
        assertNotNull(executionPlan.getTopology());
        assertFalse(executionPlan.getActions().isEmpty());
        assertFalse(executionPlan.getCleanupActions().isEmpty());
        assertFalse(executionPlan.getTestCommands().isEmpty());
        assertEquals("artifact-plan-1#topology", executionPlan.getTopologyScriptRef());
        new ExecutionSafetyPolicy().validate(executionPlan);
    }

    @Test
    void conversionDoesNotCopyConfigCommandsIntoActionParameters() {
        ExecutionPlan executionPlan = converter.convert(
                networkPlan(),
                configSet(),
                1,
                ExecutionMode.STRUCTURE_VALIDATION,
                ExecutionEnvironmentType.STRUCTURE_VALIDATION,
                traceRefs(),
                Map.of());

        for (ExecutionAction action : executionPlan.getActions()) {
            assertFalse(action.getParameters().containsKey("command"));
            assertFalse(action.getParameters().containsKey("commands"));
        }
    }

    @Test
    void failsWhenPlanIdIsMissing() {
        NetworkPlan plan = networkPlan();
        plan.setPlanId(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> converter.convert(
                plan,
                configSet(),
                1,
                ExecutionMode.STRUCTURE_VALIDATION,
                ExecutionEnvironmentType.STRUCTURE_VALIDATION,
                traceRefs(),
                Map.of()));

        assertEquals("MAC_TAV_PARAM_INVALID", exception.getErrorCode());
    }

    @Test
    void failsWhenConfigSetIdIsMissing() {
        ConfigSet configSet = configSet();
        configSet.setConfigSetId(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> converter.convert(
                networkPlan(),
                configSet,
                1,
                ExecutionMode.STRUCTURE_VALIDATION,
                ExecutionEnvironmentType.STRUCTURE_VALIDATION,
                traceRefs(),
                Map.of()));

        assertEquals("MAC_TAV_PARAM_INVALID", exception.getErrorCode());
    }

    @Test
    void failsWhenTopologyIsMissing() {
        NetworkPlan plan = networkPlan();
        plan.setTopology(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> converter.convert(
                plan,
                configSet(),
                1,
                ExecutionMode.STRUCTURE_VALIDATION,
                ExecutionEnvironmentType.STRUCTURE_VALIDATION,
                traceRefs(),
                Map.of()));

        assertEquals("MAC_TAV_PARAM_INVALID", exception.getErrorCode());
    }

    private NetworkPlan networkPlan() {
        return NetworkPlan.builder()
                .planId("plan-1")
                .taskId("task-1")
                .targetEnvironment(TargetEnvironment.builder()
                        .adapterType("STRUCTURE_VALIDATION")
                        .simulationTarget("STRUCTURE_VALIDATION")
                        .build())
                .topology(Topology.builder()
                        .nodes(List.of(
                                TopologyNode.builder().id("host-office").nodeType("HOST").traceRefs(traceRefs()).build(),
                                TopologyNode.builder().id("host-server").nodeType("HOST").traceRefs(traceRefs()).build(),
                                TopologyNode.builder().id("switch-1").nodeType("SWITCH").traceRefs(traceRefs()).build()))
                        .links(List.of(TopologyLink.builder()
                                .id("link-1")
                                .sourceNode("host-office")
                                .targetNode("switch-1")
                                .traceRefs(traceRefs())
                                .build()))
                        .build())
                .traceRefs(traceRefs())
                .build();
    }

    private ConfigSet configSet() {
        return ConfigSet.builder()
                .configSetId("config-set-1")
                .planId("plan-1")
                .taskId("task-1")
                .deviceConfigs(List.of(DeviceConfig.builder()
                        .deviceId("switch-1")
                        .deviceName("switch-1")
                        .commandBlocks(List.of(CommandBlock.builder()
                                .blockId("config-block-1")
                                .commands(List.of("system-view", "acl 3000"))
                                .traceRefs(traceRefs())
                                .build()))
                        .traceRefs(traceRefs())
                        .build()))
                .traceRefs(traceRefs())
                .build();
    }

    private TraceRefs traceRefs() {
        return TraceRefs.builder()
                .intentRelationIds(List.of("intent-relation-1"))
                .planElementIds(List.of("plan-node-1"))
                .configBlockIds(List.of("config-block-1"))
                .testIds(List.of("test-" + TestResultType.PING.name().toLowerCase()))
                .build();
    }
}
