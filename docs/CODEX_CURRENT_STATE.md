# CODEX_CURRENT_STATE

Last updated: Phase 8 P15-P18 closeout.

## 1. Current Project Phase

MAC-TAV has completed the current offline implementation scope for Phase 1
through Phase 8.

Phase 8 is functionally complete in code for the current offline acceptance
scope:

```text
ValidationReport + NetworkWorkspace failure context
  -> HealingAgent
  -> RepairPlan
  -> Orchestrator
  -> NetworkWorkspace / REPAIR_PLAN artifact / Repair API
```

Real A2A / Nacos / DashScope manual validation for HealingAgent has not been
executed in Codex. It remains the main runtime TODO before treating the Phase 8
service chain as manually accepted.

## 2. Current Main Architecture

- `mac-tav-web` remains the Web/API entry module. Controllers call
  `WorkflowOrchestrator` or read workspace state; they do not call concrete
  agents, model APIs, execution clients, or shell commands.
- `mac-tav-orchestrator` remains the deterministic workflow coordinator. It
  calls professional agents through the remote Agent invocation boundary, then
  writes `NetworkWorkspace`, `NetworkArtifact`, `WorkspaceEvent`, and
  `AgentExecutionRecord` state through Model Core.
- Professional agents remain responsible only for their own stage DTOs. They do
  not mutate workspace state directly and do not advance task state.
- `mac-tav-execution` is not an LLM Agent. It owns controlled Java execution
  boundaries, adapter selection, safety validation, conversion, client calls,
  and `ExecutionReport` generation.
- `mac-tav-healing-agent` is now a real Agent module for diagnosis and repair
  plan proposal. It returns `RepairPlan`; it does not apply repairs.

## 3. Phase 8 Completed Capabilities

### 3.1 Model / DTO Contract

- `HealingAgentInvokePayload` carries the HealingAgent input: task identity,
  failed `ValidationReport`, `NetworkWorkspace` snapshot, failed validation
  items, evidences, suggestions, trace refs, and workspace summary.
- `RepairPlan` is the current Phase 8 output DTO. The current code uses
  `taskId`, `validationVersion`, `repairVersion`, `overallRepairStrategy`,
  `failureAnalysis`, `actions`, `requiresUserConfirmation`, `stageStatus`, and
  `createTime`.
- `RepairAction` carries action identity/type, target stage, reason,
  description, proposed change summary, risk, approval fields, status, and
  trace refs.
- `FailureAnalysis` carries structured failure category, severity, affected
  scope, evidence references, trace refs, and recommendation text.
- `NetworkWorkspace` supports `currentRepairPlan` and `currentRepairVersion`.
- `ArtifactType.REPAIR_PLAN` and `WorkflowStage.HEALING` are used by the
  orchestrated healing stage.

### 3.2 HealingAgent Module

`mac-tav-healing-agent` now contains a real Agent structure:

- `HealingAgentApplication`
- `HealingAgent`
- `HealingAgentConfiguration`
- `HealingAgentProperties`
- `HealingAgentRequest`
- `HealingResponseSchema`
- `HealingResponseParser`
- `HealingService` and `HealingServiceImpl`
- `HealingOutputValidator`
- `HealingDiagnosisTool`
- `src/main/resources/prompts/healing-agent-prompt.md`
- `src/main/resources/application.yml`

The module input is:

```text
ValidationReport + NetworkWorkspace snapshot + failed items + evidences + suggestions + trace refs
```

The module output is:

```text
RepairPlan
```

The Agent follows the required boundary:

```text
ReactAgent -> HealingResponseSchema -> Parser/Service -> RepairPlan -> Validator
```

`HealingDiagnosisTool` provides structured classification, affected scope /
trace extraction, and repair action suggestion helpers. It does not write
workspace state, call Orchestrator, execute commands, or call Mininet/Ryu.

### 3.3 Service Configuration Review

`mac-tav-healing-agent/src/main/resources/application.yml` was reviewed against
the existing Agent modules during the Phase 8 closeout.

- The default service port is `18085`, which does not conflict with intent
  `18081`, planning `18082`, configuration `18083`, or verification `18084`.
- The configuration prefix is `mactav.agents.healing`.
- The prompt path is `prompts/healing-agent-prompt.md`.
- DashScope API key configuration uses environment variables only:
  `SPRING_AI_DASHSCOPE_API_KEY`, `ALI_API_KEY`, or `DASHSCOPE_API_KEY`.
- A2A / Nacos / Agent Card configuration follows the project starter-based
  direction through `application.yml` plus named `ReactAgent` Bean registration.
- The Agent Card name is `HealingAgent`.
- Orchestrator invokes the same target name: `HealingAgent`.
- The ReactAgent Bean is named with two aliases: `HealingAgent` and
  `healingReactAgent`.
- No hand-written A2A HTTP Controller, Agent Card publisher, Nacos registration
  code, or new legacy HTTP fallback was added.

The HealingAgent `application.yml` was reviewed but not modified during this
closeout pass.

### 3.4 Orchestrator / Model Core / Web Integration

- `WorkflowOrchestrator#runHealingStage(taskId)` is available.
- `MacTavWorkflowOrchestrator#runHealingStage` reads current workspace state,
  requires a failed / partial / unknown `ValidationReport`, invokes
  `HealingAgent`, normalizes the returned `RepairPlan`, and writes:
  - `currentRepairPlan`
  - `currentRepairVersion`
  - `REPAIR_PLAN` artifact
  - healing-stage `AgentExecutionRecord`
  - repair-related `WorkspaceEvent`
- `NetworkWorkspaceService` supports saving current repair plans and appending
  repair events.
