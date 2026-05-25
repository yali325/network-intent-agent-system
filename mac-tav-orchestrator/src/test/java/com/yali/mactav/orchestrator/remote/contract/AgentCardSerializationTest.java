package com.yali.mactav.orchestrator.remote.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yali.mactav.model.agent.AgentCapability;
import com.yali.mactav.model.agent.AgentCard;
import com.yali.mactav.model.agent.AgentContract;
import com.yali.mactav.model.agent.AgentHealthStatus;
import com.yali.mactav.model.enums.WorkflowStage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AgentCardSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void agentCardShouldSerializeAndDeserialize() throws Exception {
        AgentCard card = AgentCard.builder()
                .agentName("IntentAgent")
                .description("Parse user network intent")
                .capabilities(List.of(AgentCapability.builder()
                        .name("intent.parse")
                        .description("Parse raw text into NetworkIntent")
                        .supportedStages(List.of(WorkflowStage.INTENT))
                        .inputTypes(List.of("RawIntentRequest"))
                        .outputTypes(List.of("NetworkIntent"))
                        .build()))
                .inputContract(AgentContract.builder()
                        .contractName("IntentAgentInput")
                        .payloadType("IntentAgentRequest")
                        .schemaVersion("v1")
                        .requiredFields(List.of("taskId", "payloadJson"))
                        .description("Intent stage request payload")
                        .build())
                .outputContract(AgentContract.builder()
                        .contractName("IntentAgentOutput")
                        .payloadType("IntentResponseSchema")
                        .schemaVersion("v1")
                        .requiredFields(List.of("taskId", "semanticIntentGraph"))
                        .description("Intent stage response schema")
                        .build())
                .serviceEndpoint("http://127.0.0.1:18081/a2a")
                .protocol("A2A")
                .version("0.0.1")
                .healthStatus(AgentHealthStatus.UP)
                .metadata(Map.of("owner", "mac-tav"))
                .createTime(LocalDateTime.of(2026, 5, 25, 1, 0))
                .updateTime(LocalDateTime.of(2026, 5, 25, 1, 1))
                .build();

        String json = objectMapper.writeValueAsString(card);
        AgentCard decoded = objectMapper.readValue(json, AgentCard.class);

        assertTrue(json.contains("\"agentName\":\"IntentAgent\""));
        assertEquals(card.getAgentName(), decoded.getAgentName());
        assertEquals(card.getHealthStatus(), decoded.getHealthStatus());
        assertEquals(WorkflowStage.INTENT, decoded.getCapabilities().get(0).getSupportedStages().get(0));
        assertEquals("IntentAgentInput", decoded.getInputContract().getContractName());
    }
}
