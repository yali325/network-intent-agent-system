package com.yali.mactav.model.intent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntentRelation {

    private String id;
    private String type;
    private String source;
    private String target;
    private String action;
    private String service;
    private String description;
    private Boolean explicit;
}
