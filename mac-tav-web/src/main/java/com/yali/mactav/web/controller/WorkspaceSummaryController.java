package com.yali.mactav.web.controller;

import com.yali.mactav.common.result.ApiResponse;
import com.yali.mactav.web.dto.view.WorkspaceSummaryView;
import com.yali.mactav.web.service.ViewQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web API controller for read-only workspace summary projections.
 */
@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceSummaryController {

    private final ViewQueryService viewQueryService;

    public WorkspaceSummaryController(ViewQueryService viewQueryService) {
        this.viewQueryService = viewQueryService;
    }

    @GetMapping("/{taskId}/summary")
    public ApiResponse<WorkspaceSummaryView> getWorkspaceSummary(@PathVariable String taskId) {
        return ApiResponse.success(viewQueryService.getWorkspaceSummary(taskId));
    }
}
