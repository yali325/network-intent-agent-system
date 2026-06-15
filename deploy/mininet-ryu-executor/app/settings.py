"""Runtime settings for the MAC-TAV Mininet/Ryu executor."""

from __future__ import annotations

import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    """Executor configuration loaded from environment variables."""

    executor_host: str = os.getenv("EXECUTOR_HOST", "0.0.0.0")
    executor_port: int = int(os.getenv("EXECUTOR_PORT", "18091"))
    ryu_rest_url: str = os.getenv("RYU_REST_URL", "http://127.0.0.1:8080")
    ryu_auto_start: bool = os.getenv("RYU_AUTO_START", "false").lower() == "true"
    ryu_openflow_host: str = os.getenv("RYU_OPENFLOW_HOST", "127.0.0.1")
    ryu_openflow_port: int = int(os.getenv("RYU_OPENFLOW_PORT", "6633"))
    ryu_rest_timeout_seconds: float = float(os.getenv("RYU_REST_TIMEOUT_SECONDS", "3"))
    ryu_start_timeout_seconds: int = int(os.getenv("RYU_START_TIMEOUT_SECONDS", "15"))
    command_timeout_seconds: int = int(os.getenv("COMMAND_TIMEOUT_SECONDS", "20"))
    cleanup_timeout_seconds: int = int(os.getenv("CLEANUP_TIMEOUT_SECONDS", "20"))
    max_hosts: int = int(os.getenv("MAX_HOSTS", "0"))
    max_switches: int = int(os.getenv("MAX_SWITCHES", "0"))
    max_links: int = int(os.getenv("MAX_LINKS", "0"))
    max_actions: int = int(os.getenv("MAX_ACTIONS", "0"))
    max_tests: int = int(os.getenv("MAX_TESTS", "0"))
    default_timeout_seconds: int = int(os.getenv("DEFAULT_TIMEOUT_SECONDS", "120"))

    @property
    def expected_ryu_apps(self) -> list[str]:
        """Ryu apps fixed for the first executor version."""

        return ["simple_switch_13", "ofctl_rest"]


settings = Settings()
