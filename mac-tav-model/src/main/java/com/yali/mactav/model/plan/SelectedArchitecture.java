package com.yali.mactav.model.plan;

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
public class SelectedArchitecture {

    private String id;

    private String type;

    private String reason;

    @Builder.Default
    private List<String> tradeoffs = new ArrayList<>();
}
