package com.yali.mactav.model.config;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandBlock {

    private String blockId;
    private String blockType;
    private Integer order;
    private String title;
    private List<String> commands;
    private String explanation;
    private List<String> rollbackCommands;
    private List<String> dependsOn;
    private TraceRefs traceRefs;
}
