package com.yali.mactav.web.controller;

import com.yali.mactav.common.result.ApiResponse;
import com.yali.mactav.web.dto.view.ConfigBlocksView;
import com.yali.mactav.web.dto.view.TopologyView;
import com.yali.mactav.web.dto.view.WorkflowTraceView;
import com.yali.mactav.web.service.ViewQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web API controller for read-only frontend mission view projections.
 */
@RestController
@RequestMapping("/api/v1/views")
public class ViewController {

    private final ViewQueryService viewQueryService;

    public ViewController(ViewQueryService viewQueryService) {
        this.viewQueryService = viewQueryService;
    }

    @GetMapping("/{taskId}/trace")
    public ApiResponse<WorkflowTraceView> getWorkflowTrace(@PathVariable String taskId) {
        return ApiResponse.success(viewQueryService.getWorkflowTrace(taskId));
    }

    @GetMapping("/{taskId}/topology")
    public ApiResponse<TopologyView> getTopology(@PathVariable String taskId) {
        return ApiResponse.success(viewQueryService.getTopology(taskId));
    }

    @GetMapping("/{taskId}/config-blocks")
    public ApiResponse<ConfigBlocksView> getConfigBlocks(@PathVariable String taskId) {
        return ApiResponse.success(viewQueryService.getConfigBlocks(taskId));
    }
}
