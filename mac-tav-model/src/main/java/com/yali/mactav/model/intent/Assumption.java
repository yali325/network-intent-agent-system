package com.yali.mactav.model.intent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Assumption {

    private String id;

    private String field;

    private String value;

    private String reason;

    private Double confidence;
}
