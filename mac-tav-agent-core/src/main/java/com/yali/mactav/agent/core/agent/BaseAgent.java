package com.yali.mactav.agent.core.agent;

import com.yali.mactav.agent.core.context.AgentContext;
import com.yali.mactav.agent.core.result.AgentResult;

public interface BaseAgent<I, O> {

    AgentResult<O> execute(AgentContext context, I input);
}
