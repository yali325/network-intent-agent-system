package com.yali.mactav.orchestrator.remote.invoker;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.a2a.A2aRequest;
import com.yali.mactav.model.a2a.A2aResponse;
import com.yali.mactav.model.agent.AgentCard;
import com.yali.mactav.model.agent.AgentHealthStatus;
import com.yali.mactav.orchestrator.remote.client.A2aClient;
import com.yali.mactav.orchestrator.remote.discovery.AgentDiscoveryClient;
import java.util.Objects;

/**
 * Orchestrator-side entry point for invoking a remote professional agent.
 *
 * <p>The invoker performs discovery, service-call delegation, envelope
 * validation, and error conversion. It must not construct prompts, call model
 * APIs, parse business DTOs, or write NetworkWorkspace state.</p>
 */
public class RemoteAgentInvoker {

    private final AgentDiscoveryClient discoveryClient;

    private final A2aClient a2aClient;

    private final A2aResponseValidator responseValidator;

    public RemoteAgentInvoker(
            AgentDiscoveryClient discoveryClient,
            A2aClient a2aClient,
            A2aResponseValidator responseValidator
    ) {
        this.discoveryClient = Objects.requireNonNull(discoveryClient, "discoveryClient must not be null");
        this.a2aClient = Objects.requireNonNull(a2aClient, "a2aClient must not be null");
        this.responseValidator = Objects.requireNonNull(responseValidator, "responseValidator must not be null");
    }

    public A2aResponse invoke(A2aRequest request) {
        validateRequest(request);
        AgentCard agentCard = discover(request.getTargetAgent());
        ensureCallable(agentCard);

        A2aResponse response;
        try {
            response = a2aClient.call(request, agentCard);
        }
        catch (Exception ex) {
            throw new BusinessException(
                    ErrorCode.A2A_CALL_FAILED,
                    "A2A call failed for agent: " + request.getTargetAgent(),
                    ex
            );
        }

        responseValidator.validateForRequest(request, response);
        return response;
    }

    private AgentCard discover(String targetAgentName) {
        try {
            AgentCard agentCard = discoveryClient.discover(targetAgentName);
            if (agentCard == null) {
                throw new BusinessException(ErrorCode.AGENT_CARD_NOT_FOUND, "Agent card not found: " + targetAgentName);
            }
            return agentCard;
        }
        catch (BusinessException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new BusinessException(
                    ErrorCode.AGENT_DISCOVERY_FAILED,
                    "Agent discovery failed for agent: " + targetAgentName,
                    ex
            );
        }
    }

    private void validateRequest(A2aRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "A2A request must not be null");
        }
        if (request.getTaskId() == null || request.getTaskId().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "A2A request taskId must not be blank");
        }
        if (request.getTargetAgent() == null || request.getTargetAgent().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "A2A request targetAgent must not be blank");
        }
        if (request.getStage() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "A2A request stage must not be null");
        }
    }

    private void ensureCallable(AgentCard agentCard) {
        if (agentCard.getHealthStatus() == AgentHealthStatus.DOWN) {
            throw new BusinessException(
                    ErrorCode.AGENT_SERVICE_UNAVAILABLE,
                    "Agent service is down: " + agentCard.getAgentName()
            );
        }
        if (agentCard.getServiceEndpoint() == null || agentCard.getServiceEndpoint().isBlank()) {
            throw new BusinessException(
                    ErrorCode.AGENT_SERVICE_UNAVAILABLE,
                    "Agent service endpoint is missing: " + agentCard.getAgentName()
            );
        }
    }
}
