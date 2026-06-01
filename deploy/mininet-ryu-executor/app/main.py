"""FastAPI entry point for the MAC-TAV Mininet/Ryu executor."""

from __future__ import annotations

import sys
from datetime import datetime, timezone

from fastapi import FastAPI

from .cleanup import CleanupRunner
from .errors import make_error
from .flow_client import FlowClient
from .mininet_runner import MininetRunner
from .runtime_state import ExecutorRuntimeState
from .ryu_manager import RyuManager
from .safety import SafetyViolation, validate_cleanup_request, validate_run_request
from .schemas import (
    CleanupRequest,
    ExecutionError,
    ExecutionRunRequest,
    ExecutionRunResponse,
    HealthResponse,
    StatusResponse,
)
from .settings import settings
from .test_runner import TestRunner

app = FastAPI(
    title="MAC-TAV Mininet/Ryu Executor",
    version="0.9.0",
    description="Phase 6 P9 controlled Mininet/Ryu executor for native Ubuntu 22.04 deployment.",
)

runtime_state = ExecutorRuntimeState()
ryu_manager = RyuManager()
mininet_runner = MininetRunner()
flow_client = FlowClient()
test_runner = TestRunner()
cleanup_runner = CleanupRunner(mininet_runner)


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    """Return executor, Ryu, and Mininet status summary."""

    ryu = ryu_manager.status()
    mininet = mininet_runner.status(runtime_state)
    return HealthResponse(
        status="ok" if ryu.status == "available" else "degraded",
        pythonVersion=sys.version.split()[0],
        configuredPort=settings.executor_port,
        ryuExpectedApps=settings.expected_ryu_apps,
        mininetInstalled=str(mininet.details.get("mininetInstalled", "unknown")),
    )


@app.get("/api/v1/ryu/status", response_model=StatusResponse)
def ryu_status() -> StatusResponse:
    """Return Ryu REST status metadata."""

    return ryu_manager.status()


@app.get("/api/v1/mininet/status", response_model=StatusResponse)
def mininet_status() -> StatusResponse:
    """Return Mininet availability and current network status."""

    return mininet_runner.status(runtime_state)


@app.post("/api/v1/executions/run", response_model=ExecutionRunResponse)
def run_execution(request: ExecutionRunRequest) -> ExecutionRunResponse:
    """Run a structured execution request against Mininet and Ryu."""

    started_at = datetime.now(timezone.utc)
    lease = runtime_state.try_acquire(request.executionId)
    if not lease.acquired:
        ended_at = datetime.now(timezone.utc)
        busy_error = make_error(
            "EXECUTOR_BUSY",
            "Another execution is already running in this executor process.",
            "EXECUTION_LOCK",
            recoverable=True,
            trace_refs=request.traceRefs,
        )
        return ExecutionRunResponse(
            status="FAILED",
            executionId=request.executionId,
            runtimeState=mininet_runner.prepare_runtime_state(
                request,
                started_at,
                ended_at,
                ryu_status="unknown",
                mininet_status="busy",
                environment_status="EXECUTOR_BUSY",
                logs_summary="Execution rejected because the in-process executor lock is held.",
            ),
            testResults=[],
            flowStats={},
            errors=[busy_error],
            logsSummary="Executor busy.",
            startedAt=started_at,
            endedAt=ended_at,
        )
    errors: list[ExecutionError] = []
    test_results = []
    flow_stats = {}
    net = None
    ryu_ready = False
    mininet_started = False
    try:
        try:
            validate_run_request(request)
        except SafetyViolation as exc:
            errors.append(
                make_error(
                    "EXECUTOR_FORBIDDEN_ACTION",
                    str(exc),
                    "REQUEST_SAFETY_VALIDATION",
                    recoverable=True,
                    trace_refs=request.traceRefs,
                )
            )
            cleanup_errors = cleanup_runner.cleanup_runtime(runtime_state)
            errors.extend(cleanup_errors)
            return _response(request, started_at, errors, test_results, flow_stats, "REQUEST_REJECTED", "not-started", "not-started")

        ryu_errors = ryu_manager.ensure_running(runtime_state)
        errors.extend(ryu_errors)
        ryu_ready = not ryu_errors
        if not ryu_ready:
            cleanup_errors = cleanup_runner.cleanup_runtime(runtime_state)
            errors.extend(cleanup_errors)
            return _response(request, started_at, errors, test_results, flow_stats, "RYU_UNAVAILABLE", "unavailable", "not-started")

        net, mininet_errors = mininet_runner.start_network(request, runtime_state)
        errors.extend(mininet_errors)
        mininet_started = net is not None and not mininet_errors
        if not mininet_started:
            cleanup_errors = cleanup_runner.cleanup_runtime(runtime_state)
            errors.extend(cleanup_errors)
            return _response(request, started_at, errors, test_results, flow_stats, "MININET_START_FAILED", "available", "failed")

        flow_stats, flow_errors = flow_client.flow_stats()
        errors.extend(flow_errors)
        test_results.extend(test_runner.run_action_checks(request, net, flow_stats, ryu_ready))
        test_results.extend(test_runner.run_tests(request, net, flow_stats, ryu_ready))
        cleanup_errors = cleanup_runner.cleanup_runtime(runtime_state)
        errors.extend(cleanup_errors)
        return _response(
            request,
            started_at,
            errors,
            test_results,
            flow_stats,
            "EXECUTION_COMPLETED_WITH_ERRORS" if errors else "EXECUTION_COMPLETED",
            "available" if ryu_ready else "unavailable",
            "running" if mininet_started else "failed",
        )
    except Exception as exc:  # noqa: BLE001 - never expose stack traces to callers.
        errors.append(
            make_error(
                "EXECUTOR_INVALID_REQUEST",
                f"Executor failed while processing request: {type(exc).__name__}",
                "EXECUTION_RUN",
                recoverable=True,
                trace_refs=request.traceRefs,
            )
        )
        cleanup_errors = cleanup_runner.cleanup_runtime(runtime_state)
        errors.extend(cleanup_errors)
        return _response(request, started_at, errors, test_results, flow_stats, "EXECUTION_FAILED", "unknown", "unknown")
    finally:
        runtime_state.release(lease)


