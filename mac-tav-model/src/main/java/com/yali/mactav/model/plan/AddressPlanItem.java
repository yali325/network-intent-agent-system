package com.yali.mactav.model.plan;

import com.yali.mactav.model.workspace.TraceRefs;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressPlanItem {

    private String id;

    private String zoneId;

    private String subnet;

    private String gateway;

    @Builder.Default
    private List<String> dnsServers = new ArrayList<>();

    private String exampleHostAddress;

    @Builder.Default
    private List<String> hostAddressHints = new ArrayList<>();

    private TraceRefs traceRefs;
}
