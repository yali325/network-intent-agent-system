"""Ryu ofctl_rest flow query client."""

from __future__ import annotations

from typing import Any

import httpx

from .errors import make_error
from .schemas import ExecutionError
from .settings import Settings, settings


class FlowClient:
    """Queries Ryu flow, port, and switch state through fixed REST endpoints."""

    def __init__(self, config: Settings = settings) -> None:
        self._config = config

    def flow_stats(self) -> tuple[dict[str, Any], list[ExecutionError]]:
        """Collect Ryu switch and flow-table stats without mutating controller state."""

        switches, error = self._get("/stats/switches")
        if error is not None:
            return {"status": "failed"}, [error]
        result: dict[str, Any] = {"status": "collected", "switches": switches, "flows": {}}
        errors: list[ExecutionError] = []
        for dpid in switches if isinstance(switches, list) else []:
            flows, flow_error = self._get(f"/stats/flow/{dpid}")
            if flow_error is None:
                result["flows"][str(dpid)] = flows
            else:
                errors.append(flow_error)
        return result, errors

    def _get(self, path: str) -> tuple[Any | None, ExecutionError | None]:
        url = self._config.ryu_rest_url.rstrip("/") + path
        try:
            response = httpx.get(url, timeout=self._config.ryu_rest_timeout_seconds)
            response.raise_for_status()
            return response.json(), None
        except (httpx.HTTPError, ValueError) as exc:
            return None, make_error(
                "FLOW_QUERY_FAILED",
                f"Ryu flow query failed for {path}: {type(exc).__name__}",
                "RYU_FLOW_QUERY",
                recoverable=True,
            )
