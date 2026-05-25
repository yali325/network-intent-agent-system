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
public class IntentRelation {

    private String id;

    private String type;

    private String source;

    private String target;

    private String action;

    private String service;

    private Integer priority;

    private Boolean explicit;

    private String description;

    @Builder.Default
    private List<IntentConstraint> constraints = new ArrayList<>();
}
