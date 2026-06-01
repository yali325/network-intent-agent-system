"""Process-local runtime state for one-at-a-time execution."""

from __future__ import annotations

import subprocess
import threading
from dataclasses import dataclass
from typing import Any


@dataclass
class ExecutionLease:
    """Represents ownership of the in-process execution lock."""

    execution_id: str
    acquired: bool


class ExecutorRuntimeState:
    """Tracks the current Mininet network and executor-owned Ryu process."""

    def __init__(self) -> None:
        self._lock = threading.Lock()
        self._state_guard = threading.Lock()
        self._current_execution_id: str | None = None
        self._network: Any | None = None
        self._owned_ryu_process: subprocess.Popen[str] | None = None

    def try_acquire(self, execution_id: str) -> ExecutionLease:
        """Try to acquire the single-execution lock without blocking."""

        acquired = self._lock.acquire(blocking=False)
        if not acquired:
            return ExecutionLease(execution_id=execution_id, acquired=False)
        with self._state_guard:
            self._current_execution_id = execution_id
        return ExecutionLease(execution_id=execution_id, acquired=True)

    def release(self, lease: ExecutionLease) -> None:
        """Release the execution lock if this lease owns it."""

        if not lease.acquired:
            return
        with self._state_guard:
            if self._current_execution_id == lease.execution_id:
                self._current_execution_id = None
        self._lock.release()

    @property
    def current_execution_id(self) -> str | None:
        """Return the execution currently holding the lock."""

        with self._state_guard:
            return self._current_execution_id

    def set_network(self, network: Any | None) -> None:
        """Store the current Mininet network object."""

        with self._state_guard:
            self._network = network

    def get_network(self) -> Any | None:
        """Return the current Mininet network object."""

        with self._state_guard:
            return self._network

    def set_owned_ryu_process(self, process: subprocess.Popen[str] | None) -> None:
        """Store the Ryu process started by this executor."""

        with self._state_guard:
            self._owned_ryu_process = process

    def get_owned_ryu_process(self) -> subprocess.Popen[str] | None:
        """Return the executor-owned Ryu process, if any."""

        with self._state_guard:
            return self._owned_ryu_process

    def clear_transient_state(self) -> None:
        """Clear per-execution state after cleanup."""

        with self._state_guard:
            self._network = None
