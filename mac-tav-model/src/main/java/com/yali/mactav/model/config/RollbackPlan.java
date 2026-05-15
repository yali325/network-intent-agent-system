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
public class RollbackPlan {

    private String strategy;
    private List<String> blockIds;
    private List<RollbackBlock> rollbackBlocks;
    private String description;
}
