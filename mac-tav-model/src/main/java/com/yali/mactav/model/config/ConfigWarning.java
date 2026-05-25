package com.yali.mactav.model.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigWarning {

    private String level;

    private String message;

    private String relatedBlockId;
}
