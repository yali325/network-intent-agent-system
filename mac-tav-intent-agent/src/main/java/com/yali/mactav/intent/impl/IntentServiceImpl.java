package com.yali.mactav.intent.impl;

import com.yali.mactav.agent.core.context.AgentContext;
import com.yali.mactav.agent.core.properties.AgentModeProperties;
import com.yali.mactav.agent.core.result.AgentResult;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.intent.agent.IntentAgent;
import com.yali.mactav.intent.agent.LlmIntentAgent;
import com.yali.mactav.intent.agent.MockIntentAgent;
import com.yali.mactav.intent.service.IntentService;
import com.yali.mactav.model.intent.NetworkIntent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class IntentServiceImpl implements IntentService {

    private final IntentAgent fixedAgent;
    private final AgentModeProperties modeProperties;
    private final MockIntentAgent mockIntentAgent;
    private final LlmIntentAgent llmIntentAgent;

    public IntentServiceImpl() {
        this(new MockIntentAgent());
    }

    public IntentServiceImpl(IntentAgent intentAgent) {
        this.fixedAgent = intentAgent;
        this.modeProperties = null;
        this.mockIntentAgent = null;
        this.llmIntentAgent = null;
    }

    @Autowired
    public IntentServiceImpl(AgentModeProperties modeProperties,
                             MockIntentAgent mockIntentAgent,
                             LlmIntentAgent llmIntentAgent) {
        this.fixedAgent = null;
        this.modeProperties = modeProperties;
        this.mockIntentAgent = mockIntentAgent;
        this.llmIntentAgent = llmIntentAgent;
    }

    @Override
    public NetworkIntent parseIntent(String taskId, String rawText) {
        AgentContext context = AgentContext.of(taskId, rawText);
        AgentResult<NetworkIntent> result = selectAgent().execute(context, rawText);
        if (!result.isSuccess()) {
            throw new BusinessException(resolveErrorCode(result.getErrorCode()), result.getMessage());
        }
        return result.getData();
    }

    private IntentAgent selectAgent() {
        if (fixedAgent != null) {
            return fixedAgent;
        }
        if (modeProperties == null || modeProperties.isMockIntentMode()) {
            return mockIntentAgent;
        }
        if (modeProperties.isLlmIntentMode()) {
            return llmIntentAgent;
        }
        throw new BusinessException(
                ErrorCode.PIPELINE_FAILED,
                "Unsupported mactav.agent.intent-mode: " + modeProperties.getIntentMode()
        );
    }

    private ErrorCode resolveErrorCode(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return ErrorCode.PIPELINE_FAILED;
        }
        try {
            return ErrorCode.valueOf(errorCode);
        } catch (IllegalArgumentException ex) {
            return ErrorCode.PIPELINE_FAILED;
        }
    }
}