@app.post("/api/v1/executions/cleanup", response_model=ExecutionRunResponse)
def cleanup_execution(request: CleanupRequest) -> ExecutionRunResponse:
    """Run controlled cleanup without accepting external commands."""

    try:
        validate_cleanup_request(request)
    except SafetyViolation as exc:
        now = datetime.now(timezone.utc)
        dummy_request = ExecutionRunRequest(
            executionId=request.executionId or "cleanup-only",
            taskId=request.taskId or "cleanup-only",
            planId="cleanup-only",
            configSetId="cleanup-only",
            executionVersion=0,
            topology={"nodes": [], "links": []},
            traceRefs=request.traceRefs,
        )
        return ExecutionRunResponse(
            status="FAILED",
            executionId=dummy_request.executionId,
            runtimeState=mininet_runner.prepare_runtime_state(
                dummy_request,
                now,
                now,
                ryu_status="unchanged",
                mininet_status="unchanged",
                environment_status="CLEANUP_REJECTED",
                logs_summary="Cleanup request rejected by safety validation.",
            ),
            testResults=[],
            flowStats={},
            errors=[
                make_error(
                    "EXECUTOR_FORBIDDEN_ACTION",
                    str(exc),
                    "CLEANUP_SAFETY_VALIDATION",
                    recoverable=True,
                    trace_refs=request.traceRefs,
                )
            ],
            logsSummary="Cleanup request rejected.",
            startedAt=now,
            endedAt=now,
        )
    return cleanup_runner.cleanup(request, runtime_state)


def _response(
    request: ExecutionRunRequest,
    started_at: datetime,
    errors: list[ExecutionError],
    test_results: list,
    flow_stats: dict,
    environment_status: str,
    ryu_status: str,
    mininet_status: str,
) -> ExecutionRunResponse:
    ended_at = datetime.now(timezone.utc)
    failed_tests = [result for result in test_results if result.status == "FAILED"]
    if errors and not test_results:
        status = "FAILED"
    elif errors or failed_tests:
        status = "PARTIAL"
    else:
        status = "SUCCESS"
    return ExecutionRunResponse(
        status=status,
        executionId=request.executionId,
        runtimeState=mininet_runner.prepare_runtime_state(
            request,
            started_at,
            ended_at,
            ryu_status=ryu_status,
            mininet_status=mininet_status,
            environment_status=environment_status,
            logs_summary="Controlled Mininet/Ryu execution path completed.",
        ),
        testResults=test_results,
        flowStats=flow_stats,
        errors=errors,
        logsSummary="Execution completed with structured errors." if errors else "Execution completed.",
        startedAt=started_at,
        endedAt=ended_at,
    )
