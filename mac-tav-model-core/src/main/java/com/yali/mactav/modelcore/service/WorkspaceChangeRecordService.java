package com.yali.mactav.modelcore.service;

import com.yali.mactav.model.workspace.WorkspaceChangeRecord;
import java.util.List;

/**
 * Stores audit records for workspace version switches, retries, and repair changes.
 *
 * <p>The service records what changed and why; orchestrator remains responsible
 * for deciding whether a change should happen.</p>
 */
public interface WorkspaceChangeRecordService {

    WorkspaceChangeRecord appendChange(String taskId, WorkspaceChangeRecord change);

    List<WorkspaceChangeRecord> listChanges(String taskId);
}
