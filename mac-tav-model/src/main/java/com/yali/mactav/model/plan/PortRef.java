package com.yali.mactav.model.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortRef {

    private String deviceId;

    private String interfaceName;

    private String description;
}
