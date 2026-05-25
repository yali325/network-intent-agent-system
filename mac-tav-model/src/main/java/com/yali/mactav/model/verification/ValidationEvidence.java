package com.yali.mactav.model.verification;

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
public class ValidationEvidence {

    private String evidenceId;

    private String evidenceType;

    private String source;

    private String rawValue;

    private String normalizedValue;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    private String relatedTestId;

    private String relatedRuntimeObjectId;
}
