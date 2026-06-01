"""Pydantic schemas for structured Mininet/Ryu executor APIs."""

from __future__ import annotations

from datetime import datetime, timezone
from typing import Any, Literal

from pydantic import BaseModel, Field


class StrictBaseModel(BaseModel):
    """Base schema that rejects unexpected top-level fields."""

    class Config:
        extra = "forbid"


class TraceRefs(StrictBaseModel):
    """Cross-stage trace identifiers carried from Java ExecutionPlan."""

    intentNodeIds: list[str] = Field(default_factory=list)
    intentRelationIds: list[str] = Field(default_factory=list)
    planElementIds: list[str] = Field(default_factory=list)
    configBlockIds: list[str] = Field(default_factory=list)
    testIds: list[str] = Field(default_factory=list)
    validationItemIds: list[str] = Field(default_factory=list)
    repairActionIds: list[str] = Field(default_factory=list)


class TopologyNode(StrictBaseModel):
    """Structured topology node descriptor."""

    id: str
    name: str | None = None
    nodeType: str | None = None
    deviceType: str | None = None
    hostType: str | None = None
    role: str | None = None
    ipAddress: str | None = None
    ip: str | None = None
    mac: str | None = None
    defaultRoute: str | None = None
    vendor: str | None = None
    zoneId: str | None = None
    traceRefs: TraceRefs | None = None


class TopologyLink(StrictBaseModel):
    """Structured topology link descriptor."""

    id: str
    sourceNode: str
    sourceInterface: str | None = None
    targetNode: str
    targetInterface: str | None = None
    linkType: str | None = None
    traceRefs: TraceRefs | None = None


class Topology(StrictBaseModel):
    """Structured topology payload."""

    nodes: list[TopologyNode] = Field(default_factory=list)
    links: list[TopologyLink] = Field(default_factory=list)


class ExecutionAction(StrictBaseModel):
    """Structured allow-listed execution action."""

    actionId: str
    sequence: int | None = None
    actionType: str
    targetNodeId: str | None = None
    targetDeviceId: str | None = None
    parameters: dict[str, Any] = Field(default_factory=dict)
    traceRefs: TraceRefs | None = None


class TestCommand(StrictBaseModel):
    """Structured execution test descriptor."""

    testId: str
    testType: str
    sourceNodeId: str | None = None
    targetNodeId: str | None = None
    sourceNode: str | None = None
    targetNode: str | None = None
    parameters: dict[str, Any] = Field(default_factory=dict)
    expectedResult: str | None = None
    traceRefs: TraceRefs | None = None


class ExecutionRunRequest(StrictBaseModel):
    """Request body for POST /api/v1/executions/run."""

    executionId: str
    taskId: str
    planId: str
    configSetId: str
    executionVersion: int
    topology: Topology
    actions: list[ExecutionAction] = Field(default_factory=list)
    cleanupActions: list[ExecutionAction] = Field(default_factory=list)
    testCommands: list[TestCommand] = Field(default_factory=list)
    timeoutSeconds: int | None = None
    traceRefs: TraceRefs | None = None


class CleanupRequest(StrictBaseModel):
    """Request body for POST /api/v1/executions/cleanup."""

    executionId: str | None = None
    taskId: str | None = None
    cleanupActions: list[ExecutionAction] = Field(default_factory=list)
    traceRefs: TraceRefs | None = None


class RuntimeState(StrictBaseModel):
    """Executor runtime state returned to Java clients."""

    executorId: str
    executorEndpoint: str
    ryuControllerStatus: str
    mininetStatus: str
    environmentStatus: str
    logsSummary: str
    startedAt: datetime | None = None
    endedAt: datetime | None = None


class TestResult(StrictBaseModel):
    """Controlled network test result produced by the executor."""

    testId: str
    testType: str
    status: Literal["PASSED", "FAILED", "PARTIAL", "SKIPPED", "UNKNOWN"]
    sourceNodeId: str | None = None
    targetNodeId: str | None = None
    sourceNode: str | None = None
    targetNode: str | None = None
    expectedResult: str | None = None
    actualResult: str | None = None
    resultSummary: str | None = None
    stdoutSummary: str | None = None
    stderrSummary: str | None = None
    metrics: dict[str, Any] = Field(default_factory=dict)
    logsSummary: str | None = None
    evidenceRefs: dict[str, str] = Field(default_factory=dict)
    startedAt: datetime | None = None
    endedAt: datetime | None = None
    durationMs: int | None = None
    traceRefs: TraceRefs | None = None


class ExecutionError(StrictBaseModel):
    """Structured executor error."""

    errorCode: str
    message: str
    stage: str | None = None
    actionId: str | None = None
    testId: str | None = None
    recoverable: bool = True
    traceRefs: TraceRefs | None = None


class ExecutionRunResponse(StrictBaseModel):
    """Response body for POST /api/v1/executions/run."""

    status: Literal["SUCCESS", "PARTIAL", "FAILED", "SKIPPED", "UNKNOWN"]
    executionId: str
    runtimeState: RuntimeState
    testResults: list[TestResult] = Field(default_factory=list)
    flowStats: dict[str, Any] = Field(default_factory=dict)
    errors: list[ExecutionError] = Field(default_factory=list)
    logsSummary: str
    startedAt: datetime
    endedAt: datetime


class StatusResponse(StrictBaseModel):
    """Simple component status response."""

    status: str
    details: dict[str, Any] = Field(default_factory=dict)


class HealthResponse(StrictBaseModel):
    """Executor health response."""

    status: str
    pythonVersion: str
    configuredPort: int
    ryuExpectedApps: list[str]
    mininetInstalled: str
    timestamp: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))
