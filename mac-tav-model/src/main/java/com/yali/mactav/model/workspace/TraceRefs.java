package com.yali.mactav.model.workspace;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cross-stage trace references linking intent, plan, config, execution, validation, and repair objects.
 *
 * <p>TraceRefs is a shared value object. It carries identifiers only and should
 * not pull in repositories, services, or live runtime objects.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceRefs {

    @Builder.Default
    private List<String> intentNodeIds = new ArrayList<>();

    @Builder.Default
    private List<String> intentRelationIds = new ArrayList<>();

    @Builder.Default
    private List<String> planElementIds = new ArrayList<>();

    @Builder.Default
    private List<String> configBlockIds = new ArrayList<>();

    @Builder.Default
    private List<String> testIds = new ArrayList<>();

    @Builder.Default
    private List<String> validationItemIds = new ArrayList<>();

    @Builder.Default
    private List<String> repairActionIds = new ArrayList<>();
}
