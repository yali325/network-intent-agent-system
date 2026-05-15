package com.yali.mactav.model.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressPlanItem {

    private String id;
    private String zoneId;
    private String subnet;
    private String gateway;
    private String sampleIp;
}
