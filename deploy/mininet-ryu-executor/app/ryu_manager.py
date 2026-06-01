"""Controlled Ryu lifecycle and REST status checks."""

from __future__ import annotations

import subprocess
import time
from typing import Any

import httpx

from .errors import make_error
from .runtime_state import ExecutorRuntimeState
from .schemas import ExecutionError, StatusResponse
from .settings import Settings, settings


class RyuManager:
    """Checks Ryu ofctl_rest and starts only the fixed Ryu app set when enabled."""

    RYU_COMMAND = ["ryu-manager", "ryu.app.simple_switch_13", "ryu.app.ofctl_rest"]

    def __init__(self, config: Settings = settings) -> None:
        self._config = config

    def status(self) -> StatusResponse:
        """Return Ryu REST status based on /stats/switches."""

        data, error = self.query_switches()
        if error is not None:
            return StatusResponse(
                status="unavailable",
                details={
                    "ryuRestUrl": self._config.ryu_rest_url,
                    "expectedApps": self._config.expected_ryu_apps,
                    "errorCode": error.errorCode,
                    "message": error.message,
                    "autoStart": self._config.ryu_auto_start,
                },
            )
        return StatusResponse(
            status="available",
            details={
                "ryuRestUrl": self._config.ryu_rest_url,
                "expectedApps": self._config.expected_ryu_apps,
                "switches": data,
                "autoStart": self._config.ryu_auto_start,
            },
        )

    def query_switches(self) -> tuple[Any | None, ExecutionError | None]:
        """Query Ryu /stats/switches and convert failures to structured errors."""

        url = self._endpoint("/stats/switches")
        try:
            response = httpx.get(url, timeout=self._config.ryu_rest_timeout_seconds)
            response.raise_for_status()
            return response.json(), None
        except (httpx.HTTPError, ValueError) as exc:
            return None, make_error(
                "RYU_REST_UNAVAILABLE",
                f"Ryu REST API is unavailable at {self._config.ryu_rest_url}: {type(exc).__name__}",
                "RYU_STATUS_CHECK",
                recoverable=True,
            )

    def ensure_running(self, runtime: ExecutorRuntimeState) -> list[ExecutionError]:
        """Ensure Ryu REST is reachable, optionally starting a fixed Ryu process."""

        _, error = self.query_switches()
        if error is None:
            return []
        if not self._config.ryu_auto_start:
            return [error]
        process = runtime.get_owned_ryu_process()
        if process is None or process.poll() is not None:
            try:
                process = subprocess.Popen(  # noqa: S603 - fixed allow-listed command.
                    self.RYU_COMMAND,
                    stdin=subprocess.DEVNULL,
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.DEVNULL,
                    text=True,
                )
                runtime.set_owned_ryu_process(process)
            except OSError as exc:
                return [
                    make_error(
                        "RYU_START_FAILED",
                        f"Failed to start fixed Ryu app set: {type(exc).__name__}",
                        "RYU_START",
                        recoverable=True,
                    )
                ]
        deadline = time.monotonic() + self._config.ryu_start_timeout_seconds
        last_error = error
        while time.monotonic() < deadline:
            if process.poll() is not None:
                return [
                    make_error(
                        "RYU_START_FAILED",
                        "Ryu process exited before REST API became available.",
                        "RYU_START",
                        recoverable=True,
                    )
                ]
            _, last_error = self.query_switches()
            if last_error is None:
                return []
            time.sleep(0.5)
        return [
            make_error(
                "RYU_REST_UNAVAILABLE",
                last_error.message if last_error else "Ryu REST API did not become available in time.",
                "RYU_START_WAIT",
                recoverable=True,
            )
        ]

    def stop_owned(self, runtime: ExecutorRuntimeState) -> list[ExecutionError]:
        """Stop only the Ryu process started by this executor."""

        process = runtime.get_owned_ryu_process()
        if process is None:
            return []
        if process.poll() is not None:
            runtime.set_owned_ryu_process(None)
            return []
        process.terminate()
        try:
            process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            process.kill()
            try:
                process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                return [
                    make_error(
                        "RYU_START_FAILED",
                        "Executor-owned Ryu process did not stop after terminate/kill.",
                        "RYU_STOP",
                        recoverable=True,
                    )
                ]
        runtime.set_owned_ryu_process(None)
        return []

    def _endpoint(self, path: str) -> str:
        return self._config.ryu_rest_url.rstrip("/") + path
