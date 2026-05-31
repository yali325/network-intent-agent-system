package com.yali.mactav.model.execution;

/**
 * Execution test categories that can later be produced by Mininet/Ryu adapters.
 */
public enum TestResultType {
    PING,
    TRACEROUTE,
    IPERF,
    FLOW_TABLE,
    CONTROLLER_STATE,
    TOPOLOGY_STATE
}
