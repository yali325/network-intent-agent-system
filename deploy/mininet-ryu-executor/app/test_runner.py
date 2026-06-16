"""Controlled network test runner for structured test descriptors."""

from __future__ import annotations

import time
from datetime import datetime, timezone
from typing import Any

from .schemas import ExecutionAction, ExecutionRunRequest, TestCommand, TestResult
from .settings import Settings, settings


class TestRunner:
    """Executes allow-listed tests by generating bounded Mininet host commands."""

    def __init__(self, config: Settings = settings) -> None:
        self._config = config

    def run_action_checks(
        self,
        request: ExecutionRunRequest,
        net: Any,
        flow_stats: dict[str, Any],
        ryu_available: bool,
    ) -> list[TestResult]:
        """Represent state-check actions as execution facts without intent judgment."""

        results: list[TestResult] = []
        for action in request.actions:
            if action.actionType in {"RYU_CONTROLLER_CHECK", "RYU_FLOW_QUERY", "TOPOLOGY_STATE_CHECK"}:
                results.append(self._action_result(action, net, flow_stats, ryu_available))
        return results

    def run_tests(
        self,
        request: ExecutionRunRequest,
        net: Any,
        flow_stats: dict[str, Any],
        ryu_available: bool,
    ) -> list[TestResult]:
        """Run structured test commands against the current Mininet network."""

        results: list[TestResult] = []
        for command in request.testCommands:
            started = datetime.now(timezone.utc)
            monotonic_start = time.monotonic()
            test_type = self._normalize_type(command.testType)
            if test_type == "PING":
                result = self._run_ping(command, net, started, monotonic_start)
            elif test_type == "TRACEROUTE":
                result = self._run_traceroute(command, net, started, monotonic_start)
            elif test_type == "IPERF":
                result = self._run_iperf(command, net, started, monotonic_start)
            elif test_type == "FLOW_TABLE":
                result = self._state_result(command, started, monotonic_start, flow_stats, "flow table queried")
            elif test_type == "CONTROLLER_STATE":
                summary = "Ryu REST available" if ryu_available else "Ryu REST unavailable"
                result = self._state_result(command, started, monotonic_start, {"ryuAvailable": ryu_available}, summary)
            elif test_type == "TOPOLOGY_STATE":
                result = self._state_result(command, started, monotonic_start, self._topology_state(net), "topology state checked")
            else:
                result = self._failed_result(
                    command,
                    started,
                    monotonic_start,
                    "Unsupported allow-listed test type for this executor.",
                    "",
                    "TEST_FAILED",
                )
            results.append(result)
        return results

    def _run_ping(self, command: TestCommand, net: Any, started: datetime, monotonic_start: float) -> TestResult:
        source, target = self._require_hosts(command, net)
        if source is None or target is None:
            return self._failed_result(command, started, monotonic_start, "source or target host not found", "", "TEST_FAILED")
        count = self._bounded_int(command.parameters.get("count"), 1, 5, 3)
        timeout = min(self._config.command_timeout_seconds, count + 5)
        stdout = source.cmd(f"timeout {timeout} ping -c {count} {target.IP()}")
        reachable = self._ping_reachable(stdout)
        status = self._reachability_status(command.expectedResult, reachable)
        return self._command_result(command, started, monotonic_start, status, stdout, "")

    def _run_traceroute(self, command: TestCommand, net: Any, started: datetime, monotonic_start: float) -> TestResult:
        source, target = self._require_hosts(command, net)
        if source is None or target is None:
            return self._failed_result(command, started, monotonic_start, "source or target host not found", "", "TEST_FAILED")
        max_hops = self._bounded_int(command.parameters.get("maxHops"), 1, 16, 8)
        timeout = min(self._config.command_timeout_seconds, 10)
        stdout = source.cmd(f"timeout {timeout} traceroute -m {max_hops} {target.IP()}")
        status = "PASSED" if target.IP() in stdout or self._node_id(command, "target") in stdout else "FAILED"
        return self._command_result(command, started, monotonic_start, status, stdout, "")

    def _run_iperf(self, command: TestCommand, net: Any, started: datetime, monotonic_start: float) -> TestResult:
        source, target = self._require_hosts(command, net)
        if source is None or target is None:
            return self._failed_result(command, started, monotonic_start, "source or target host not found", "", "TEST_FAILED")
        duration = self._bounded_int(command.parameters.get("durationSeconds"), 1, 10, 3)
        server_port = self._bounded_int(command.parameters.get("port"), 1024, 65535, 5201)
        target.cmd(f"pkill -f 'iperf3 -s -1 -p {server_port}' || true")
        target.cmd(f"iperf3 -s -1 -p {server_port} >/tmp/mactav-iperf-{command.testId}.log 2>&1 &")
        time.sleep(0.5)
        stdout = source.cmd(f"timeout {duration + 5} iperf3 -c {target.IP()} -p {server_port} -t {duration}")
        target.cmd(f"pkill -f 'iperf3 -s -1 -p {server_port}' || true")
        status = "PASSED" if "receiver" in stdout.lower() else "FAILED"
        return self._command_result(command, started, monotonic_start, status, stdout, "")

    def _action_result(
        self,
        action: ExecutionAction,
        net: Any,
        flow_stats: dict[str, Any],
        ryu_available: bool,
    ) -> TestResult:
        started = datetime.now(timezone.utc)
        monotonic_start = time.monotonic()
        if action.actionType == "RYU_CONTROLLER_CHECK":
            metrics = {"ryuAvailable": ryu_available}
            summary = "Ryu controller REST checked."
            status = "PASSED" if ryu_available else "FAILED"
        elif action.actionType == "RYU_FLOW_QUERY":
            metrics = {"flowStatsStatus": flow_stats.get("status")}
            summary = "Ryu flow table queried."
            status = "PASSED" if flow_stats.get("status") == "collected" else "FAILED"
        else:
            metrics = self._topology_state(net)
            summary = "Mininet topology state checked."
            status = "PASSED" if metrics.get("networkRunning") else "FAILED"
        ended = datetime.now(timezone.utc)
        return TestResult(
            testId=action.actionId,
            testType=action.actionType,
            status=status,
            sourceNodeId=action.targetNodeId,
            targetNodeId=action.targetNodeId,
            resultSummary=summary,
            stdoutSummary=summary,
            metrics=metrics,
            startedAt=started,
            endedAt=ended,
            durationMs=self._duration_ms(monotonic_start),
            traceRefs=action.traceRefs,
        )

    def _state_result(
        self,
        command: TestCommand,
        started: datetime,
        monotonic_start: float,
        metrics: dict[str, Any],
        summary: str,
    ) -> TestResult:
        ended = datetime.now(timezone.utc)
        return TestResult(
            testId=command.testId,
            testType=command.testType,
            status="PASSED",
            sourceNodeId=self._node_id(command, "source"),
            targetNodeId=self._node_id(command, "target"),
            sourceNode=command.sourceNode,
            targetNode=command.targetNode,
            expectedResult=command.expectedResult,
            actualResult=summary,
            resultSummary=summary,
            stdoutSummary=summary,
            metrics=metrics,
            startedAt=started,
            endedAt=ended,
            durationMs=self._duration_ms(monotonic_start),
            traceRefs=command.traceRefs,
        )

    def _command_result(
        self,
        command: TestCommand,
        started: datetime,
        monotonic_start: float,
        status: str,
        stdout: str,
        stderr: str,
    ) -> TestResult:
        ended = datetime.now(timezone.utc)
        return TestResult(
            testId=command.testId,
            testType=command.testType,
            status=status,  # type: ignore[arg-type]
            sourceNodeId=self._node_id(command, "source"),
            targetNodeId=self._node_id(command, "target"),
            sourceNode=command.sourceNode,
            targetNode=command.targetNode,
            expectedResult=command.expectedResult,
            actualResult=self._summarize(stdout),
            resultSummary=self._summarize(stdout),
            stdoutSummary=self._summarize(stdout),
            stderrSummary=self._summarize(stderr),
            startedAt=started,
            endedAt=ended,
            durationMs=self._duration_ms(monotonic_start),
            traceRefs=command.traceRefs,
        )

    def _failed_result(
        self,
        command: TestCommand,
        started: datetime,
        monotonic_start: float,
        stdout: str,
        stderr: str,
        summary: str,
    ) -> TestResult:
        return self._command_result(command, started, monotonic_start, "FAILED", stdout or summary, stderr)

    def _require_hosts(self, command: TestCommand, net: Any) -> tuple[Any | None, Any | None]:
        try:
            runtime_names = getattr(net, "mactav_node_runtime_names", {})
            source_id = self._node_id(command, "source")
            target_id = self._node_id(command, "target")
            source = net.get(runtime_names.get(source_id, source_id))
            target = net.get(runtime_names.get(target_id, target_id))
            return source, target
        except Exception:  # noqa: BLE001
            return None, None

    def _topology_state(self, net: Any) -> dict[str, Any]:
        return {
            "networkRunning": net is not None,
            "hostCount": len(getattr(net, "hosts", [])) if net is not None else 0,
            "switchCount": len(getattr(net, "switches", [])) if net is not None else 0,
            "linkCount": len(getattr(net, "links", [])) if net is not None else 0,
        }

    def _normalize_type(self, value: str) -> str:
        mapping = {
            "PING_TEST": "PING",
            "TRACEROUTE_TEST": "TRACEROUTE",
            "IPERF_TEST": "IPERF",
            "RYU_FLOW_QUERY": "FLOW_TABLE",
            "RYU_CONTROLLER_CHECK": "CONTROLLER_STATE",
            "TOPOLOGY_STATE_CHECK": "TOPOLOGY_STATE",
        }
        return mapping.get(value, value)

    def _ping_reachable(self, stdout: str) -> bool:
        """Return whether ping output proves source can reach target."""

        normalized = stdout.lower()
        return " 0% packet loss" in normalized or ", 0% packet loss" in normalized

    def _reachability_status(self, expected_result: str | None, reachable: bool) -> str:
        """Judge reachability tests against the expected result contract."""

        expected = (expected_result or "reachable").strip().lower()
        if expected in {"unreachable", "blocked", "deny", "denied", "isolated", "not_reachable"}:
            return "PASSED" if not reachable else "FAILED"
        if expected in {"reachable", "allowed", "allow", "permit", "permitted"}:
            return "PASSED" if reachable else "FAILED"
        return "PASSED" if reachable else "FAILED"

    def _node_id(self, command: TestCommand, side: str) -> str:
        if side == "source":
            return command.sourceNodeId or command.sourceNode or ""
        return command.targetNodeId or command.targetNode or ""

    def _bounded_int(self, raw: Any, minimum: int, maximum: int, default: int) -> int:
        try:
            value = int(raw)
        except (TypeError, ValueError):
            value = default
        return max(minimum, min(maximum, value))

    def _summarize(self, value: str | None) -> str:
        if not value:
            return ""
        single_line = " ".join(value.split())
        return single_line[:500]

    def _duration_ms(self, monotonic_start: float) -> int:
        return int((time.monotonic() - monotonic_start) * 1000)
