package com.yali.mactav.model.config;

/**
 * Enumerates supported sources for configuration generation evidence.
 */
public enum GenerationSourceType {

    LLM,

    RAG,

    TEMPLATE,

    RULE,

    TOOL,

    MCP,

    MANUAL_OVERRIDE
}
