package com.yali.mactav.model.agent;

import com.yali.mactav.model.enums.WorkflowStage;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Describes one capability exposed by an AgentCard.
 *
 * <p>The model module keeps this as declarative metadata only. Runtime discovery,
 * health checks, and remote invocation remain orchestrator-side concerns.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentCapability {

    private String name;

    private String description;

    @Builder.Default
    private List<WorkflowStage> supportedStages = new ArrayList<>();

    @Builder.Default
    private List<String> inputTypes = new ArrayList<>();

    @Builder.Default
    private List<String> outputTypes = new ArrayList<>();
}
