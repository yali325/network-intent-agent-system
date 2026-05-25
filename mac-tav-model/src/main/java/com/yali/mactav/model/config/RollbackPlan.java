package com.yali.mactav.model.config;

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
public class RollbackPlan {

    private String strategy;

    @Builder.Default
    private List<RollbackBlock> rollbackBlocks = new ArrayList<>();

    private String description;
}
