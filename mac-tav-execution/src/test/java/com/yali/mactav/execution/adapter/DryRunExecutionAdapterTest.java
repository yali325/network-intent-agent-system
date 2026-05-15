package com.yali.mactav.execution.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.config.EndpointConfig;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.plan.Topology;
import com.yali.mactav.model.plan.TopologyLink;
import com.yali.mactav.model.plan.TopologyNode;
import java.util.List;
import org.junit.jupiter.api.Test;

class DryRunExecutionAdapterTest {

    @Test
    void dryRunAdapterOutputsExecutionReport() {
        ExecutionReport report = new DryRunExecutionAdapter().adapt(plan(), configSet());

        assertEquals("DRY_RUN", report.getExecutionMode());
        assertNotNull(report.getExecutionPlan());
        assertNotNull(report.getRuntimeState());
        assertNotNull(report.getTestResult());
        assertEquals(3, report.getTestResult().getConnectivityTests().size());
        assertEquals(2, report.getTestResult().getPolicyTests().size());
        assertFalse(report.getExecutionPlan().getHostCommands().isEmpty());
    }

    private NetworkPlan plan() {
        return NetworkPlan.builder()
                .taskId("task-10001")
                .planVersion(1)
                .topology(Topology.builder()
                        .nodes(List.of(
                                TopologyNode.builder().id("R1").build(),
                                TopologyNode.builder().id("office-pc-1").build()
                        ))
                        .links(List.of(TopologyLink.builder().id("link-001").build()))
                        .build())
                .build();
    }

    private ConfigSet configSet() {
        return ConfigSet.builder()
                .taskId("task-10001")
                .configVersion(1)
                .endpointConfigs(List.of(EndpointConfig.builder()
                        .nodeId("office-pc-1")
                        .commands(List.of("ip addr add 192.168.10.10/24 dev eth0"))
                        .build()))
                .build();
    }
}
