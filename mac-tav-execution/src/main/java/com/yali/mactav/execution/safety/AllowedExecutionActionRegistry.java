package com.yali.mactav.execution.safety;

import com.yali.mactav.model.execution.ExecutionActionType;
import com.yali.mactav.model.execution.TestResultType;
import java.util.EnumSet;
import java.util.Set;

/**
 * Phase 6 allow-list for structured execution actions and test categories.
 */
public class AllowedExecutionActionRegistry {

    private final Set<ExecutionActionType> actionTypes;

    private final Set<TestResultType> testTypes;

    public AllowedExecutionActionRegistry() {
        this.actionTypes = EnumSet.of(
                ExecutionActionType.MININET_TOPOLOGY_START,
                ExecutionActionType.MININET_TOPOLOGY_STOP,
                ExecutionActionType.MININET_CLEANUP,
                ExecutionActionType.RYU_CONTROLLER_CHECK,
                ExecutionActionType.RYU_FLOW_QUERY,
                ExecutionActionType.PING_TEST,
                ExecutionActionType.TRACEROUTE_TEST,
                ExecutionActionType.IPERF_TEST,
                ExecutionActionType.TOPOLOGY_STATE_CHECK);
        this.testTypes = EnumSet.of(
                TestResultType.PING,
                TestResultType.TRACEROUTE,
                TestResultType.IPERF,
                TestResultType.FLOW_TABLE,
                TestResultType.CONTROLLER_STATE,
                TestResultType.TOPOLOGY_STATE);
    }

    public boolean isAllowed(ExecutionActionType actionType) {
        return actionType != null && actionTypes.contains(actionType);
    }

    public boolean isAllowed(TestResultType testType) {
        return testType != null && testTypes.contains(testType);
    }
}
