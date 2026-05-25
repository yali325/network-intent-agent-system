package com.yali.mactav.orchestrator.remote.invoker;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.a2a.A2aRequest;
import com.yali.mactav.model.a2a.A2aResponse;
import java.util.Objects;

/**
 * Validates protocol-level consistency of remote A2A responses.
 *
 * <p>This validator checks envelope fields only. Business payload validation
 * remains the Parser/DTO/Validator responsibility of concrete agent modules.</p>
 */
public class A2aResponseValidator {

    public void validate(A2aResponse response) {
        if (response == null) {
            throw invalid("A2A response must not be null");
        }
        if (response.getSuccess() == null) {
            throw invalid("A2A response success flag must not be null");
        }
        requireText(response.getTaskId(), "A2A response taskId must not be blank");
        requireText(response.getSourceAgent(), "A2A response sourceAgent must not be blank");
        requireText(response.getTargetAgent(), "A2A response targetAgent must not be blank");
        if (response.getStage() == null) {
            throw invalid("A2A response stage must not be null");
        }
        if (Boolean.TRUE.equals(response.getSuccess())) {
            requireText(response.getPayloadJson(), "A2A response payloadJson must not be blank");
        }
        else {
            requireText(response.getErrorCode(), "Failed A2A response errorCode must not be blank");
        }
    }

    public void validateForRequest(A2aRequest request, A2aResponse response) {
        validate(response);
        if (request == null) {
            throw invalid("A2A request must not be null when validating response");
        }
        if (!Objects.equals(request.getTaskId(), response.getTaskId())) {
            throw invalid("A2A response taskId does not match request");
        }
        if (!Objects.equals(request.getStage(), response.getStage())) {
            throw invalid("A2A response stage does not match request");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw invalid(message);
        }
    }

    private BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.A2A_RESPONSE_INVALID, message);
    }
}
