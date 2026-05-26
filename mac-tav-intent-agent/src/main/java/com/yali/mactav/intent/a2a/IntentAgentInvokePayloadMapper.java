package com.yali.mactav.intent.a2a;

import com.yali.mactav.intent.request.IntentAgentRequest;
import com.yali.mactav.model.intent.IntentAgentInvokePayload;

/**
 * Maps the shared A2A invocation payload into IntentAgent's internal request type.
 *
 * <p>The mapper keeps the concrete agent request private to mac-tav-intent-agent
 * while allowing Orchestrator to depend only on mac-tav-model contracts.</p>
 */
public class IntentAgentInvokePayloadMapper {

    public IntentAgentRequest toRequest(IntentAgentInvokePayload payload) {
        if (payload == null) {
            return null;
        }
        return IntentAgentRequest.builder()
                .taskId(payload.getTaskId())
                .rawText(payload.getRawText())
                .intentVersion(payload.getIntentVersion())
                .traceId(payload.getTraceId())
                .userContext(payload.getUserContext())
                .workspaceSnapshot(payload.getWorkspaceSnapshot())
                .targetEnvironmentHint(payload.getTargetEnvironmentHint())
                .createdBy(payload.getCreatedBy())
                .build();
    }
}
