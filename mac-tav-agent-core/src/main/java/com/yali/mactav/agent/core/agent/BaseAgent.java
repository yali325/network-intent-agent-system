package com.yali.mactav.agent.core.agent;

import com.yali.mactav.agent.core.context.AgentContext;
import com.yali.mactav.agent.core.result.AgentResult;

/**
 * 定义所有 Agent 的统一入口
 * @param <I>
 * @param <O>
 */
public interface BaseAgent<I, O> {

    AgentResult<O> execute(AgentContext context, I input);
}
