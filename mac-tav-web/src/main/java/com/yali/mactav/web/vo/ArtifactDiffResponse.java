package com.yali.mactav.web.vo;

import com.yali.mactav.orchestrator.service.ArtifactDiffResult;
import lombok.Builder;
import lombok.Data;

/**
 * First-version artifact diff response containing two payload snapshots.
 */
@Data
@Builder
public class ArtifactDiffResponse {

    private ArtifactPayloadResponse from;

    private ArtifactPayloadResponse to;

    public static ArtifactDiffResponse from(ArtifactDiffResult result) {
        return ArtifactDiffResponse.builder()
                .from(ArtifactPayloadResponse.from(result.from()))
                .to(ArtifactPayloadResponse.from(result.to()))
                .build();
    }
}
