# CODEX_CURRENT_STATE

Last updated: Phase 7 P16 documentation closeout.

## 1. Current Project Phase

MAC-TAV has completed the main implementation work for Phase 1 through Phase 7
for the current milestone.

Phase 7 is functionally complete in code for the current offline acceptance
scope:

```text
NetworkIntent + NetworkPlan + ConfigSet + ExecutionReport
  -> VerificationAgent
  -> ValidationReport
  -> Orchestrator
  -> NetworkWorkspace / NetworkArtifact
```

Real A2A / Nacos / DashScope manual validation for VerificationAgent has not
been executed in Codex. It remains the main Phase 7 runtime TODO before treating
the service chain as manually accepted.

The next implementation phase is Phase 8: HealingAgent. Phase 8 must start from
the existing `ValidationReport`, workspace state, and failure evidence. It must
not let HealingAgent directly mutate workspace state or execute repairs.

## 2. Current Main Architecture

- `mac-tav-web` remains the Web/API entry module. Controllers call
  `WorkflowOrchestrator` or read workspace state; they do not call concrete
  agents, model APIs, execution clients, or shell commands.
- `mac-tav-orchestrator` remains the deterministic workflow coordinator. It
  calls professional agents through the remote Agent invocation boundary, then
  writes `NetworkWorkspace`, `NetworkArtifact`, `WorkspaceEvent`, and
  `AgentExecutionRecord` state through Model Core.
- Professional agents remain responsible only for their own stage DTOs. They do
  not mutate workspace state directly.
- `mac-tav-execution` is not an LLM Agent. It owns controlled Java execution
  boundaries, adapter selection, safety validation, conversion, client calls,
  and `ExecutionReport` generation.
- `mac-tav-verification-agent` is now a real Agent module for intent
  satisfaction verification. It returns `ValidationReport`; it does not modify
  configuration, rerun execution, or generate repair plans.

## 3. Phase 7 Completed Capabilities

### 3.1 Model / DTO Contract

- `ValidationReport` carries validation identity, task identity, execution
  identity, versions, overall status, summary, validation items, evidences,
  suggestions, trace refs, stage status, and timestamps.
- `VerificationAgentInvokePayload` carries the VerificationAgent input:
  `NetworkIntent`, `NetworkPlan`, `ConfigSet`, `ExecutionReport`, raw task text,
  versions, trace id, and workspace summary.
- `NetworkWorkspace` supports `currentValidationReport` and
  `currentValidationVersion`.
- `ArtifactType.VALIDATION_REPORT` and `WorkflowStage.VERIFICATION` are used by
  the orchestrated verification stage.

### 3.2 VerificationAgent Module

`mac-tav-verification-agent` now contains:

- `VerificationAgentApplication`
- `VerificationAgent`
- `VerificationAgentConfiguration`
- `VerificationAgentProperties`
- `VerificationAgentRequest`
- `VerificationResponseSchema`
- `VerificationResponseParser`
- `VerificationService` and `VerificationServiceImpl`
- `VerificationOutputValidator`
- `VerificationFactTool`
- `src/main/resources/prompts/verification-agent-prompt.md`
- `src/main/resources/application.yml`

The module input is:

```text
NetworkIntent + NetworkPlan + ConfigSet + ExecutionReport
```

The module output is:

```text
ValidationReport
```

The Agent follows the required boundary:

```text
ReactAgent -> VerificationResponseSchema -> Parser/Service -> ValidationReport -> Validator
```

### 3.3 Service Configuration Review

`mac-tav-verification-agent/src/main/resources/application.yml` has been
reviewed against the existing Agent modules.

- The service port, `mactav.agents.verification` prefix, prompt path, DashScope
  section, A2A/Nacos section, and Agent Card section follow the same general
  style as the existing Agent modules.
- The Agent Card name is `VerificationAgent`.
- Orchestrator discovers/invokes the same target name: `VerificationAgent`.
- The ReactAgent Bean is a single bean with two names/aliases:
  `VerificationAgent` and `verificationReactAgent`.
- This dual name shape is intended to satisfy both project target-name matching
  and the Spring AI Alibaba starter convention for lowercase ReactAgent bean
  names. It does not create two ReactAgent instances.
- No hand-written A2A HTTP Controller, Agent Card publisher, Nacos registration
  code, or new legacy HTTP fallback was added in the VerificationAgent module.
- No environment variable names were added or changed during this closeout pass.
- The VerificationAgent `application.yml` was reviewed but not modified during
  this closeout pass.

