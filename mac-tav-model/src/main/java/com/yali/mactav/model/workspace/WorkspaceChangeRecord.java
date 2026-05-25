package com.yali.mactav.model.workspace;

import com.yali.mactav.model.enums.WorkflowStage;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 状态或版本发生了什么变化 —— 版本变更 / 重跑 / 修复 / 人工确认记录
 */
/**
 * Audit record for version switches, retries, repair application, and manual changes.
 *
 * <p>It records why the workspace view changed; it must not replace artifact
 * history or orchestrator decision logic.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceChangeRecord {

    private String changeId;

    private String taskId;

    private WorkflowStage stage;

    private String changeType;

    private String fromArtifactId;

    private String toArtifactId;

    private String reason;

    private LocalDateTime createTime;

    private String createdBy;
}
