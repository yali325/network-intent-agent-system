"""Controlled Mininet topology runner."""

from __future__ import annotations

import importlib
import subprocess
import time
from datetime import datetime, timezone
from typing import Any

from .errors import make_error
from .runtime_state import ExecutorRuntimeState
from .schemas import ExecutionError, ExecutionRunRequest, RuntimeState, StatusResponse, TopologyNode
from .settings import Settings, settings


class MininetRunner:
    """Builds a bounded Mininet topology from structured node and link data."""

    def __init__(self, config: Settings = settings) -> None:
        self._config = config

    def status(self, runtime: ExecutorRuntimeState | None = None) -> StatusResponse:
        """Return Mininet import and current network status."""

        installed = self._mininet_importable()
        running = runtime is not None and runtime.get_network() is not None
        return StatusResponse(
            status="running" if running else ("available" if installed else "unavailable"),
            details={
                "mininetInstalled": installed,
                "networkRunning": running,
                "maxHosts": self._config.max_hosts,
                "maxSwitches": self._config.max_switches,
                "maxLinks": self._config.max_links,
            },
        )

    def start_network(
        self,
        request: ExecutionRunRequest,
        runtime: ExecutorRuntimeState,
    ) -> tuple[Any | None, list[ExecutionError]]:
        """Create and start a Mininet network using only structured topology."""

        errors = self._validate_topology(request)
        if errors:
            return None, errors
        try:
            mininet_net = importlib.import_module("mininet.net")
            mininet_node = importlib.import_module("mininet.node")
            mininet_link = importlib.import_module("mininet.link")
        except ImportError as exc:
            return None, [
                make_error(
                    "MININET_IMPORT_FAILED",
                    f"Mininet Python package is not importable: {type(exc).__name__}",
                    "MININET_IMPORT",
                    recoverable=True,
                    trace_refs=request.traceRefs,
                )
            ]
        try:
            net = mininet_net.Mininet(
                controller=None,
                switch=mininet_node.OVSSwitch,
                link=mininet_link.TCLink,
                autoSetMacs=True,
            )
            net.addController(
                "c0",
                controller=mininet_node.RemoteController,
                ip=self._config.ryu_openflow_host,
                port=self._config.ryu_openflow_port,
            )
            nodes: dict[str, Any] = {}
            for node in request.topology.nodes:
                if self._is_host(node):
                    nodes[node.id] = net.addHost(
                        self._node_name(node),
                        ip=node.ipAddress or node.ip,
                        mac=node.mac,
                        defaultRoute=node.defaultRoute,
                    )
                elif self._is_switch(node):
                    nodes[node.id] = net.addSwitch(self._node_name(node))
            for link in request.topology.links:
                left = nodes.get(link.sourceNode)
                right = nodes.get(link.targetNode)
                if left is None or right is None:
                    return None, [
                        make_error(
                            "EXECUTOR_INVALID_REQUEST",
                            f"Topology link {link.id} references an unknown node.",
                            "MININET_TOPOLOGY_BUILD",
                            recoverable=True,
                            trace_refs=link.traceRefs or request.traceRefs,
                        )
                    ]
                net.addLink(left, right)
            net.start()
            runtime.set_network(net)
            self._wait_for_switches(net)
            return net, []
        except Exception as exc:  # noqa: BLE001 - convert runtime failures to structured errors.
            return None, [
                make_error(
                    "MININET_START_FAILED",
                    f"Mininet failed to start structured topology: {type(exc).__name__}",
                    "MININET_START",
                    recoverable=True,
                    trace_refs=request.traceRefs,
                )
            ]

    def stop_network(self, runtime: ExecutorRuntimeState) -> list[ExecutionError]:
        """Stop the current Mininet network object, if one exists."""

        net = runtime.get_network()
        if net is None:
            return []
        try:
            net.stop()
            return []
        except Exception as exc:  # noqa: BLE001
            return [
                make_error(
                    "MININET_CLEANUP_FAILED",
                    f"Failed to stop current Mininet network: {type(exc).__name__}",
                    "MININET_CLEANUP",
                    recoverable=True,
                )
            ]
        finally:
            runtime.set_network(None)

    def run_fixed_cleanup(self) -> list[ExecutionError]:
        """Run the fixed Mininet cleanup command `mn -c`."""

        try:
            completed = subprocess.run(  # noqa: S603,S607 - fixed allow-listed command.
                ["mn", "-c"],
                capture_output=True,
                text=True,
                timeout=self._config.cleanup_timeout_seconds,
                check=False,
            )
        except (OSError, subprocess.TimeoutExpired) as exc:
            return [
                make_error(
                    "MININET_CLEANUP_FAILED",
                    f"Fixed Mininet cleanup failed: {type(exc).__name__}",
                    "MININET_CLEANUP",
                    recoverable=True,
                )
            ]
        if completed.returncode != 0:
            return [
                make_error(
                    "MININET_CLEANUP_FAILED",
                    "Fixed Mininet cleanup exited with a non-zero status.",
                    "MININET_CLEANUP",
                    recoverable=True,
                )
            ]
        return []

    def prepare_runtime_state(
        self,
        request: ExecutionRunRequest,
        started_at: datetime,
        ended_at: datetime | None = None,
        ryu_status: str = "unknown",
        mininet_status: str = "unknown",
        environment_status: str = "UNKNOWN",
        logs_summary: str | None = None,
    ) -> RuntimeState:
        """Create response runtime state."""

        return RuntimeState(
            executorId="python-mininet-ryu-executor",
            executorEndpoint=f"{self._config.executor_host}:{self._config.executor_port}",
            ryuControllerStatus=ryu_status,
            mininetStatus=mininet_status,
            environmentStatus=environment_status,
            logsSummary=logs_summary or f"Execution {request.executionId} processed by Python executor.",
            startedAt=started_at,
            endedAt=ended_at or datetime.now(timezone.utc),
        )

    def _validate_topology(self, request: ExecutionRunRequest) -> list[ExecutionError]:
        host_count = sum(1 for node in request.topology.nodes if self._is_host(node))
        switch_count = sum(1 for node in request.topology.nodes if self._is_switch(node))
        if host_count > self._config.max_hosts:
            return [
                make_error(
                    "EXECUTOR_INVALID_REQUEST",
                    f"Topology host count {host_count} exceeds limit {self._config.max_hosts}.",
                    "MININET_TOPOLOGY_VALIDATE",
                    recoverable=True,
                    trace_refs=request.traceRefs,
                )
            ]
        if switch_count > self._config.max_switches:
            return [
                make_error(
                    "EXECUTOR_INVALID_REQUEST",
                    f"Topology switch count {switch_count} exceeds limit {self._config.max_switches}.",
                    "MININET_TOPOLOGY_VALIDATE",
                    recoverable=True,
                    trace_refs=request.traceRefs,
                )
            ]
        if len(request.topology.links) > self._config.max_links:
            return [
                make_error(
                    "EXECUTOR_INVALID_REQUEST",
                    f"Topology link count {len(request.topology.links)} exceeds limit {self._config.max_links}.",
                    "MININET_TOPOLOGY_VALIDATE",
                    recoverable=True,
                    trace_refs=request.traceRefs,
                )
            ]
        if host_count == 0 or switch_count == 0:
            return [
                make_error(
                    "EXECUTOR_INVALID_REQUEST",
                    "Topology must include at least one host and one switch.",
                    "MININET_TOPOLOGY_VALIDATE",
                    recoverable=True,
                    trace_refs=request.traceRefs,
                )
            ]
        return []

    def _wait_for_switches(self, net: Any) -> None:
        deadline = time.monotonic() + 5
        while time.monotonic() < deadline:
            switches = getattr(net, "switches", [])
            if not switches:
                return
            if all(bool(getattr(switch, "connected", lambda: True)()) for switch in switches):
                return
            time.sleep(0.25)

    def _mininet_importable(self) -> bool:
        return importlib.util.find_spec("mininet.net") is not None

    def _is_host(self, node: TopologyNode) -> bool:
        return self._has_type(node, "host")

    def _is_switch(self, node: TopologyNode) -> bool:
        return self._has_type(node, "switch")

    def _has_type(self, node: TopologyNode, token: str) -> bool:
        values = [node.nodeType, node.deviceType, node.hostType, node.role]
        return any(value is not None and token in value.lower() for value in values)

    def _node_name(self, node: TopologyNode) -> str:
        return node.name or node.id
