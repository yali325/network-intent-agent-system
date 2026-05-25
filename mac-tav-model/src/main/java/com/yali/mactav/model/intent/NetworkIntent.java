package com.yali.mactav.model.intent;

import com.yali.mactav.model.enums.StageStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Intent 阶段产物对象
 */
/**
 * Intent-stage artifact produced by IntentAgent after schema parsing and validation.
 *
 * <p>NetworkIntent captures business intent only. It must not include device
 * configs, CLI, topology implementation, or provider-specific model details.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkIntent {

    private String taskId;

    private Integer intentVersion;

    private String rawText;

    private SemanticIntentGraph semanticIntentGraph;

    @Builder.Default
    private List<Assumption> assumptions = new ArrayList<>();

    @Builder.Default
    private List<IntentConstraint> constraints = new ArrayList<>();

    @Builder.Default
    private List<IntentPreference> preferences = new ArrayList<>();

    private StageStatus stageStatus;

    private String traceId;

    private LocalDateTime createTime;
}
