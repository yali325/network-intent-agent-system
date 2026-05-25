package com.yali.mactav.model.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnforcementPoint {

    private String deviceId;

    private String interfaceName;

    private String direction;
}
