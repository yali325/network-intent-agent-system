package com.yali.mactav.verification.service;

import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.verification.request.VerificationAgentRequest;
import com.yali.mactav.verification.schema.VerificationResponseSchema;

/**
 * Internal VerificationAgent module service for parsing and output validation.
 */
public interface VerificationService {

    ValidationReport parse(VerificationResponseSchema schema, VerificationAgentRequest request);
}
