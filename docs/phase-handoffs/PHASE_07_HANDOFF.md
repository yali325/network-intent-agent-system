# Phase 7 Handoff

## 1. Scope

Phase 7 adds the real VerificationAgent stage for MAC-TAV.

The stage responsibility is:

```text
NetworkIntent + NetworkPlan + ConfigSet + ExecutionReport
  -> VerificationAgent
  -> ValidationReport
```

VerificationAgent judges whether execution facts satisfy the original business
intent. It must not modify configuration, execute repairs, rerun execution, or
produce `RepairPlan`.

## 2. Completed Capabilities

### 2.1 VerificationAgent

Implemented in `mac-tav-verification-agent`:

- `VerificationAgentApplication`
- `VerificationAgent`
- `VerificationAgentConfiguration`
- `VerificationAgentProperties`
- `VerificationAgentRequest`
- `VerificationResponseSchema`
- `VerificationResponseParser`
- `VerificationService`
- `VerificationServiceImpl`
- `VerificationOutputValidator`
- `VerificationFactTool`
- `prompts/verification-agent-prompt.md`
- `application.yml`

The production Agent path is:

```text
ReactAgent
  -> VerificationResponseSchema
  -> VerificationResponseParser / VerificationService
  -> ValidationReport
  -> VerificationOutputValidator
```

The Agent module does not write `NetworkWorkspace` and does not advance task
state.

### 2.2 Model Contract

Relevant model-side contracts now include:

- `ValidationReport`
- `VerificationAgentInvokePayload`
- `NetworkWorkspace.currentValidationReport`
- `NetworkWorkspace.currentValidationVersion`
- `ArtifactType.VALIDATION_REPORT`
- `WorkflowStage.VERIFICATION`

`ValidationReport` is the Phase 7 output and the Phase 8 input anchor.

### 2.3 Orchestrator / Web

`WorkflowOrchestrator#runVerificationStage(taskId)` is wired.

`MacTavWorkflowOrchestrator#runVerificationStage`:

- Reads `NetworkIntent`, `NetworkPlan`, `ConfigSet`, and `ExecutionReport` from
  the workspace.
- Builds `VerificationAgentInvokePayload`.
- Invokes target Agent name `VerificationAgent`.
- Parses the returned payload as `ValidationReport`.
- Normalizes task id, execution id, versions, stage status, and trace refs.
- Writes `currentValidationReport`.
- Writes a `VALIDATION_REPORT` artifact.
- Appends Verification-stage `AgentExecutionRecord`.

`mac-tav-web` exposes:

```text
POST /api/v1/validations/{taskId}/run
GET /api/v1/validations/{taskId}
GET /api/v1/validations/{taskId}/items
```

The Web layer only calls Orchestrator or reads workspace state. It does not call
`VerificationAgent`, `ChatModel`, or `ReactAgent` directly.

## 3. Service Configuration Review

Reviewed:

- Existing Agent module `application.yml` files.
- `mac-tav-verification-agent/src/main/resources/application.yml`.
- `VerificationAgentConfiguration`.
- Orchestrator target Agent name.
- VerificationAgent module source tree for hand-written Controller, Agent Card,
  Nacos, or legacy fallback code.

Conclusion:

- VerificationAgent configuration follows the existing Agent-module style:
  service port, `mactav.agents.verification` prefix, prompt path, DashScope
  config, A2A/Nacos config, and Agent Card config.
- Agent Card name is `VerificationAgent`.
- Orchestrator invokes target name `VerificationAgent`.
- `VerificationAgentConfiguration` registers one `ReactAgent` bean with two
  names/aliases: `VerificationAgent` and `verificationReactAgent`.
- The dual name shape should not create duplicate ReactAgent instances or
  duplicate Agent Cards by itself.
- No hand-written A2A HTTP Controller, Agent Card publisher, Nacos registration
  code, or new legacy HTTP fallback was found in the VerificationAgent module.
- No environment variable names were added or changed in this handoff pass.
- `application.yml` was reviewed and not modified in this handoff pass.