- Repair approve / reject / apply operations are available through
  `WorkflowOrchestrator`.
- Approve / reject only update action approval/status metadata and record
  change/event history. They do not trigger execution.
- Apply dispatches only controlled stage re-entry:
  - planning for `REPLAN` or `targetStage=PLANNING`
  - configuration for `REGENERATE_CONFIG`, `PATCH_CONFIG`, or
    `targetStage=CONFIGURATION`
  - execution for `REEXECUTE` or `targetStage=EXECUTION`
  - verification for `targetStage=VERIFICATION`
  - `ASK_USER` sets the task to `WAITING_USER`
  - `ROLLBACK` is rejected until controlled version switching is implemented
- High-risk or approval-required actions must be approved before apply.
- Selected repair guidance is passed into planning/configuration re-entry as
  minimal traceable user-context guidance.
- `mac-tav-web` exposes:
  - `POST /api/v1/repairs/{taskId}/analyze`
  - `GET /api/v1/repairs/{taskId}`
  - `POST /api/v1/repairs/{taskId}/actions/{actionId}/approve`
  - `POST /api/v1/repairs/{taskId}/actions/{actionId}/reject`
  - `POST /api/v1/repairs/{taskId}/actions/{actionId}/apply`
- Web controllers do not depend on `mac-tav-healing-agent` directly.

### 3.5 Phase 8 Scenario Fixtures

Phase 8 regression fixtures were added under `docs/test-data/scenarios`:

- `guest-to-server-unexpected-pass`
- `routing-missing-failure`
- `acl-direction-error`

These fixtures are for Parser / Validator / Orchestrator regression coverage.
They are not substitutes for the real Agent path.

## 4. Automated Test Result

The Phase 8 closeout pass ran these commands successfully:

```bash
mvn -pl mac-tav-healing-agent -am test   # BUILD SUCCESS, 11 healing-agent tests
mvn -pl mac-tav-orchestrator -am test    # BUILD SUCCESS, 29 orchestrator tests
mvn -pl mac-tav-web -am test             # BUILD SUCCESS, 8 web tests
mvn compile                              # BUILD SUCCESS, all 13 modules
```

Default automated tests must stay offline. They must not call real external
model APIs, real Nacos, real Qdrant, real Mininet/Ryu, or the real Python
executor.

## 5. HealingAgent Manual Acceptance Checklist

Do not run long-lived services from Codex unless explicitly requested.

Manual acceptance steps:

1. Start Nacos.
2. Export one model API key through environment variables, for example
   `SPRING_AI_DASHSCOPE_API_KEY`.
3. Start `mac-tav-healing-agent`:

   ```bash
   mvn -pl mac-tav-healing-agent -am spring-boot:run
   ```

4. Confirm the HealingAgent Agent Card is discoverable.
5. Confirm Nacos has registered `HealingAgent`.
6. Start the orchestrator/web entry service:

   ```bash
   mvn -pl mac-tav-web -am spring-boot:run
   ```

7. Prepare a task that already has a failed, partial, or unknown
   `ValidationReport`, a `VALIDATION_REPORT` artifact, and available upstream
   workspace state.
8. Trigger and read repair analysis:

   ```text
   POST /api/v1/repairs/{taskId}/analyze
   GET /api/v1/repairs/{taskId}
   ```

9. Confirm the API returns a `RepairPlan`.
10. Confirm the workspace contains `currentRepairPlan`.
11. Confirm a `REPAIR_PLAN` artifact was written.
12. Confirm `AgentExecutionRecord`, `WorkspaceEvent`, and
    `WorkspaceChangeRecord` record the healing/repair activity.
13. Approve a high-risk action before apply:

    ```text
    POST /api/v1/repairs/{taskId}/actions/{actionId}/approve
    POST /api/v1/repairs/{taskId}/actions/{actionId}/apply
    ```

14. Confirm apply re-enters only the controlled Orchestrator stage and does not
    execute arbitrary shell or HealingAgent-generated commands.

Do not write real API key values into commands, source files, fixtures, logs, or
documentation.

## 6. Current TODO

- Manually validate real HealingAgent A2A / Nacos / Agent Card discovery.
- Manually validate real DashScope-backed HealingAgent invocation through
  `POST /api/v1/repairs/{taskId}/analyze`.
- Confirm the returned `RepairPlan` is written to `currentRepairPlan`.
- Confirm the `REPAIR_PLAN` artifact, `AgentExecutionRecord`,
  `WorkspaceEvent`, and `WorkspaceChangeRecord` are written for healing and
  repair operations.
- Implement controlled rollback/version switching before enabling
  `RepairActionType.ROLLBACK` apply behavior.
- Harden durable persistence and event/SSE history beyond the in-memory model
  core path.
- Build UI views for validation/healing/repair if needed in a later phase.

## 7. Known Architecture Debt / Doc Mismatch

- Legacy remote invocation support such as `RemoteAgentInvoker`,
  `HttpA2aClient`, or `NacosAgentCardRegistryClient` still exists from earlier
  transition phases. This is an architecture debt and must not be copied as a
  template for new Agents.
- HealingAgent service configuration follows the official starter direction
  through `application.yml` plus a named `ReactAgent` Bean. This closeout pass
  did not add legacy fallback code.
- `docs/04_DATA_MODELS.md` still describes older `RepairPlan` field names such
  as `repairId`, `validationId`, singular `failureAnalysis`, `repairActions`,
  and `status`. Current code uses `taskId`, `validationVersion`,
  `repairVersion`, list-based `failureAnalysis`, `actions`, and `stageStatus`.
  Phase 8 proceeded with the current code contract and records this as
  documentation debt instead of rewriting the old model document broadly.
