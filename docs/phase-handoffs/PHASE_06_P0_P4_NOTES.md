# PHASE_06_P0_P4_NOTES

## 1. Scope

This note records Phase 6 P0-P4 preparation decisions only. It is a transition-preparation document, not final execution acceptance.

Phase 6 P0-P4 only covers Execution architecture boundary alignment and Maven dependency cleanup. It does not implement `ExecutionAdapter`, `ExecutionRequest`, `MininetRyuPythonClient`, Controller endpoints, Orchestrator integration, a Python executor, Verification, or Healing.

## 2. Confirmed Inputs

- P0 read scope: `AGENTS.md`, `docs/CODEX_CURRENT_STATE.md`, `docs/CODEX_DOC_INDEX.md`, and `docs/phase-handoffs/PHASE_05_HANDOFF.md`.
- P1 workspace check: `git status --short` and `git diff --name-only` were clean before edits.
- P2 minimum context: Phase 6 in `docs/06_DEV_PLAN.md`, Execution / Orchestrator / Web boundaries in `docs/03_MODULE_DESIGN.md`, Execution DTO and Workspace / Artifact sections in `docs/04_DATA_MODELS.md`, Execution API in `docs/05_API_DESIGN.md`, Mininet/Ryu run rules in `docs/08_RUN_AND_TEST.md`, and dependency boundaries in `docs/02_MAVEN_MODULES.md`.

## 3. Phase 6 P0-P4 Decisions

- Java side keeps `mac-tav-execution` centered on `ExecutionAdapter`.
- The Python Mininet/Ryu executor is a controlled internal executor, not a public business API and not an Agent service.
- The tentative Python executor port is `18091`.
- Ryu v1 uses `simple_switch_13` plus `ofctl_rest`; it does not add a custom Ryu app in this phase.
- Phase 6 does not implement Verification or Healing and does not decide whether the business intent has been achieved.
- Controller must not accept arbitrary shell.
- Java and Python exchange only structured request / response payloads; arbitrary shell text is not allowed as an execution contract.
- Real Mininet/Ryu integration tests are excluded from default CI. Default automated tests use structure validation and fixtures.

## 4. Dependency Boundary Result

`mac-tav-execution` currently has no production Java code using Qdrant, VectorStore, Spring AI, ReactAgent, or `mac-tav-agent-core`.

P0-P4 cleanup keeps `mac-tav-execution` dependent only on:

- `mac-tav-common`
- `mac-tav-model`

`mac-tav-orchestrator` is not forced to depend on `mac-tav-execution` before P20 / actual Execution orchestration integration. `mac-tav-web` must not directly depend on `mac-tav-execution`.

## 5. Next Starting Point

P5 should start from the Execution DTO and adapter contract design, then add the smallest compile-safe Java types needed for controlled `NetworkPlan + ConfigSet -> ExecutionReport` conversion. Do not start Python executor, Controller, Orchestrator wiring, Verification, or Healing in P5 unless the phase plan is explicitly updated.
