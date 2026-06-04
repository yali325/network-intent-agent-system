package com.yali.mactav.orchestrator.job;

import java.util.Optional;

/**
 * Runtime mutual exclusion boundary for task-scoped workflow execution.
 */
public interface TaskRunLockService {

    Optional<TaskRunLock> tryLock(String taskId, String token);

    void unlock(TaskRunLock lock);
}
