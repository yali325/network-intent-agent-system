package com.yali.mactav.model.config;

import com.yali.mactav.model.workspace.TraceRefs;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandBlock {

    private String blockId;

    private String blockType;

    private Integer order;

    private String title;

    @Builder.Default
    private List<String> commands = new ArrayList<>();

    private String explanation;

    @Builder.Default
    private List<String> rollbackCommands = new ArrayList<>();

    private String rollbackStrategy;

    @Builder.Default
    private List<String> dependsOn = new ArrayList<>();

    private TraceRefs traceRefs;

    private String riskLevel;
}
