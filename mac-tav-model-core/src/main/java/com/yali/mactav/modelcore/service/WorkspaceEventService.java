package com.yali.mactav.modelcore.service;

import com.yali.mactav.common.result.PageResult;
import com.yali.mactav.model.workspace.WorkspaceEvent;
import com.yali.mactav.modelcore.query.WorkspaceEventQuery;
import java.util.List;

/**
 * Appends and reads lightweight workspace timeline events.
 *
 * <p>Events are summaries for state history and UI/SSE projections; they must
 * not become the source of truth for artifacts or workflow decisions.</p>
 */
public interface WorkspaceEventService {

    WorkspaceEvent appendEvent(String taskId, WorkspaceEvent event);

    List<WorkspaceEvent> listEvents(String taskId);

    PageResult<WorkspaceEvent> listEvents(String taskId, WorkspaceEventQuery query);
}
