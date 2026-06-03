package com.yali.mactav.web.dto;

import lombok.Data;

/**
 * Request body for approving or rejecting a proposed repair action.
 */
@Data
public class RepairActionDecisionRequest {

    private String actor;

    private String comment;
}
