package com.yali.mactav.web.controller;

import com.yali.mactav.common.result.ApiResponse;
import com.yali.mactav.web.dto.view.ExecutionLogsView;
import com.yali.mactav.web.service.ViewQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web API controller for read-only execution log projections.
 */
@RestController
@RequestMapping("/api/v1/executions")
public class ExecutionLogsController {

    private final ViewQueryService viewQueryService;

    public ExecutionLogsController(ViewQueryService viewQueryService) {
        this.viewQueryService = viewQueryService;
    }

    @GetMapping("/{taskId}/logs")
    public ApiResponse<ExecutionLogsView> getExecutionLogs(@PathVariable String taskId) {
        return ApiResponse.success(viewQueryService.getExecutionLogs(taskId));
    }
}
