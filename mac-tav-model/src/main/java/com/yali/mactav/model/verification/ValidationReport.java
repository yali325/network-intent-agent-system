package com.yali.mactav.model.verification;

import com.yali.mactav.common.enums.StageStatus;
import com.yali.mactav.common.enums.ValidationStatus;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationReport {

    private String taskId;
    private Integer intentVersion;
    private Integer planVersion;
    private Integer configVersion;
    private Integer executionVersion;
    private Integer validationVersion;
    private ValidationStatus overallStatus;
    private String summary;
    private List<ValidationItem> items;
    private List<String> suggestions;
    private StageStatus stageStatus;
}
