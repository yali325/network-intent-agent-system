"""Structured input safety checks for the Mininet/Ryu executor skeleton."""

from __future__ import annotations

from collections.abc import Mapping, Sequence
from typing import Any

from .schemas import CleanupRequest, ExecutionRunRequest
from .settings import Settings, settings


ALLOWED_ACTION_TYPES = {
    "MININET_TOPOLOGY_START",
    "MININET_TOPOLOGY_STOP",
    "MININET_CLEANUP",
    "RYU_CONTROLLER_CHECK",
    "RYU_FLOW_QUERY",
    "PING_TEST",
    "TRACEROUTE_TEST",
    "IPERF_TEST",
    "TOPOLOGY_STATE_CHECK",
}

ALLOWED_TEST_TYPES = {
    "PING",
    "PING_TEST",
    "TRACEROUTE",
    "TRACEROUTE_TEST",
    "IPERF",
    "IPERF_TEST",
    "FLOW_TABLE",
    "RYU_FLOW_QUERY",
    "CONTROLLER_STATE",
    "RYU_CONTROLLER_CHECK",
    "TOPOLOGY_STATE",
    "TOPOLOGY_STATE_CHECK",
}

FORBIDDEN_FIELD_NAMES = {
    "command",
    "cmd",
    "shell",
    "script",
    "cli",
    "rawcommand",
}

FORBIDDEN_TEXT = (
    "rm -rf",
    "bash -c",
    "sh -c",
    "powershell",
    "cmd /c",
    "curl http",
    "wget http",
    "system-view",
    "display current-configuration",
    "huawei",
)


class SafetyViolation(ValueError):
    """Raised when a request contains unsafe or unsupported execution content."""


def validate_run_request(request: ExecutionRunRequest, config: Settings = settings) -> None:
    """Validate execution run payload size and content."""

    host_count = _count_nodes(request, "host")
    switch_count = _count_nodes(request, "switch")
    if host_count > config.max_hosts:
        raise SafetyViolation(f"host count {host_count} exceeds MAX_HOSTS={config.max_hosts}")
    if switch_count > config.max_switches:
        raise SafetyViolation(f"switch count {switch_count} exceeds MAX_SWITCHES={config.max_switches}")
    if len(request.topology.links) > config.max_links:
        raise SafetyViolation(f"link count {len(request.topology.links)} exceeds MAX_LINKS={config.max_links}")
    if len(request.actions) + len(request.cleanupActions) > config.max_actions:
        raise SafetyViolation("action count exceeds configured limit")
    if len(request.testCommands) > config.max_tests:
        raise SafetyViolation(f"testCommand count {len(request.testCommands)} exceeds MAX_TESTS={config.max_tests}")
    for action in [*request.actions, *request.cleanupActions]:
        if not action.actionId or not action.actionId.strip():
            raise SafetyViolation("actionId must not be blank")
        if action.actionType not in ALLOWED_ACTION_TYPES:
            raise SafetyViolation(f"actionType is not allow-listed: {action.actionType}")
        if action.traceRefs is None:
            raise SafetyViolation(f"traceRefs must not be empty for action {action.actionId}")
    for command in request.testCommands:
        if not command.testId or not command.testId.strip():
            raise SafetyViolation("testId must not be blank")
        if command.testType not in ALLOWED_TEST_TYPES:
            raise SafetyViolation(f"testType is not allow-listed: {command.testType}")
        if command.traceRefs is None:
            raise SafetyViolation(f"traceRefs must not be empty for test {command.testId}")
    _reject_dangerous_shape(request.model_dump())


def validate_cleanup_request(request: CleanupRequest, config: Settings = settings) -> None:
    """Validate cleanup payload size and content."""

    if len(request.cleanupActions) > config.max_actions:
        raise SafetyViolation("cleanup action count exceeds configured limit")
    for action in request.cleanupActions:
        if not action.actionId or not action.actionId.strip():
            raise SafetyViolation("actionId must not be blank")
        if action.actionType not in ALLOWED_ACTION_TYPES:
            raise SafetyViolation(f"actionType is not allow-listed: {action.actionType}")
    _reject_dangerous_shape(request.model_dump())


def _count_nodes(request: ExecutionRunRequest, token: str) -> int:
    count = 0
    for node in request.topology.nodes:
        values = [node.nodeType, node.deviceType, node.hostType, node.role]
        if any(value and token in value.lower() for value in values):
            count += 1
    return count


def _reject_dangerous_shape(value: Any, path: str = "$") -> None:
    if isinstance(value, Mapping):
        for key, item in value.items():
            normalized_key = str(key).replace("_", "").replace("-", "").lower()
            if normalized_key in FORBIDDEN_FIELD_NAMES:
                raise SafetyViolation(f"forbidden field name at {path}.{key}")
            _reject_dangerous_shape(item, f"{path}.{key}")
        return
    if isinstance(value, str):
        normalized_value = value.strip().lower()
        for token in FORBIDDEN_TEXT:
            if token in normalized_value:
                raise SafetyViolation(f"forbidden text token at {path}")
        return
    if isinstance(value, Sequence) and not isinstance(value, (bytes, bytearray, str)):
        for index, item in enumerate(value):
            _reject_dangerous_shape(item, f"{path}[{index}]")
