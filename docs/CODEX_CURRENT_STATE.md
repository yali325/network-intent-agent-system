# CODEX_CURRENT_STATE

Last updated: Phase 6 P16 documentation closeout.

## 1. Current Project Phase

MAC-TAV has completed the main implementation work for Phase 1 through Phase 6.

Phase 6 is now considered functionally complete for the current milestone:

```text
NetworkPlan + ConfigSet
  -> NetworkExecutionPlanConverter
  -> ExecutionPlan
  -> ExecutionService
  -> ExecutionAdapter
  -> ExecutionReport
  -> NetworkWorkspace / NetworkArtifact
```

The real Mininet/Ryu path has also been manually validated through:

```text
MininetRyuExecutionAdapter
  -> MininetRyuExecutorClient
  -> Python executor on 127.0.0.1:18091
  -> Ryu REST + Mininet
```

The next main phase is Phase 7: implement the real `VerificationAgent`.

## 2. Current Main Architecture

- `mac-tav-web` remains the Web/API entry module. Controllers call `WorkflowOrchestrator` or read workspace state; they do not call concrete agents, model APIs, execution clients, or shell commands.
- `mac-tav-orchestrator` remains the deterministic workflow coordinator. It writes `NetworkWorkspace`, `NetworkArtifact`, `WorkspaceEvent`, and `AgentExecutionRecord` state through Model Core.
- Professional agents remain responsible only for their own stage DTOs. They do not mutate workspace state directly.
- `mac-tav-execution` is not an LLM Agent. It owns controlled Java execution boundaries, adapter selection, safety validation, conversion, client calls, and `ExecutionReport` generation.
- `deploy/mininet-ryu-executor` is an internal Python executor, not a public business API and not an Agent service.

## 3. Phase 6 Completed Capabilities

### 3.1 Model / DTO Contract

- `mac-tav-model` contains execution DTOs and enums for:
  - `ExecutionReport`
  - `ExecutionPlan`
  - `ExecutionAction`
  - `TestCommand`
  - `RuntimeState`
  - `TestResult`
  - `ExecutionError`
  - `ExecutionStatus`
  - `ExecutionMode`
  - `ExecutionEnvironmentType`
  - `ExecutionActionType`
  - `TestResultType`
  - `TestResultStatus`
- `NetworkWorkspace`, `NetworkArtifact`, `ArtifactType`, `WorkflowStage`, and `StageStatus` support the execution stage.
- `TopologyNode` includes structured runtime fields such as `ipAddress`, `ip`, `mac`, and `defaultRoute` for Mininet/Ryu topology exchange.

### 3.2 Java Execution Module

- `ExecutionAdapter` defines the Java-side controlled execution boundary.
- `ExecutionAdapterRegistry` and `ExecutionAdapterRegistryFactory` support adapter selection by `ExecutionEnvironmentType + ExecutionMode`.
- `ExecutionProperties` provides the configuration contract:
  - `mactav.execution.mode`
  - `mactav.execution.dry-run-enabled`
  - `mactav.execution.mininet-ryu.enabled`
  - `mactav.execution.mininet-ryu.base-url`
  - `mactav.execution.mininet-ryu.connect-timeout-ms`
  - `mactav.execution.mininet-ryu.read-timeout-ms`
- `ExecutionSafetyPolicy`, `ExecutionActionValidator`, `ExecutionCommandClassifier`, and `AllowedExecutionActionRegistry` enforce structured action allow-lists and reject shell-like semantics.
- `NetworkExecutionPlanConverter` converts `NetworkPlan + ConfigSet` into a controlled `ExecutionPlan`.
- `StructureValidationExecutionAdapter` remains the default CI-safe adapter.
- `MininetRyuExecutorClient` calls the Python executor API.
- `MininetRyuExecutionAdapter` maps executor responses to `ExecutionReport`.
- `ExecutionService` and `DefaultExecutionService` convert inputs, select an adapter, and return `ExecutionReport`.

### 3.3 Python Executor

- `deploy/mininet-ryu-executor` implements a FastAPI executor for native Ubuntu 22.04 deployment.
- The executor listens on port `18091`.
- Ryu first version is fixed to `ryu.app.simple_switch_13 + ryu.app.ofctl_rest`.
- The executor supports:
  - `GET /health`
  - `GET /api/v1/ryu/status`
  - `GET /api/v1/mininet/status`
  - `POST /api/v1/executions/run`
  - `POST /api/v1/executions/cleanup`