## 4. Automated Tests

The Phase 7 implementation pass reported these commands as passing:

```bash
mvn compile
mvn -pl mac-tav-verification-agent -am test
mvn -pl mac-tav-orchestrator -am test
mvn -pl mac-tav-web -am test
```

This handoff pass should rerun the minimum closeout commands:

```bash
mvn compile
mvn -pl mac-tav-verification-agent -am test
```

Automated tests are offline and must not call real external model APIs, real
Nacos, real Qdrant, real Mininet/Ryu, or the real Python executor.

## 5. Manual Acceptance Checklist

Real A2A / Nacos / DashScope validation has not been executed in Codex for this
handoff.

Run these steps manually when ready:

1. Start Nacos.
2. Start `mac-tav-verification-agent`:

   ```bash
   mvn -pl mac-tav-verification-agent -am spring-boot:run
   ```

3. Check the VerificationAgent Agent Card is discoverable.
4. Check Nacos has registered `VerificationAgent`.
5. Start `mac-tav-web`:

   ```bash
   mvn -pl mac-tav-web -am spring-boot:run
   ```

6. Prepare a task that already has these workspace artifacts:
   `NetworkIntent`, `NetworkPlan`, `ConfigSet`, and `ExecutionReport`.
7. Trigger verification:

   ```text
   POST /api/v1/validations/{taskId}/run
   ```

8. Read verification:

   ```text
   GET /api/v1/validations/{taskId}
   ```

9. Confirm the response is a `ValidationReport`.
10. Confirm `NetworkWorkspace.currentValidationReport` is populated.
11. Confirm a `VALIDATION_REPORT` artifact exists.
12. Confirm `AgentExecutionRecord` records the Verification stage.
13. Confirm `WorkspaceEvent` records the Verification stage.

Do not put real API key values, Nacos credentials, SSH keys, or tokens in
commands, source files, fixtures, logs, or documentation.

## 6. Current TODO

- Complete real Nacos registration and Agent Card discovery validation for
  VerificationAgent.
- Complete real DashScope-backed VerificationAgent invocation validation through
  the Web API.
- Confirm workspace, artifact, execution record, and workspace event writes in
  the running service chain.
- Keep legacy remote invocation code marked as transition architecture debt.
- Do not enter HealingAgent implementation until Phase 8 starts explicitly.

## 7. Phase 8 Handoff Boundary

Recommended Phase 8 starting point:

1. Read `AGENTS.md`.
2. Read `docs/CODEX_CURRENT_STATE.md`.
3. Read `docs/CODEX_DOC_INDEX.md`.
4. Read this handoff.
5. Read `docs/06_DEV_PLAN.md` Phase 8.
6. Read `docs/04_DATA_MODELS.md` sections for `ValidationReport`,
   `RepairPlan`, and workspace trace refs.
7. Read `docs/09_AGENT_BUILD_GUIDE.md` for the real Agent structure.

Phase 8 input boundary:

```text
ValidationReport + NetworkWorkspace + failed validation items + evidences + suggestions + trace refs
```

Phase 8 output boundary:

```text
RepairPlan
```

Phase 8 prohibitions:

- Do not let HealingAgent directly modify `NetworkWorkspace`.
- Do not let HealingAgent execute repair commands.
- Do not bypass Orchestrator.
- Do not add Repair API before the Healing stage contract and orchestrator
  boundary are stable.
- Do not make VerificationAgent generate `RepairPlan` or `RepairAction`.

## 8. Architecture Debt

Legacy remote invocation support such as `RemoteAgentInvoker`, `HttpA2aClient`,
or `NacosAgentCardRegistryClient` may still exist from earlier transition
phases. Treat it as transition architecture debt. Do not copy it into new Agent
modules and do not add a second HTTP JSON fallback path for VerificationAgent or
HealingAgent.

If `docs/phase-handoffs/PHASE_06_P0_P4_NOTES.md` is absent, keep that as
pre-existing worktree state unless the user explicitly asks to restore it.
