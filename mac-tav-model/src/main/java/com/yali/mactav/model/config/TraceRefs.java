package com.yali.mactav.model.config;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TraceRefs {

    private List<String> intentRelationIds;
    private List<String> planElementIds;
}
