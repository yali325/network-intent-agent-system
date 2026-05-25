package com.yali.mactav.model.a2a;

import com.yali.mactav.model.enums.WorkflowStage;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Stable remote-agent request contract exchanged over the A2A boundary.
 *
 * <p>This DTO belongs to mac-tav-model and carries JSON payload text rather than
 * framework objects. It must not depend on Nacos, web, orchestrator, or concrete
 * agent implementations.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class A2aRequest {

    private String taskId;

    private String sourceAgent;

    private String targetAgent;

    private WorkflowStage stage;

    private Integer artifactVersion;

    private String payloadJson;

    private String traceId;

    private LocalDateTime timestamp;
}
