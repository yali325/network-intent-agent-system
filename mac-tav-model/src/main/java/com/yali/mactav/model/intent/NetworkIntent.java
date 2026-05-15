package com.yali.mactav.model.intent;

import com.yali.mactav.common.enums.StageStatus;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetworkIntent {

    private String taskId;
    private Integer intentVersion;
    private String rawText;
    private SemanticIntentGraph semanticIntentGraph;
    private List<Assumption> assumptions;
    private StageStatus stageStatus;
}
