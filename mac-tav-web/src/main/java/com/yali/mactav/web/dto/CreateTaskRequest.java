package com.yali.mactav.web.dto;

import lombok.Data;

/**
 * Web request body for creating a MAC-TAV workflow task.
 *
 * <p>The DTO is a frontend API shape only. It is converted by the controller
 * into Orchestrator calls and is not passed to concrete agent modules.</p>
 */
@Data
public class CreateTaskRequest {

    private String rawText;

    private String targetEnvironmentHint;

    private String createdBy;
}
