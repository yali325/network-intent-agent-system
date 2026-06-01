"""Structured executor error helpers."""

from __future__ import annotations

from .schemas import ExecutionError, TraceRefs


class ExecutorOperationError(RuntimeError):
    """Internal exception that carries a structured executor error."""

    def __init__(
        self,
        error_code: str,
        message: str,
        stage: str,
        action_id: str | None = None,
        test_id: str | None = None,
        recoverable: bool = True,
        trace_refs: TraceRefs | None = None,
    ) -> None:
        super().__init__(message)
        self.error = make_error(
            error_code,
            message,
            stage,
            action_id=action_id,
            test_id=test_id,
            recoverable=recoverable,
            trace_refs=trace_refs,
        )


def make_error(
    error_code: str,
    message: str,
    stage: str,
    action_id: str | None = None,
    test_id: str | None = None,
    recoverable: bool = True,
    trace_refs: TraceRefs | None = None,
) -> ExecutionError:
    """Create the response-level structured error shape."""

    return ExecutionError(
        errorCode=error_code,
        message=message,
        stage=stage,
        actionId=action_id,
        testId=test_id,
        recoverable=recoverable,
        traceRefs=trace_refs,
    )
