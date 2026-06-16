"""Unit tests for reachability result judgment."""

from __future__ import annotations

import unittest
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from app.test_runner import TestRunner


class TestReachabilityStatus(unittest.TestCase):
    """Verify PING status honors the expected reachability contract."""

    def setUp(self) -> None:
        self.runner = TestRunner()

    def test_reachable_expected_passes_when_ping_reaches_target(self) -> None:
        self.assertEqual(self.runner._reachability_status("reachable", True), "PASSED")

    def test_reachable_expected_fails_when_ping_cannot_reach_target(self) -> None:
        self.assertEqual(self.runner._reachability_status("reachable", False), "FAILED")

    def test_unreachable_expected_passes_when_ping_cannot_reach_target(self) -> None:
        self.assertEqual(self.runner._reachability_status("unreachable", False), "PASSED")

    def test_unreachable_expected_fails_when_ping_reaches_target(self) -> None:
        self.assertEqual(self.runner._reachability_status("unreachable", True), "FAILED")

    def test_ping_reachable_detects_zero_packet_loss(self) -> None:
        stdout = "3 packets transmitted, 3 received, 0% packet loss, time 2003ms"
        self.assertTrue(self.runner._ping_reachable(stdout))

    def test_ping_reachable_rejects_total_packet_loss(self) -> None:
        stdout = "3 packets transmitted, 0 received, 100% packet loss, time 2003ms"
        self.assertFalse(self.runner._ping_reachable(stdout))


if __name__ == "__main__":
    unittest.main()
