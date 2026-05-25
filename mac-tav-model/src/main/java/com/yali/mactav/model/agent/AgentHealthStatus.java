package com.yali.mactav.model.agent;

/**
 * Coarse health state published in an AgentCard.
 *
 * <p>It is shared DTO metadata; active health probing and failover decisions
 * belong to orchestrator-side discovery/invocation adapters.</p>
 */
public enum AgentHealthStatus {
    UP,
    DOWN,
    UNKNOWN
}
