package com.yali.mactav.model.intent;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentNode {

    private String id;

    private String name;

    private String type;

    private String description;

    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();
}
