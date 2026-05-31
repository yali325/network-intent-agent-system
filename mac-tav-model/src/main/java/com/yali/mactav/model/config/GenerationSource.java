package com.yali.mactav.model.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Source metadata explaining how a configuration element was generated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationSource {

    private GenerationSourceType sourceType;

    private String sourceId;

    private String sourceDescription;

    private String sourceName;

    private String description;

    private String artifactRef;

    private Double confidence;
}
