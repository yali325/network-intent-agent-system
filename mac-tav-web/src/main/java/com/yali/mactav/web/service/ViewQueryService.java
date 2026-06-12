package com.yali.mactav.web.service;

import com.yali.mactav.web.dto.view.ConfigBlocksView;
import com.yali.mactav.web.dto.view.ExecutionLogsView;
import com.yali.mactav.web.dto.view.TopologyView;
import com.yali.mactav.web.dto.view.WorkflowTraceView;
import com.yali.mactav.web.dto.view.WorkspaceSummaryView;

/**
 * Read-side facade for frontend-only mission view projections.
 */
public interface ViewQueryService {

    WorkspaceSummaryView getWorkspaceSummary(String taskId);

    WorkflowTraceView getWorkflowTrace(String taskId);

    TopologyView getTopology(String taskId);

    ConfigBlocksView getConfigBlocks(String taskId);

    ExecutionLogsView getExecutionLogs(String taskId);
}
