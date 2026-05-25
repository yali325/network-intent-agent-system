package com.yali.mactav.model.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationSource {

    private String sourceType;

    private String sourceName;

    private String description;

    private String artifactRef;
}
