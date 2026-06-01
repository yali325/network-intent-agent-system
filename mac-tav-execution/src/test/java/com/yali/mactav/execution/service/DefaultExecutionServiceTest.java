package com.yali.mactav.execution.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.execution.registry.ExecutionAdapterRegistry;
import com.yali.mactav.execution.registry.ExecutionAdapterRegistryFactory;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.execution.ExecutionEnvironmentType;
import com.yali.mactav.model.execution.ExecutionMode;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.execution.ExecutionStatus;
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
 * Tests the default execution service with structure-validation execution only.
 */
class DefaultExecutionServiceTest {

    @Test
    void usesStructureValidationAdapterToProduceExecutionReport() {
        ExecutionService service = new DefaultExecutionService(ExecutionAdapterRegistryFactory.structureValidationRegistry());

        ExecutionReport report = service.execute(
                "task-1",
                networkPlan(),
                configSet(),
                1,
                ExecutionEnvironmentType.STRUCTURE_VALIDATION,
                ExecutionMode.STRUCTURE_VALIDATION,
                traceRefs(),
                Map.of("NETWORK_PLAN", "artifact-plan-1", "CONFIG_SET", "artifact-config-1"));

        assertEquals(ExecutionStatus.SUCCESS, report.getOverallStatus());
        assertEquals(ExecutionEnvironmentType.STRUCTURE_VALIDATION, report.getEnvironmentType());
        assertEquals(1, report.getExecutionVersion());
        assertFalse(report.getTestResults().isEmpty());
    }

    @Test
    void failsClearlyWhenAdapterIsMissing() {
        ExecutionService service = new DefaultExecutionService(new ExecutionAdapterRegistry(List.of()));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.execute(
                "task-1",
                networkPlan(),
                configSet(),
                1,
                ExecutionEnvironmentType.MININET_RYU,
                ExecutionMode.MININET_RYU,
                traceRefs(),
                Map.of()));

        assertEquals("EXECUTION_ADAPTER_NOT_FOUND", exception.getErrorCode());
    }

    private NetworkPlan networkPlan() {
        return NetworkPlan.builder()
                .taskId("task-1")
                .planId("plan-1")
                .targetEnvironment(TargetEnvironment.builder().adapterType("STRUCTURE_VALIDATION").build())
                .topology(Topology.builder()
                        .nodes(List.of(
                                TopologyNode.builder().id("h1").nodeType("host").traceRefs(traceRefs()).build(),
                                TopologyNode.builder().id("h2").nodeType("host").traceRefs(traceRefs()).build()))
                        .links(List.of(TopologyLink.builder()
                                .id("l1")
                                .sourceNode("h1")
                                .targetNode("h2")
                                .traceRefs(traceRefs())
                                .build()))
                        .build())
                .traceRefs(traceRefs())
                .build();
    }

    private ConfigSet configSet() {
        return ConfigSet.builder()
                .taskId("task-1")
                .planId("plan-1")
                .configSetId("config-set-1")
                .traceRefs(traceRefs())
                .build();
    }

    private TraceRefs traceRefs() {
        return TraceRefs.builder()
                .planElementIds(List.of("plan-node-1"))
                .configBlockIds(List.of("config-block-1"))
                .testIds(List.of("test-ping"))
                .build();
    }
}
