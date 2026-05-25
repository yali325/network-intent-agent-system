package com.yali.mactav.model.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TargetEnvironment {

    private String vendor;

    private String configStyle;

    private String simulationTarget;

    private String adapterType;
}
