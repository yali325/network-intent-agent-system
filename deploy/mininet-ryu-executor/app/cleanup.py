"""Controlled cleanup for Mininet and executor-owned runtime state."""

from __future__ import annotations

from datetime import datetime, timezone

from .mininet_runner import MininetRunner
from .runtime_state import ExecutorRuntimeState
from .schemas import CleanupRequest, ExecutionError, ExecutionRunResponse, RuntimeState
from .settings import Settings, settings


class CleanupRunner:
    """Stops executor-owned Mininet state without killing the shared Ryu process."""

    def __init__(self, mininet_runner: MininetRunner, config: Settings = settings) -> None:
        self._mininet_runner = mininet_runner
        self._config = config

    def cleanup_runtime(self, runtime: ExecutorRuntimeState) -> list[ExecutionError]:
        """Cleanup active Mininet state without accepting external commands."""

        errors = []
        errors.extend(self._mininet_runner.stop_network(runtime))
        runtime.clear_transient_state()
        return errors

    def cleanup(self, request: CleanupRequest, runtime: ExecutorRuntimeState) -> ExecutionRunResponse:
        """Return a structured cleanup response for POST /executions/cleanup."""

        started = datetime.now(timezone.utc)
        errors = self.cleanup_runtime(runtime)
        ended = datetime.now(timezone.utc)
        execution_id = request.executionId or "cleanup-only"
        return ExecutionRunResponse(
            status="FAILED" if errors else "SUCCESS",
            executionId=execution_id,
            runtimeState=RuntimeState(
                executorId="python-mininet-ryu-executor",
                executorEndpoint=f"{self._config.executor_host}:{self._config.executor_port}",
                ryuControllerStatus="unchanged",
                mininetStatus="cleanup-failed" if errors else "cleaned",
                environmentStatus="CLEANUP_FAILED" if errors else "CLEANUP_COMPLETED",
                logsSummary="Controlled cleanup stopped executor-owned Mininet state.",
                startedAt=started,
                endedAt=ended,
            ),
            testResults=[],
            flowStats={},
            errors=errors,
            logsSummary="Cleanup completed with errors." if errors else "Cleanup completed.",
            startedAt=started,
            endedAt=ended,
        )
