package com.yali.mactav.agent.core.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class AgentUtilsTest {

    @Test
    void callSchemaShouldDelegateToSchemaInvoker() {
        SchemaAgentInvoker invoker = new SchemaAgentInvoker() {
            @Override
            public <T> T call(String input, Class<T> outputType) {
                return outputType.cast(input);
            }
        };

        String actual = AgentUtils.callSchema(invoker, "schema", String.class);

        assertEquals("schema", actual);
    }

    @Test
    void callSchemaShouldConvertInvokerFailure() {
        SchemaAgentInvoker invoker = new SchemaAgentInvoker() {
            @Override
            public <T> T call(String input, Class<T> outputType) {
                throw new IllegalStateException("boom");
            }
        };

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> AgentUtils.callSchema(invoker, "schema", String.class)
        );

        assertEquals(ErrorCode.AGENT_EXECUTION_FAILED.getErrorCode(), exception.getErrorCode());
    }

    @Test
    void wrapExceptionShouldPreserveBusinessException() {
        BusinessException original = new BusinessException(ErrorCode.TOOL_CALL_FAILED);

        BusinessException actual = AgentUtils.wrapException(ErrorCode.MODEL_CALL_FAILED, "wrapped", original);

        assertEquals(original, actual);
    }

    @Test
    void reactAgentBuilderShouldRejectMissingChatModel() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> AgentUtils.reactAgentBuilder("IntentAgent", "test", null)
        );

        assertEquals(ErrorCode.MODEL_CALL_FAILED.getErrorCode(), exception.getErrorCode());
    }
}
