package com.yali.mactav.agent.core.agent;

/**
 * Minimal schema-call abstraction used while concrete ReactAgent APIs remain isolated.
 *
 * <p>Real agents can adapt their framework call into this boundary. The
 * interface must not become a fake agent or a mock tool in production code.</p>
 */
@FunctionalInterface
public interface SchemaAgentInvoker {

    <T> T call(String input, Class<T> outputType);
}
