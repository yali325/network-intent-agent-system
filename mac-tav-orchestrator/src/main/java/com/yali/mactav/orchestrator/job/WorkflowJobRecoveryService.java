package com.yali.mactav.orchestrator.job;

/**
 * Startup recovery boundary for converging abandoned workflow jobs.
 */
public interface WorkflowJobRecoveryService {

    int recoverInterruptedJobs();
}
