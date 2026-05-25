package com.yali.mactav.model.intent;

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
public class SemanticIntentGraph {

    @Builder.Default
    private List<IntentNode> nodes = new ArrayList<>();

    @Builder.Default
    private List<IntentRelation> relations = new ArrayList<>();
}
