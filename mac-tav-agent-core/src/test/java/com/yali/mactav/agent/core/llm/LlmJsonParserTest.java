package com.yali.mactav.agent.core.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yali.mactav.agent.core.properties.AgentModeProperties;
import com.yali.mactav.common.enums.StageStatus;
import com.yali.mactav.model.intent.NetworkIntent;
import org.junit.jupiter.api.Test;

class LlmJsonParserTest {

    @Test
    void parsesJsonObjectFromLlmOutput() {
        String output = """
                ```json
                {
                  "taskId": "task-llm-001",
                  "intentVersion": 1,
                  "rawText": "办公区可以访问服务器，访客区不能访问服务器",
                  "semanticIntentGraph": {
                    "nodes": [
                      {"id": "office", "name": "办公区", "type": "ZONE"},
                      {"id": "guest", "name": "访客区", "type": "ZONE"},
                      {"id": "server", "name": "服务器区", "type": "ZONE"}
                    ],
                    "relations": [
                      {
                        "id": "rel-001",
                        "type": "ACCESS",
                        "source": "office",
                        "target": "server",
                        "action": "ALLOW",
                        "service": "ANY",
                        "explicit": true
                      }
                    ]
                  },
                  "assumptions": [],
                  "stageStatus": "SUCCESS"
                }
                ```
                """;

        NetworkIntent intent = new LlmJsonParser().parseObject(output, NetworkIntent.class);

        assertEquals("task-llm-001", intent.getTaskId());
        assertEquals(StageStatus.SUCCESS, intent.getStageStatus());
        assertNotNull(intent.getSemanticIntentGraph());
        assertEquals(3, intent.getSemanticIntentGraph().getNodes().size());
        assertEquals(1, intent.getSemanticIntentGraph().getRelations().size());
    }

    @Test
    void intentModeDefaultsToMock() {
        AgentModeProperties properties = new AgentModeProperties();

        assertEquals("mock", properties.getIntentMode());
        properties.setIntentMode(null);
        assertEquals("mock", properties.getIntentMode());
    }
}
