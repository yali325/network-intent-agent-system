package com.yali.mactav.configuration.agent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yali.mactav.agent.core.context.AgentContext;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.plan.TargetEnvironment;
import org.junit.jupiter.api.Test;

class MockConfigurationAgentTest {

    @Test
    void mockConfigurationOutputsDeviceConfigsAndCommandBlocks() {
        NetworkPlan plan = NetworkPlan.builder()
                .taskId("task-10001")
                .planVersion(1)
                .targetEnvironment(TargetEnvironment.builder()
                        .vendor("Huawei")
                        .configStyle("CLI")
                        .simulationTarget("DRY_RUN")
                        .build())
                .build();

        ConfigSet configSet = new MockConfigurationAgent()
                .execute(AgentContext.of("task-10001", null), plan)
                .getData();

        assertFalse(configSet.getDeviceConfigs().isEmpty());
        assertTrue(configSet.getDeviceConfigs().stream()
                .allMatch(deviceConfig -> deviceConfig.getCommandBlocks() != null
                        && !deviceConfig.getCommandBlocks().isEmpty()));
        assertTrue(configSet.getDeviceConfigs().stream()
                .flatMap(deviceConfig -> deviceConfig.getCommandBlocks().stream())
                .allMatch(block -> block.getBlockId() != null
                        && block.getBlockType() != null
                        && block.getOrder() != null
                        && block.getTitle() != null
                        && block.getCommands() != null
                        && block.getExplanation() != null
                        && block.getRollbackCommands() != null
                        && block.getDependsOn() != null
                        && block.getTraceRefs() != null));
        assertNotNull(configSet.getEndpointConfigs());
        assertFalse(configSet.getWarnings().isEmpty());
    }
}
