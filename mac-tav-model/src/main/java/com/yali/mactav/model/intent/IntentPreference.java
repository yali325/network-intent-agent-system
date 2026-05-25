package com.yali.mactav.model.intent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentPreference {

    private String id;

    private String type;

    private String value;

    private Integer priority;
}
