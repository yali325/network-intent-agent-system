package com.yali.mactav.intent.service;

import com.yali.mactav.agent.core.context.AgentRunContext;
import com.yali.mactav.agent.core.parser.AgentResponseParser;
import com.yali.mactav.agent.core.validator.AgentOutputValidator;
import com.yali.mactav.intent.parser.IntentResponseParser;
import com.yali.mactav.intent.request.IntentAgentRequest;
import com.yali.mactav.intent.schema.IntentResponseSchema;
import com.yali.mactav.intent.validator.IntentOutputValidator;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.intent.NetworkIntent;

/**
 * Default offline IntentService implementation that runs Parser -> Validator.
 *
 * <p>The implementation deliberately avoids model calls and Workspace writes.
 * A future real IntentAgent or A2A endpoint can call it after obtaining an
 * IntentResponseSchema from the framework boundary.</p>
 */
public class IntentServiceImpl implements IntentService {

    private final AgentResponseParser<IntentResponseSchema, NetworkIntent> parser;

    private final AgentOutputValidator<NetworkIntent> validator;

    public IntentServiceImpl() {
        this(new IntentResponseParser(), new IntentOutputValidator());
    }

    public IntentServiceImpl(AgentResponseParser<IntentResponseSchema, NetworkIntent> parser,
                             AgentOutputValidator<NetworkIntent> validator) {
        this.parser = parser;
        this.validator = validator;
    }

    @Override
    public NetworkIntent parse(IntentResponseSchema schema, IntentAgentRequest request) {
        NetworkIntent intent = parser.parse(schema, toContext(request));
        return validator.validateAndReturn(intent);
    }

    private AgentRunContext toContext(IntentAgentRequest request) {
        return AgentRunContext.builder()
                .taskId(request == null ? null : request.getTaskId())
                .stage(WorkflowStage.INTENT)
                .version(request == null ? null : request.getIntentVersion())
                .traceId(request == null ? null : request.getTraceId())
                .userInput(request == null ? null : request.getRawText())
                .workspaceSnapshot(request == null ? null : request.getWorkspaceSnapshot())
                .build();
    }
}
