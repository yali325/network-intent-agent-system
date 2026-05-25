package com.yali.mactav.modelcore.validator;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.workspace.NetworkArtifact;

/**
 * Validates the minimum envelope required before a NetworkArtifact can be saved.
 *
 * <p>This validator checks artifact metadata and serialized payload presence,
 * not business semantics inside payloadJson.</p>
 */
public class ArtifactValidator {

    public void validate(NetworkArtifact artifact) {
        if (artifact == null) {
            throw invalid("Artifact must not be null");
        }
        if (isBlank(artifact.getArtifactId())) {
            throw invalid("artifactId must not be blank");
        }
        if (isBlank(artifact.getTaskId())) {
            throw invalid("taskId must not be blank");
        }
        if (artifact.getArtifactType() == null) {
            throw invalid("artifactType must not be null");
        }
        if (artifact.getVersion() == null || artifact.getVersion() <= 0) {
            throw invalid("version must be greater than 0");
        }
        if (artifact.getStage() == null) {
            throw invalid("stage must not be null");
        }
        if (isBlank(artifact.getPayloadType())) {
            throw invalid("payloadType must not be blank");
        }
        if (isBlank(artifact.getPayloadJson())) {
            throw invalid("payloadJson must not be blank");
        }
    }

    private BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.ARTIFACT_INVALID, message);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
