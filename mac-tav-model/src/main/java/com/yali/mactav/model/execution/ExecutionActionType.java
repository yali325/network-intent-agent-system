package com.yali.mactav.model.execution;

/**
 * Allowed structured execution action categories.
 */
public enum ExecutionActionType {
    CREATE_TOPOLOGY,
    START_CONTROLLER,
    START_MININET,
    APPLY_FLOW_RULE,
    APPLY_DEVICE_CONFIG,
    RUN_TEST,
    COLLECT_STATE,
    CLEANUP,
    MININET_TOPOLOGY_START,
    MININET_TOPOLOGY_STOP,
    MININET_CLEANUP,
    RYU_CONTROLLER_CHECK,
    RYU_FLOW_QUERY,
    PING_TEST,
    TRACEROUTE_TEST,
    IPERF_TEST,
    TOPOLOGY_STATE_CHECK
}
