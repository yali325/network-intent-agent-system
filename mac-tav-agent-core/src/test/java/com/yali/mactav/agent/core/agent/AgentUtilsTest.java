package com.yali.mactav.agent.core.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class AgentUtilsTest {

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