### 3.4 Orchestrator / Web Integration

- `WorkflowOrchestrator#runVerificationStage(taskId)` is available.
- `MacTavWorkflowOrchestrator#runVerificationStage` reads current
  `NetworkIntent`, `NetworkPlan`, `ConfigSet`, and `ExecutionReport`, invokes
  `VerificationAgent`, normalizes the returned `ValidationReport`, and writes:
  - `currentValidationReport`
  - `currentValidationVersion`
  - `VALIDATION_REPORT` artifact
  - verification-stage `AgentExecutionRecord`
- `mac-tav-web` exposes:
  - `POST /api/v1/validations/{taskId}/run`
  - `GET /api/v1/validations/{taskId}`
  - `GET /api/v1/validations/{taskId}/items`
- Web controllers do not depend on `mac-tav-verification-agent` directly.

## 4. Automated Test Result

The previous Phase 7 implementation pass reported these tests as passing:

```bash
mvn compile
mvn -pl mac-tav-verification-agent -am test
mvn -pl mac-tav-orchestrator -am test
mvn -pl mac-tav-web -am test
```

This closeout pass should at minimum rerun:

```bash
mvn compile
mvn -pl mac-tav-verification-agent -am test
```

Default automated tests must stay offline. They must not call real external
model APIs, real Nacos, real Qdrant, real Mininet/Ryu, or the real Python
executor.

## 5. VerificationAgent Manual Acceptance Checklist

Do not run long-lived services from Codex unless explicitly requested.

Manual acceptance steps:

1. Start Nacos.
2. Start `mac-tav-verification-agent`:

   ```bash
   mvn -pl mac-tav-verification-agent -am spring-boot:run
   ```

3. Check that the VerificationAgent Agent Card is discoverable.
4. Check that Nacos has registered `VerificationAgent`.
5. Start `mac-tav-web`:

   ```bash
   mvn -pl mac-tav-web -am spring-boot:run
   ```

6. Prepare a task that already has `NetworkIntent`, `NetworkPlan`, `ConfigSet`,
   and `ExecutionReport` in its workspace.
7. Trigger and read verification:

   ```text
   POST /api/v1/validations/{taskId}/run
   GET /api/v1/validations/{taskId}
   ```

8. Confirm the API returns a `ValidationReport`.
9. Confirm the workspace contains `currentValidationReport`.
10. Confirm a `VALIDATION_REPORT` artifact was written.
11. Confirm `AgentExecutionRecord` and `WorkspaceEvent` record the Verification
    stage.

Do not write real API key values into commands, source files, fixtures, logs, or
documentation.

## 6. Current TODO

- Manually validate real VerificationAgent A2A / Nacos / Agent Card discovery.
- Manually validate real DashScope-backed VerificationAgent invocation through
  `POST /api/v1/validations/{taskId}/run`.
- Confirm the returned `ValidationReport` is written to
  `currentValidationReport`.
- Confirm the `VALIDATION_REPORT` artifact, `AgentExecutionRecord`, and
  `WorkspaceEvent` are written for the Verification stage.
- Phase 8: implement HealingAgent only after the Phase 7 manual service chain is
  accepted or explicitly waived.
- Long-term: persistence, SSE/event history hardening, UI verification/healing
  views, and production deployment automation remain later work.

## 7. Phase 8 Starting Boundary

Phase 8 should begin with HealingAgent contracts and boundaries:

- Input: `ValidationReport`, `NetworkWorkspace`, failed validation items,
  evidences, suggestions, and trace refs.
- Output: `RepairPlan`.
- HealingAgent must not directly modify `NetworkWorkspace`.
- HealingAgent must not execute repair commands.
- HealingAgent must not bypass Orchestrator.
- Orchestrator remains responsible for writing artifacts, advancing state, and
  deciding whether to re-enter planning, configuration, execution, or
  verification.

## 8. Known Architecture Debt

- Legacy remote invocation support such as `RemoteAgentInvoker`,
  `HttpA2aClient`, or `NacosAgentCardRegistryClient` may still exist from
  earlier transition phases. This is an architecture debt and must not be copied
  as a template for new Agents.
- The VerificationAgent service configuration follows the official starter
  direction through `application.yml` plus a named `ReactAgent` Bean, and this
  closeout pass did not add legacy fallback code.
- If `docs/phase-handoffs/PHASE_06_P0_P4_NOTES.md` is absent from the worktree,
  that absence is treated as pre-existing worktree state for this handoff and
  must not be restored unless explicitly requested.
