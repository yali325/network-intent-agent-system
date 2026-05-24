package com.yali.mactav.agent.core.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class AgentUtilsTest {

    @Test
    void loadInstructionReadsClasspathPrompt() {
        String instruction = AgentUtils.loadInstruction("prompts/core-test-prompt.md");

        assertNotNull(instruction);
        assertEquals("core prompt fixture", instruction.trim());
    }

    @Test
    void wrapExceptionMapsKnownErrorCodeAndPreservesCause() {
        RuntimeException cause = new RuntimeException("tool failed");

        BusinessException exception = AgentUtils.wrapException(
                ErrorCode.TOOL_CALL_FAILED.name(),
                "Tool call failed",
                cause
        );

        assertEquals(ErrorCode.TOOL_CALL_FAILED, exception.getErrorCode());
        assertEquals("Tool call failed", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void wrapExceptionFallsBackForUnknownErrorCode() {
        BusinessException exception = AgentUtils.wrapException("UNKNOWN_CODE", "fallback", null);

        assertEquals(ErrorCode.PIPELINE_FAILED, exception.getErrorCode());
        assertEquals("fallback", exception.getMessage());
    }

    @Test
    void callSchemaParsesJsonFromFakeAgentInvoker() {
        TestSchema schema = AgentUtils.callSchema(
                input -> """
                        ```json
                        {
                          "id": "schema-001",
                          "version": 1
                        }
                        ```
                        """,
                "input",
                TestSchema.class
        );

        assertEquals("schema-001", schema.id());
        assertEquals(1, schema.version());
    }

    @Test
    void callSchemaRejectsInvalidStructuredOutput() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> AgentUtils.callSchema(input -> "not json", "input", TestSchema.class)
        );

        assertEquals(ErrorCode.AGENT_SCHEMA_INVALID, exception.getErrorCode());
    }

    @Test
    void callSchemaWrapsAgentInvocationFailure() {
        RuntimeException cause = new RuntimeException("model offline");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> AgentUtils.callSchema(input -> {
                    throw cause;
                }, "input", TestSchema.class)
        );

        assertEquals(ErrorCode.MODEL_CALL_FAILED, exception.getErrorCode());
        assertSame(cause, exception.getCause());
    }

    private record TestSchema(String id, int version) {
    }
}
