package com.yali.mactav.model.verification;

import com.yali.mactav.model.enums.StageStatus;
import com.yali.mactav.model.enums.ValidationStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Verification-stage artifact summarizing whether execution satisfies intent.
 *
 * <p>ValidationReport carries findings and evidence for orchestration and
 * healing. It must not apply configuration or trigger repair execution itself.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationReport {

    private String validationId;

    private String taskId;

    private String executionId;

    private Integer intentVersion;

    private Integer planVersion;

    private Integer configVersion;

    private Integer executionVersion;

    private Integer validationVersion;

    private ValidationStatus overallStatus;

    private String summary;

    @Builder.Default
    private List<ValidationItem> items = new ArrayList<>();

    @Builder.Default
    private List<ValidationEvidence> evidences = new ArrayList<>();

    @Builder.Default
    private List<String> suggestions = new ArrayList<>();

    private com.yali.mactav.model.workspace.TraceRefs traceRefs;

    private StageStatus stageStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
