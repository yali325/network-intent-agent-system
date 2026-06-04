package com.yali.mactav.web.vo;

import com.yali.mactav.model.workspace.NetworkArtifact;
import lombok.Builder;
import lombok.Data;

/**
 * Artifact payload response used only by explicit payload APIs.
 */
@Data
@Builder
public class ArtifactPayloadResponse {

    private ArtifactSummaryResponse metadata;

    private String payloadJson;

    public static ArtifactPayloadResponse from(NetworkArtifact artifact) {
        return ArtifactPayloadResponse.builder()
                .metadata(ArtifactSummaryResponse.from(artifact))
                .payloadJson(artifact.getPayloadJson())
                .build();
    }
}
