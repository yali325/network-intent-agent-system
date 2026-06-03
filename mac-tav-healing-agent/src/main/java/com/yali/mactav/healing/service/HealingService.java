package com.yali.mactav.healing.service;

import com.yali.mactav.healing.request.HealingAgentRequest;
import com.yali.mactav.healing.schema.HealingResponseSchema;
import com.yali.mactav.model.healing.RepairPlan;

/**
 * Module-local service boundary for parsing HealingAgent structured output.
 */
public interface HealingService {

    RepairPlan parse(HealingResponseSchema schema, HealingAgentRequest request);
}
