package com.yali.mactav.orchestrator.job;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory task lock for explicit test or local in-memory profiles.
 */
public class InMemoryTaskRunLockService implements TaskRunLockService {

    private final ConcurrentMap<String, String> tokensByTaskId = new ConcurrentHashMap<>();

    @Override
    public Optional<TaskRunLock> tryLock(String taskId, String token) {
        String existing = tokensByTaskId.putIfAbsent(taskId, token);
        return existing == null
                ? Optional.of(new TaskRunLock(taskId, "inmemory:" + taskId, token))
                : Optional.empty();
    }

    @Override
    public boolean isLocked(String taskId) {
        return tokensByTaskId.containsKey(taskId);
    }

    @Override
    public void unlock(TaskRunLock lock) {
        if (lock != null) {
            tokensByTaskId.remove(lock.taskId(), lock.token());
        }
    }
}
