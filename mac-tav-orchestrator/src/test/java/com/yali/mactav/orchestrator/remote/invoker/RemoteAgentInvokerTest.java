package com.yali.mactav.orchestrator.remote.invoker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.a2a.A2aRequest;
import com.yali.mactav.model.a2a.A2aResponse;
import com.yali.mactav.model.agent.AgentCard;
import com.yali.mactav.model.agent.AgentHealthStatus;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.orchestrator.remote.card.AgentCardRegistryClient;
import com.yali.mactav.orchestrator.remote.discovery.RegistryAgentDiscoveryClient;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RemoteAgentInvokerTest {

    private final A2aResponseValidator responseValidator = new A2aResponseValidator();

    @Test
    void invokeShouldThrowAgentCardNotFoundWhenRegistryHasNoCard() {
        AgentCardRegistryClient emptyRegistry = new AgentCardRegistryClient() {
            @Override
            public Optional<AgentCard> findByAgentName(String agentName) {
                return Optional.empty();
            }

            @Override
            public List<AgentCard> listAvailableAgents() {
                return List.of();
            }
        };

        RemoteAgentInvoker invoker = new RemoteAgentInvoker(
                new RegistryAgentDiscoveryClient(emptyRegistry),
                (request, agentCard) -> successResponse(request),
                responseValidator
        );

        BusinessException exception = assertThrows(BusinessException.class, () -> invoker.invoke(request()));

        assertEquals(ErrorCode.AGENT_CARD_NOT_FOUND.getErrorCode(), exception.getErrorCode());
    }

    @Test
    void invokeShouldConvertA2aClientExceptionToA2aCallFailed() {
        RemoteAgentInvoker invoker = new RemoteAgentInvoker(
                targetAgentName -> agentCard(),
                (request, agentCard) -> {
                    throw new IllegalStateException("remote connection refused");
                },
                responseValidator
        );

        BusinessException exception = assertThrows(BusinessException.class, () -> invoker.invoke(request()));

        assertEquals(ErrorCode.A2A_CALL_FAILED.getErrorCode(), exception.getErrorCode());
    }

    private A2aRequest request() {
        return A2aRequest.builder()
                .taskId("task-001")
                .sourceAgent("Orchestrator")
                .targetAgent("IntentAgent")
                .stage(WorkflowStage.INTENT)
                .artifactVersion(1)
                .payloadJson("{\"rawText\":\"build network\"}")
                .traceId("trace-001")
                .timestamp(LocalDateTime.of(2026, 5, 25, 1, 0))
                .build();
    }

    private A2aResponse successResponse(A2aRequest request) {
        return A2aResponse.builder()
                .success(true)
                .taskId(request.getTaskId())
                .sourceAgent(request.getTargetAgent())
                .targetAgent(request.getSourceAgent())
                .stage(request.getStage())
                .payloadJson("{\"intentVersion\":1}")
                .traceId(request.getTraceId())
                .timestamp(LocalDateTime.of(2026, 5, 25, 1, 1))
                .build();
    }

    private AgentCard agentCard() {
        return AgentCard.builder()
                .agentName("IntentAgent")
                .description("Intent parsing agent")
                .serviceEndpoint("http://127.0.0.1:18081/a2a")
                .protocol("A2A")
                .version("0.0.1")
                .healthStatus(AgentHealthStatus.UP)
                .build();
    }
}
