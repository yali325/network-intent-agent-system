package com.yali.mactav.web.dto.view;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Shared readiness marker for frontend-only view DTOs.
 */
@Data
@Builder
public class ViewReadiness {

    private String status;

    private boolean ready;

    private String reasonCode;

    private String message;

    private List<String> missingArtifacts;
}
