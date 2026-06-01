# Phase 6 Handoff: ExecutionAdapter + Mininet/Ryu

Last updated: Phase 6 P16 documentation closeout.

## 1. Status

Phase 6 is complete for the current milestone.

Completed scope:

- Execution DTOs, enums, workspace/artifact stage support.
- Java `ExecutionAdapter` boundary and registry.
- Java safety policy and structured action allow-list.
- `NetworkPlan + ConfigSet -> ExecutionPlan` converter.
- Default `StructureValidationExecutionAdapter`.
- Python Mininet/Ryu executor for Ubuntu 22.04 native deployment.
- Java `MininetRyuExecutorClient`.
- Java `MininetRyuExecutionAdapter`.
- Java `ExecutionService`.
- Orchestrator `runExecutionStage`.
- Web Execution API.
- Opt-in real Mininet/Ryu manual integration test.

Not implemented in Phase 6:

- VerificationAgent.
- HealingAgent.
- Intent satisfaction judgment.
- Public Python executor exposure.
- Arbitrary shell execution.
- Huawei CLI execution.
- Docker deployment path for the executor.

## 2. Key Java Files

Model DTOs and enums:

- `mac-tav-model/src/main/java/com/yali/mactav/model/execution/ExecutionReport.java`
- `mac-tav-model/src/main/java/com/yali/mactav/model/execution/ExecutionPlan.java`
- `mac-tav-model/src/main/java/com/yali/mactav/model/execution/ExecutionAction.java`
- `mac-tav-model/src/main/java/com/yali/mactav/model/execution/TestCommand.java`
- `mac-tav-model/src/main/java/com/yali/mactav/model/execution/RuntimeState.java`
- `mac-tav-model/src/main/java/com/yali/mactav/model/execution/TestResult.java`
- `mac-tav-model/src/main/java/com/yali/mactav/model/execution/ExecutionError.java`
- `mac-tav-model/src/main/java/com/yali/mactav/model/execution/*Status.java`
- `mac-tav-model/src/main/java/com/yali/mactav/model/execution/*Type.java`
- `mac-tav-model/src/main/java/com/yali/mactav/model/plan/TopologyNode.java`

Execution module:

- `mac-tav-execution/src/main/java/com/yali/mactav/execution/adapter/ExecutionAdapter.java`
- `mac-tav-execution/src/main/java/com/yali/mactav/execution/adapter/StructureValidationExecutionAdapter.java`
- `mac-tav-execution/src/main/java/com/yali/mactav/execution/adapter/MininetRyuExecutionAdapter.java`
- `mac-tav-execution/src/main/java/com/yali/mactav/execution/client/MininetRyuExecutorClient.java`
- `mac-tav-execution/src/main/java/com/yali/mactav/execution/client/dto/*`
- `mac-tav-execution/src/main/java/com/yali/mactav/execution/config/ExecutionProperties.java`
- `mac-tav-execution/src/main/java/com/yali/mactav/execution/converter/NetworkExecutionPlanConverter.java`
- `mac-tav-execution/src/main/java/com/yali/mactav/execution/model/ExecutionRequest.java`
- `mac-tav-execution/src/main/java/com/yali/mactav/execution/registry/ExecutionAdapterRegistry.java`
- `mac-tav-execution/src/main/java/com/yali/mactav/execution/registry/ExecutionAdapterRegistryFactory.java`
- `mac-tav-execution/src/main/java/com/yali/mactav/execution/safety/*`
- `mac-tav-execution/src/main/java/com/yali/mactav/execution/service/ExecutionService.java`
- `mac-tav-execution/src/main/java/com/yali/mactav/execution/service/DefaultExecutionService.java`

Orchestrator and Web:

- `mac-tav-orchestrator/src/main/java/com/yali/mactav/orchestrator/service/WorkflowOrchestrator.java`
- `mac-tav-orchestrator/src/main/java/com/yali/mactav/orchestrator/service/MacTavWorkflowOrchestrator.java`
- `mac-tav-orchestrator/src/main/java/com/yali/mactav/orchestrator/config/OrchestratorConfiguration.java`
- `mac-tav-web/src/main/java/com/yali/mactav/web/controller/ExecutionController.java`

Tests:

- `mac-tav-execution/src/test/java/com/yali/mactav/execution/adapter/MininetRyuExecutorManualIT.java`
- `mac-tav-execution/src/test/java/com/yali/mactav/execution/**`
- `mac-tav-orchestrator/src/test/java/com/yali/mactav/orchestrator/service/MacTavWorkflowOrchestratorTest.java`
- `mac-tav-web/src/test/java/com/yali/mactav/web/controller/WebControllerTest.java`

## 3. Python Executor

Location:

- `deploy/mininet-ryu-executor`

Deployment path:

- Ubuntu 22.04 native deployment.
- No Dockerfile or docker-compose is used for the validated path.
- Service port: `18091`.
- First Ryu app set: `ryu.app.simple_switch_13 + ryu.app.ofctl_rest`.

Python API:

- `GET /health`
- `GET /api/v1/ryu/status`
- `GET /api/v1/mininet/status`
- `POST /api/v1/executions/run`
- `POST /api/v1/executions/cleanup`

The executor accepts structured topology, actions, cleanup actions, and test
commands. It must not accept arbitrary shell, uploaded scripts, arbitrary Ryu
apps, Huawei CLI, or business-intent verification decisions.

