package com.yali.mactav.agent.core.parser;

import com.yali.mactav.agent.core.context.AgentRunContext;

/**
 * Converts a structured model response schema into a MAC-TAV stage DTO.
 *
 * <p>Concrete agent modules provide business parsers. Agent Core only defines
 * the boundary so A2A/local calls still pass through ResponseSchema -> DTO.</p>
 */
public interface AgentResponseParser<S, D> {

    D parse(S schema, AgentRunContext context);
}
