package com.yali.mactav.intent.service;

import com.yali.mactav.intent.request.IntentAgentRequest;
import com.yali.mactav.intent.schema.IntentResponseSchema;
import com.yali.mactav.model.intent.NetworkIntent;

/**
 * Internal IntentAgent module service for schema parsing and output validation.
 *
 * <p>This service belongs to mac-tav-intent-agent. It is not a Web controller,
 * not an Orchestrator local-agent shortcut, and must not write Workspace state.</p>
 */
public interface IntentService {

    NetworkIntent parse(IntentResponseSchema schema, IntentAgentRequest request);
}
