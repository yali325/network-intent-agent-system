package com.yali.mactav.web.controller;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.common.result.ApiResponse;
import com.yali.mactav.common.result.PageResult;
import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.workspace.NetworkArtifact;
import com.yali.mactav.modelcore.query.ArtifactQuery;
import com.yali.mactav.orchestrator.service.WorkflowQueryService;
import com.yali.mactav.web.vo.ArtifactDiffResponse;
import com.yali.mactav.web.vo.ArtifactPayloadResponse;
import com.yali.mactav.web.vo.ArtifactSummaryResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web API controller for querying versioned workflow artifacts.
 */
@RestController
@RequestMapping("/api/v1/artifacts")
public class ArtifactController {

    private final WorkflowQueryService workflowQueryService;

    public ArtifactController(WorkflowQueryService workflowQueryService) {
        this.workflowQueryService = workflowQueryService;
    }

    @GetMapping("/{taskId}")
    public ApiResponse<PageResult<ArtifactSummaryResponse>> listArtifacts(
            @PathVariable String taskId,
            @RequestParam(required = false) String artifactType,
            @RequestParam(required = false) String stage,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<NetworkArtifact> result = workflowQueryService.listArtifacts(
                taskId,
                new ArtifactQuery(parseArtifactType(artifactType), parseStage(stage), page, size));
        return ApiResponse.success(toSummaryPage(result));
    }

    @GetMapping("/{taskId}/{artifactId}")
    public ApiResponse<ArtifactSummaryResponse> getArtifact(@PathVariable String taskId,
                                                            @PathVariable String artifactId) {
        return ApiResponse.success(ArtifactSummaryResponse.from(workflowQueryService.getArtifact(taskId, artifactId)));
    }

    @GetMapping("/{taskId}/{artifactId}/payload")
    public ApiResponse<ArtifactPayloadResponse> getArtifactPayload(@PathVariable String taskId,
                                                                   @PathVariable String artifactId) {
        return ApiResponse.success(ArtifactPayloadResponse.from(workflowQueryService.getArtifact(taskId, artifactId)));
    }

    @GetMapping("/{taskId}/current/{artifactType}")
    public ApiResponse<ArtifactSummaryResponse> getCurrentArtifact(@PathVariable String taskId,
                                                                   @PathVariable String artifactType) {
        return ApiResponse.success(ArtifactSummaryResponse.from(
                workflowQueryService.getCurrentArtifact(taskId, parseRequiredArtifactType(artifactType))));
    }

    @GetMapping("/{taskId}/{artifactId}/versions")
    public ApiResponse<PageResult<ArtifactSummaryResponse>> listArtifactVersions(
            @PathVariable String taskId,
            @PathVariable String artifactId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(toSummaryPage(
                workflowQueryService.listArtifactVersions(taskId, artifactId, page, size)));
    }

    @GetMapping("/{taskId}/{artifactId}/diff")
    public ApiResponse<ArtifactDiffResponse> diffArtifactVersions(
            @PathVariable String taskId,
            @PathVariable String artifactId,
            @RequestParam(required = false) Integer fromVersion,
            @RequestParam(required = false) Integer toVersion) {
        return ApiResponse.success(ArtifactDiffResponse.from(
                workflowQueryService.diffArtifactVersions(taskId, artifactId, fromVersion, toVersion)));
    }

    private PageResult<ArtifactSummaryResponse> toSummaryPage(PageResult<NetworkArtifact> result) {
        List<ArtifactSummaryResponse> items = result.getItems().stream()
                .map(ArtifactSummaryResponse::from)
                .toList();
        return PageResult.<ArtifactSummaryResponse>builder()
                .items(items)
                .page(result.getPage())
                .size(result.getSize())
                .total(result.getTotal())
                .build();
    }

    private ArtifactType parseArtifactType(String value) {
        return value == null || value.isBlank() ? null : parseRequiredArtifactType(value);
    }

    private ArtifactType parseRequiredArtifactType(String value) {
        try {
            return ArtifactType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "Invalid artifactType: " + value);
        }
    }

    private WorkflowStage parseStage(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return WorkflowStage.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "Invalid stage: " + value);
        }
    }
}