- It accepts structured topology/actions/tests only. It does not accept arbitrary shell, Huawei CLI, custom Ryu apps, or uploaded topology scripts.
- It executes one run at a time and returns structured errors such as `EXECUTOR_BUSY`, `RYU_REST_UNAVAILABLE`, `MININET_START_FAILED`, `TEST_FAILED`, and `FLOW_QUERY_FAILED`.

### 3.4 Orchestrator / Web Integration

- `WorkflowOrchestrator#runExecutionStage(taskId)` is available.
- `MacTavWorkflowOrchestrator#runExecutionStage` reads current `NetworkPlan` and `ConfigSet`, calls `ExecutionService`, and writes:
  - `currentExecutionReport`
  - `currentExecutionVersion`
  - `EXECUTION_REPORT` artifact
  - execution-stage `AgentExecutionRecord`
- `mac-tav-web` exposes:
  - `POST /api/v1/executions/{taskId}/run`
  - `GET /api/v1/executions/{taskId}`
- Web controllers do not depend on `mac-tav-execution` directly.

## 4. Real Mininet/Ryu Validation Result

Manual validation has succeeded through the real Java-to-Python execution chain:

```text
MininetRyuExecutionAdapter
  -> MininetRyuExecutorClient
  -> Python executor
  -> Ryu REST
  -> Mininet
```

Validated scenario:

- Minimal `h1-s1-h2` topology.
- `RYU_CONTROLLER_CHECK` passed.
- `TOPOLOGY_STATE_CHECK` passed.
- `PING_TEST` passed.
- Cleanup succeeded.
- Post-run Mininet status confirmed `networkRunning=false`.

Executor health during successful validation:

- `GET http://127.0.0.1:18091/health` returned `status=ok`.
- `GET /api/v1/ryu/status` returned `status=available`.
- `GET /api/v1/mininet/status` returned `status=available`.

## 5. Default Test Policy

Default automated tests must stay offline:

```bash
mvn compile
mvn -pl mac-tav-execution -am test
```

Default execution mode remains `STRUCTURE_VALIDATION`.

Real Mininet/Ryu validation is opt-in only:

```powershell
$env:MACTAV_RUN_MININET_RYU_IT="true"
mvn -pl mac-tav-execution -am -Dtest=MininetRyuExecutorManualIT "-Dsurefire.failIfNoSpecifiedTests=false" test
```

The real executor URL is configured for manual validation as:

```properties
mactav.execution.mode=MININET_RYU
mactav.execution.mininet-ryu.enabled=true
mactav.execution.mininet-ryu.base-url=http://127.0.0.1:18091
```

Do not enable the manual integration test in default CI.

## 6. Current TODO

- Phase 7: implement the real `VerificationAgent`.
- Define and validate `ValidationReport` production from `NetworkIntent + NetworkPlan + ConfigSet + ExecutionReport`.
- Add verification prompts, response schema, parser, validator, and service boundary.
- Connect `WorkflowOrchestrator#runVerificationStage(taskId)` after Phase 7 implementation.
- Keep Verification responsible for deciding whether the original intent is satisfied. Execution must only report execution facts.
- Long-term: persistence, SSE/event history hardening, UI execution/verification views, and production deployment automation remain later work.

## 7. Phase 7 Handoff Reading Scope

For a new Codex window starting Phase 7, read:

1. `AGENTS.md`
2. `docs/CODEX_CURRENT_STATE.md`
3. `docs/CODEX_DOC_INDEX.md`
4. `docs/phase-handoffs/PHASE_06_HANDOFF.md`
5. `docs/06_DEV_PLAN.md` Phase 7
6. `docs/03_MODULE_DESIGN.md` Verification / Orchestrator / Web sections
7. `docs/04_DATA_MODELS.md` `NetworkIntent`, `NetworkPlan`, `ConfigSet`, `ExecutionReport`, `ValidationReport`, `NetworkWorkspace`
8. `docs/07_TEST_DATA_AND_SCENARIOS.md` verification scenarios
9. `docs/08_RUN_AND_TEST.md` testing and manual validation rules
10. `docs/09_AGENT_BUILD_GUIDE.md` for real Agent implementation rules

## 8. Known Documentation Notes

- Older sections in broad docs may still mention Docker/WSL2 for Mininet/Ryu as historical or generic environment notes. The validated Phase 6 executor path is Ubuntu 22.04 native deployment plus local tunnel/private access to port `18091`.
- Do not commit real server IPs, usernames, passwords, SSH keys, API keys, or tokens.
