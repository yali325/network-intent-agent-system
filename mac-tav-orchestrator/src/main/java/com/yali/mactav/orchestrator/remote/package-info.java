/**
 * Orchestrator-side remote-agent adapter layer.
 *
 * <p>This package owns Agent Card lookup, discovery, A2A invocation, protocol
 * validation, and exception conversion. It must not construct prompts, call
 * ChatModel/ReactAgent, depend on concrete agent modules, or write workspace
 * state.</p>
 */
package com.yali.mactav.orchestrator.remote;
