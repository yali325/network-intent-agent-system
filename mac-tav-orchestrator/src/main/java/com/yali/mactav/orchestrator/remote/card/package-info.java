/**
 * Agent Card registry lookup contracts for remote professional agents.
 *
 * <p>The preferred implementation adapts Spring AI Alibaba's official Nacos
 * AgentCardProvider. Legacy Nacos Config lookup remains only as a fallback and
 * must not leak registry SDK types into shared DTOs.</p>
 */
package com.yali.mactav.orchestrator.remote.card;
