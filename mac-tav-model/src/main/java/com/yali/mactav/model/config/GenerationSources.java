package com.yali.mactav.model.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerationSources {

    private String sourceType;
    private String sourceName;
    private String description;
}