## 4. Execution Paths

### Default CI Path

Default Java execution uses structure validation:

```properties
mactav.execution.mode=STRUCTURE_VALIDATION
mactav.execution.mininet-ryu.enabled=false
```

This path:

- Does not contact `127.0.0.1:18091`.
- Does not start Mininet or Ryu.
- Validates conversion, safety policy, and `ExecutionReport` structure.
- Is suitable for default `mvn test`.

### Real Mininet/Ryu Path

Manual real execution requires explicit opt-in:

```properties
mactav.execution.mode=MININET_RYU
mactav.execution.mininet-ryu.enabled=true
mactav.execution.mininet-ryu.base-url=http://127.0.0.1:18091
mactav.execution.mininet-ryu.connect-timeout-ms=3000
mactav.execution.mininet-ryu.read-timeout-ms=60000
```

The Java path is:

```text
ExecutionService
  -> ExecutionAdapterRegistry
  -> MininetRyuExecutionAdapter
  -> MininetRyuExecutorClient
  -> Python executor
  -> Ryu REST / Mininet
  -> ExecutionReport
```

## 5. Web Execution API

`mac-tav-web` exposes:

- `POST /api/v1/executions/{taskId}/run`
- `GET /api/v1/executions/{taskId}`

Controller boundary:

- Controllers call `WorkflowOrchestrator.runExecutionStage(taskId)` or query the current workspace report.
- Controllers do not depend directly on `mac-tav-execution`.
- Controllers do not accept shell, command, script, or raw execution text.
- Controllers do not call Python executor directly.

## 6. Real Validation Result

Validated via local tunnel/private local endpoint:

- Executor URL: `http://127.0.0.1:18091`
- `GET /health`: `status=ok`
- `GET /api/v1/ryu/status`: `status=available`
- `GET /api/v1/mininet/status`: `status=available`

Validated Java command:

```powershell
$env:MACTAV_RUN_MININET_RYU_IT="true"
mvn -pl mac-tav-execution -am -Dtest=MininetRyuExecutorManualIT "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result:

- `MininetRyuExecutorManualIT` passed.
- h1-s1-h2 smoke topology passed.
- `RYU_CONTROLLER_CHECK` passed.
- `TOPOLOGY_STATE_CHECK` passed.
- `PING_TEST` passed.
- Cleanup completed.
- Follow-up Mininet status reported `networkRunning=false`.

Do not commit real public IPs, usernames, passwords, SSH keys, API keys, or
tokens. The handoff intentionally records only local tunnel/private endpoint
shape.

## 7. Test Commands

Default compile:

```bash
mvn compile
```

Default execution tests:

```bash
mvn -pl mac-tav-execution -am test
```

Orchestrator tests:

```bash
mvn -pl mac-tav-orchestrator -am test
```

Web tests:

```bash
mvn -pl mac-tav-web -am test
```

Manual Mininet/Ryu integration:

```powershell
$env:MACTAV_RUN_MININET_RYU_IT="true"
mvn -pl mac-tav-execution -am -Dtest=MininetRyuExecutorManualIT "-Dsurefire.failIfNoSpecifiedTests=false" test
```

## 8. Safety Boundaries

Execution stage must obey:

- Do not execute LLM-generated arbitrary shell.
- Do not execute Huawei CLI.
- Do not accept raw shell, command, script, cmd, or rawCommand as the main contract.
- Do not let Web pass execution commands directly.
- Do not expose Python executor as a public business API.
- Do not let Execution decide whether the original intent is satisfied.
- Do not let Execution perform Verification or Healing.

Execution may report:

- Controlled execution plan.
- Runtime state.
- Test results.
- Flow/controller/topology facts.
- Structured errors.

Intent satisfaction belongs to Phase 7 VerificationAgent.

## 9. Phase 7 Handoff

Phase 7 should implement real `VerificationAgent`.

Expected inputs:

- `NetworkIntent`
- `NetworkPlan`
- `ConfigSet`
- `ExecutionReport`
- `NetworkWorkspace` context and trace refs

Expected output:

- `ValidationReport`

Responsibilities:

- Decide whether the original business intent is satisfied.
- Map execution facts to validation items.
- Produce evidences and suggestions.
- Keep each validation item traceable to intent / plan / config / test IDs.
- Distinguish passed, failed, partial, and unknown states.

Non-goals:

- Do not rerun Mininet/Ryu tests.
- Do not execute shell.
- Do not modify device configuration.
- Do not produce `RepairPlan`; that belongs to Phase 8 HealingAgent.
- Do not write `NetworkWorkspace` directly; Orchestrator / Model Core owns state writes.

Recommended first reads for Phase 7:

1. `AGENTS.md`
2. `docs/CODEX_CURRENT_STATE.md`
3. `docs/CODEX_DOC_INDEX.md`
4. `docs/phase-handoffs/PHASE_06_HANDOFF.md`
5. `docs/06_DEV_PLAN.md` Phase 7
6. `docs/03_MODULE_DESIGN.md` Verification / Orchestrator / Web sections
7. `docs/04_DATA_MODELS.md` `ValidationReport` and input DTO sections
8. `docs/07_TEST_DATA_AND_SCENARIOS.md` verification scenarios
9. `docs/08_RUN_AND_TEST.md`
10. `docs/09_AGENT_BUILD_GUIDE.md`
