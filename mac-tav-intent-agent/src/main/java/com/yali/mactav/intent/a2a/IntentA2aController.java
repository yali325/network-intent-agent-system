package com.yali.mactav.intent.a2a;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.intent.agent.IntentAgent;
import com.yali.mactav.model.a2a.A2aRequest;
import com.yali.mactav.model.a2a.A2aResponse;
import com.yali.mactav.model.intent.IntentAgentInvokePayload;
import com.yali.mactav.model.intent.NetworkIntent;
import java.time.LocalDateTime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Legacy internal HTTP JSON A2A endpoint for invoking IntentAgent.
 *
 * <p>The preferred Phase 3 direction is Spring AI Alibaba's official A2A
 * server/registry auto-configuration. This controller remains opt-in only for
 * compatibility tests and must not be used as a frontend business API, write
 * NetworkWorkspace, or advance workflow state.</p>
 */
@RestController
@ConditionalOnProperty(prefix = "mactav.a2a.legacy.http-json", name = "enabled", havingValue = "true")
public class IntentA2aController {

    private static final String AGENT_NAME = "IntentAgent";

    public static final String INVOKE_PATH = "/internal/a2a/intent/invoke";

    private final IntentAgent intentAgent;

    private final ObjectMapper objectMapper;

    private final IntentAgentInvokePayloadMapper payloadMapper;

    public IntentA2aController(IntentAgent intentAgent,
                               ObjectMapper objectMapper,
                               IntentAgentInvokePayloadMapper payloadMapper) {
        this.intentAgent = intentAgent;
        this.objectMapper = objectMapper;
        this.payloadMapper = payloadMapper;
    }

    @PostMapping(INVOKE_PATH)
    public A2aResponse invoke(@RequestBody A2aRequest request) {
        try {
            validateRequest(request);
            IntentAgentInvokePayload payload = objectMapper.readValue(
                    request.getPayloadJson(),
                    IntentAgentInvokePayload.class);
            NetworkIntent intent = intentAgent.run(payloadMapper.toRequest(payload));
            return successResponse(request, objectMapper.writeValueAsString(intent));
        }
        catch (BusinessException ex) {
            return failureResponse(request, ex.getErrorCode(), ex.getMessage());
        }
        catch (JsonProcessingException ex) {
            return failureResponse(request, ErrorCode.A2A_RESPONSE_INVALID.getErrorCode(), "Invalid A2A payload JSON");
        }
        catch (RuntimeException ex) {
            return failureResponse(request, ErrorCode.AGENT_EXECUTION_FAILED.getErrorCode(), "IntentAgent invocation failed");
        }
    }

    private void validateRequest(A2aRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "A2A request must not be null");
        }
        if (request.getPayloadJson() == null || request.getPayloadJson().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "A2A request payloadJson must not be blank");
        }
    }

    private A2aResponse successResponse(A2aRequest request, String payloadJson) {
        return A2aResponse.builder()
                .success(true)
                .taskId(request.getTaskId())
                .sourceAgent(AGENT_NAME)
                .targetAgent(request.getSourceAgent())
                .stage(request.getStage())
                .payloadJson(payloadJson)
                .message("IntentAgent invocation succeeded")
                .traceId(request.getTraceId())
                .timestamp(LocalDateTime.now())
                .build();
    }

    private A2aResponse failureResponse(A2aRequest request, String errorCode, String message) {
        return A2aResponse.builder()
                .success(false)
                .taskId(request == null ? null : request.getTaskId())
                .sourceAgent(AGENT_NAME)
                .targetAgent(request == null ? null : request.getSourceAgent())
                .stage(request == null ? null : request.getStage())
                .errorCode(errorCode)
                .message(message)
                .traceId(request == null ? null : request.getTraceId())
                .timestamp(LocalDateTime.now())
                .build();
    }
}
