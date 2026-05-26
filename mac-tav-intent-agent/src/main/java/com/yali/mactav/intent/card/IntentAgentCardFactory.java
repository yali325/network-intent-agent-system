package com.yali.mactav.intent.card;

import com.yali.mactav.intent.config.IntentAgentCardProperties;
import com.yali.mactav.model.agent.AgentCapability;
import com.yali.mactav.model.agent.AgentCard;
import com.yali.mactav.model.agent.AgentContract;
import com.yali.mactav.model.agent.AgentHealthStatus;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.intent.IntentAgentInvokePayload;
import com.yali.mactav.model.intent.NetworkIntent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Builds the published AgentCard for the IntentAgent service.
 *
 * <p>The card is declarative metadata for discovery. It must not perform Nacos
 * calls, invoke the model, or expose any credential material.</p>
 */
public class IntentAgentCardFactory {

    public static final String AGENT_NAME = "IntentAgent";

    public AgentCard create(IntentAgentCardProperties properties) {
        LocalDateTime now = LocalDateTime.now();
        return AgentCard.builder()
                .agentName(AGENT_NAME)
                .description("Parses user network intent into a validated NetworkIntent artifact")
                .capabilities(List.of(AgentCapability.builder()
                        .name("intent-extraction")
                        .description("Extract semantic intent graph, assumptions, constraints, and preferences")
                        .supportedStages(List.of(WorkflowStage.INTENT))
                        .inputTypes(List.of(IntentAgentInvokePayload.class.getName()))
                        .outputTypes(List.of(NetworkIntent.class.getName()))
                        .build()))
                .inputContract(AgentContract.builder()
                        .contractName("IntentAgentInvokePayload")
                        .payloadType(IntentAgentInvokePayload.class.getName())
                        .schemaVersion("v1")
                        .requiredFields(List.of("taskId", "rawText", "intentVersion", "traceId"))
                        .description("Intent-stage invocation payload serialized as A2aRequest.payloadJson")
                        .build())
                .outputContract(AgentContract.builder()
                        .contractName("NetworkIntent")
                        .payloadType(NetworkIntent.class.getName())
                        .schemaVersion("v1")
                        .requiredFields(List.of("taskId", "intentVersion", "semanticIntentGraph", "stageStatus"))
                        .description("Validated intent-stage artifact serialized as A2aResponse.payloadJson")
                        .build())
                .serviceEndpoint(properties.effectiveServiceEndpoint())
                .protocol("HTTP_JSON_A2A")
                .version(properties.getVersion())
                .healthStatus(AgentHealthStatus.UP)
                .metadata(Map.of(
                        "a2aPath", properties.getA2aPath(),
                        "nacosGroup", properties.getNacosGroup(),
                        "nacosDataId", properties.getNacosDataId()))
                .createTime(now)
                .updateTime(now)
                .build();
    }
}
