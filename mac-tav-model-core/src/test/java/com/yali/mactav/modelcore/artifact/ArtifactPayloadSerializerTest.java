package com.yali.mactav.modelcore.artifact;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yali.mactav.model.enums.StageStatus;
import com.yali.mactav.model.intent.NetworkIntent;
import org.junit.jupiter.api.Test;

class ArtifactPayloadSerializerTest {

    @Test
    void serializeShouldWriteStageDtoJson() {
        ArtifactPayloadSerializer serializer = new ArtifactPayloadSerializer();
        NetworkIntent intent = NetworkIntent.builder()
                .taskId("task-004")
                .intentVersion(1)
                .rawText("Connect branches with redundant links")
                .stageStatus(StageStatus.SUCCESS)
                .build();

        String json = serializer.serialize(intent);

        assertTrue(json.contains("\"taskId\":\"task-004\""));
        assertTrue(json.contains("\"rawText\":\"Connect branches with redundant links\""));
    }
}
