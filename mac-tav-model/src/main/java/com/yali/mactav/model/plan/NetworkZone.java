package com.yali.mactav.model.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkZone {

    private String id;

    private String name;

    private String mappedFromIntentNode;

    private String zoneType;

    private String description;
}
