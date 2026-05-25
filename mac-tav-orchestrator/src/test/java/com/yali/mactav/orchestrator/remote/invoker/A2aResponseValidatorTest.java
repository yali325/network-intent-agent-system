package com.yali.mactav.orchestrator.remote.invoker;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.a2a.A2aResponse;
import com.yali.mactav.model.enums.WorkflowStage;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class A2aResponseValidatorTest {

    private final A2aResponseValidator validator = new A2aResponseValidator();

    @Test
    void validateShouldPassLegalSuccessResponse() {
        assertDoesNotThrow(() -> validator.validate(validResponse()));
    }

    @Test
    void validateShouldRejectMissingTaskIdStageOrPayloadJson() {
        assertInvalid(validResponse().toBuilder().taskId(null).build());
        assertInvalid(validResponse().toBuilder().stage(null).build());
        assertInvalid(validResponse().toBuilder().payloadJson(" ").build());
    }

    private void assertInvalid(A2aResponse response) {
        BusinessException exception = assertThrows(BusinessException.class, () -> validator.validate(response));

        assertEquals(ErrorCode.A2A_RESPONSE_INVALID.getErrorCode(), exception.getErrorCode());
    }

    private A2aResponse validResponse() {
        return A2aResponse.builder()
                .success(true)
                .taskId("task-001")
                .sourceAgent("IntentAgent")
                .targetAgent("Orchestrator")
                .stage(WorkflowStage.INTENT)
                .payloadJson("{\"intentVersion\":1}")
                .traceId("trace-001")
                .timestamp(LocalDateTime.of(2026, 5, 25, 1, 1))
                .build();
    }
}
