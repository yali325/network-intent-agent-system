package com.yali.mactav.orchestrator.job;

/**
 * Tokenized task run lock acquired before creating an asynchronous job.
 */
public record TaskRunLock(String taskId, String lockKey, String token) {
}
