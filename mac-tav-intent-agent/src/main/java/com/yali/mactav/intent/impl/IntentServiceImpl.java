package com.yali.mactav.intent.impl;

import com.yali.mactav.agent.core.context.AgentContext;
import com.yali.mactav.agent.core.result.AgentResult;
import com.yali.mactav.intent.agent.IntentAgent;
import com.yali.mactav.intent.agent.MockIntentAgent;
import com.yali.mactav.intent.service.IntentService;
import com.yali.mactav.model.intent.NetworkIntent;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class IntentServiceImpl implements IntentService {

    private final IntentAgent intentAgent;

    public IntentServiceImpl() {
        this(new MockIntentAgent());
    }

    public IntentServiceImpl(IntentAgent intentAgent) {
        this.intentAgent = intentAgent;
    }

    @Override
    public NetworkIntent parseIntent(String taskId, String rawText) {
        AgentContext context = AgentContext.of(taskId, rawText);
        AgentResult<NetworkIntent> result = intentAgent.execute(context, rawText);
        if (!result.isSuccess()) {
            throw new IllegalStateException(result.getMessage());
        }
        return result.getData();
    }
}
