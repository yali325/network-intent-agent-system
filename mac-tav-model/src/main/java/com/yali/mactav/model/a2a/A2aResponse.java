package com.yali.mactav.model.a2a;

import com.yali.mactav.model.enums.WorkflowStage;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Stable remote-agent response contract returned through the A2A boundary.
 *
 * <p>The payload remains serialized JSON so parser and validator boundaries are
 * preserved after remote calls. This DTO does not represent orchestrator state
 * and must not update NetworkWorkspace directly.</p>
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class A2aResponse {

    private Boolean success;

    private String taskId;

    private String sourceAgent;

    private String targetAgent;

    private WorkflowStage stage;

    private String payloadJson;

    private String errorCode;

    private String message;

    private String traceId;

    private LocalDateTime timestamp;
}
