package com.yali.mactav.modelcore.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.workspace.NetworkArtifact;
import org.junit.jupiter.api.Test;

class ArtifactValidatorTest {

    @Test
    void validateShouldRejectInvalidArtifact() {
        ArtifactValidator validator = new ArtifactValidator();
        NetworkArtifact artifact = NetworkArtifact.builder()
                .taskId("task-005")
                .artifactType(ArtifactType.NETWORK_INTENT)
                .version(0)
                .stage(WorkflowStage.INTENT)
                .payloadType("com.yali.mactav.model.intent.NetworkIntent")
                .payloadJson("{}")
                .build();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> validator.validate(artifact));

        assertEquals(ErrorCode.ARTIFACT_INVALID.getErrorCode(), exception.getErrorCode());
    }
}
