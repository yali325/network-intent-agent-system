package com.yali.mactav.model.intent;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SemanticIntentGraph {

    private List<IntentNode> nodes;
    private List<IntentRelation> relations;
}
