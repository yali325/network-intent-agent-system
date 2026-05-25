package com.yali.mactav.modelcore.artifact;

import com.yali.mactav.model.enums.ArtifactStatus;
import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.workspace.NetworkArtifact;
import com.yali.mactav.model.workspace.TraceRefs;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * Factory for building validated NetworkArtifact envelopes from typed DTO payloads.
 *
 * <p>The factory sets metadata and serialized payload only. It does not persist
 * artifacts, calculate versions, or make workflow decisions.</p>
 */
public class NetworkArtifactFactory {

    private final ArtifactPayloadSerializer serializer;

    public NetworkArtifactFactory(ArtifactPayloadSerializer serializer) {
        this.serializer = serializer;
    }

    public NetworkArtifact create(
            String taskId,
            ArtifactType artifactType,
            WorkflowStage stage,
            Integer version,
            Object payloadDto,
            String payloadSummary,
            String createdBy,
            TraceRefs traceRefs) {
        String artifactId = createArtifactId(artifactType);
        return NetworkArtifact.builder()
                .artifactId(artifactId)
                .taskId(taskId)
                .artifactType(artifactType)
                .version(version)
                .stage(stage)
                .status(ArtifactStatus.GENERATED)
                .payloadType(serializer.payloadType(payloadDto))
                .payloadJson(serializer.serialize(payloadDto))
                .payloadSummary(resolveSummary(artifactType, version, payloadSummary))
                .createTime(LocalDateTime.now())
                .createdBy(createdBy)
                .traceRefs(traceRefs)
                .build();
    }

    private String createArtifactId(ArtifactType artifactType) {
        String prefix = artifactType == null ? "artifact" : artifactType.name().toLowerCase(Locale.ROOT);
        return prefix + "-" + UUID.randomUUID();
    }

    private String resolveSummary(ArtifactType artifactType, Integer version, String payloadSummary) {
        if (payloadSummary != null && !payloadSummary.isBlank()) {
            return payloadSummary;
        }
        String type = artifactType == null ? "artifact" : artifactType.name();
        return type + " v" + version;
    }
}
