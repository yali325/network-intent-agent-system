package com.yali.mactav.agent.core.agent;

@FunctionalInterface
public interface AgentInvoker {

    String call(String input) throws Exception;
}
