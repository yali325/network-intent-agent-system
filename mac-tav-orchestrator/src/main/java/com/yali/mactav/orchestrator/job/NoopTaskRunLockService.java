package com.yali.mactav.orchestrator.job;

import java.util.Optional;

/**
 * Explicit no-op task lock for tests that do not exercise mutual exclusion.
 */
public class NoopTaskRunLockService implements TaskRunLockService {

    @Override
    public Optional<TaskRunLock> tryLock(String taskId, String token) {
        return Optional.of(new TaskRunLock(taskId, "noop:" + taskId, token));
    }

    @Override
    public void unlock(TaskRunLock lock) {
        // Explicit no-op for mactav.task-lock.type=noop only.
    }
}
