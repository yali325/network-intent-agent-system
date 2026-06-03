# Phase 8 Handoff - HealingAgent and Repair Loop

## Scope

Phase 8 implemented the first complete offline healing and repair planning
loop:

```text
ValidationReport + NetworkWorkspace failure context
  -> HealingAgent
  -> RepairPlan
  -> Orchestrator
  -> REPAIR_PLAN artifact / currentRepairPlan
  -> Repair API approve / reject / apply
```

The scope intentionally excludes direct repair execution by HealingAgent,
arbitrary shell execution, Web UI pages, SSE, durable persistence hardening, and
real A2A / Nacos / DashScope manual validation.

## Completed Capabilities

- HealingAgent module scaffolded and closed into a real
  `ResponseSchema -> Parser/Service -> RepairPlan -> Validator` chain.
- HealingDiagnosisTool provides structured diagnosis helpers without writing
  workspace state or executing commands.
- Orchestrator can run the healing stage after failed / partial / unknown
  validation.
- Model Core can save `REPAIR_PLAN` artifacts and synchronize
  `currentRepairPlan` / `currentRepairVersion`.
- Repair approve, reject, and controlled apply operations are available through
  `WorkflowOrchestrator`.
- Web Repair API exposes analyze, get, approve, reject, and apply endpoints.
- Phase 8 scenario fixtures were added for healing parser / validator /
  service regression.

## Model / DTO Contract

Current code is the Phase 8 authority for repair DTO fields:

- `HealingAgentInvokePayload` is the HealingAgent input contract.
- `RepairPlan` uses `taskId`, `validationVersion`, `repairVersion`,
  `overallRepairStrategy`, `failureAnalysis`, `actions`,
  `requiresUserConfirmation`, `stageStatus`, and `createTime`.
- `RepairAction` carries action metadata, target stage, risk, approval fields,
  status, natural-language guidance, and trace refs.
- `FailureAnalysis` carries category, severity, scope, evidence references,
  trace refs, and recommendation details.
- `NetworkWorkspace` carries `currentRepairPlan` and
  `currentRepairVersion`.
- `ArtifactType.REPAIR_PLAN` and `WorkflowStage.HEALING` are now part of the
  closed loop.

DTOs do not depend on Spring AI Alibaba types.

## HealingAgent Module

Main files:

- `mac-tav-healing-agent/src/main/java/com/yali/mactav/healing/HealingAgentApplication.java`
- `mac-tav-healing-agent/src/main/java/com/yali/mactav/healing/agent/HealingAgent.java`
- `mac-tav-healing-agent/src/main/java/com/yali/mactav/healing/config/HealingAgentConfiguration.java`
- `mac-tav-healing-agent/src/main/java/com/yali/mactav/healing/config/HealingAgentProperties.java`
- `mac-tav-healing-agent/src/main/java/com/yali/mactav/healing/request/HealingAgentRequest.java`
- `mac-tav-healing-agent/src/main/java/com/yali/mactav/healing/schema/HealingResponseSchema.java`
- `mac-tav-healing-agent/src/main/java/com/yali/mactav/healing/parser/HealingResponseParser.java`
- `mac-tav-healing-agent/src/main/java/com/yali/mactav/healing/service/HealingService.java`
- `mac-tav-healing-agent/src/main/java/com/yali/mactav/healing/service/HealingServiceImpl.java`
- `mac-tav-healing-agent/src/main/java/com/yali/mactav/healing/validator/HealingOutputValidator.java`
- `mac-tav-healing-agent/src/main/java/com/yali/mactav/healing/tool/HealingDiagnosisTool.java`
- `mac-tav-healing-agent/src/main/resources/prompts/healing-agent-prompt.md`
- `mac-tav-healing-agent/src/main/resources/application.yml`

The prompt forbids claims that workspace was modified, commands were executed,
or configuration was applied. The validator checks the same unsafe natural
language surfaces.

## A2A / Nacos / Application Configuration

`mac-tav-healing-agent/src/main/resources/application.yml` was reviewed during
P15:

- default port: `18085`
- config prefix: `mactav.agents.healing`
- prompt path: `prompts/healing-agent-prompt.md`
- model API key: environment variables only
- Agent Card name: `HealingAgent`
- Orchestrator target agent: `HealingAgent`
- ReactAgent Bean aliases: `HealingAgent` and `healingReactAgent`
- Nacos/A2A configuration follows the same starter-based style as the current
  VerificationAgent module

No hand-written A2A Controller, Agent Card publisher, Nacos registration code,
or new legacy HTTP fallback was added.

## Orchestrator Integration

`WorkflowOrchestrator#runHealingStage(taskId)` and
`MacTavWorkflowOrchestrator#runHealingStage` are implemented.

The healing stage:

- reads current workspace state;
- requires `currentValidationReport`;
- refuses `PASSED` validation reports;
- only enters healing for `FAILED`, `PARTIAL`, or `UNKNOWN`;
- requires a `VALIDATION_REPORT` artifact;
- constructs `HealingAgentInvokePayload`;
- invokes target agent `HealingAgent` through the existing remote invocation
  boundary;
- parses the remote payload as `RepairPlan`;
- normalizes task id, validation version, repair version, stage status,
  confirmation flag, action status, and trace refs;
- saves a `REPAIR_PLAN` artifact;
- updates `currentRepairPlan` and `currentRepairVersion`;
- appends healing `AgentExecutionRecord`, workspace events, and task status.

Healing does not execute repair actions.

## Web Repair API

`mac-tav-web` exposes:

- `POST /api/v1/repairs/{taskId}/analyze`
- `GET /api/v1/repairs/{taskId}`
- `POST /api/v1/repairs/{taskId}/actions/{actionId}/approve`
- `POST /api/v1/repairs/{taskId}/actions/{actionId}/reject`
- `POST /api/v1/repairs/{taskId}/actions/{actionId}/apply`

The controller delegates to `WorkflowOrchestrator` or workspace reads only. It
does not depend on `mac-tav-healing-agent`, `ChatModel`, `ReactAgent`, execution
adapters, or shell commands.

## RepairAction Approve / Reject / Apply Behavior

Approve / reject:

- update only the selected action's approval/status metadata;
- record actor, comment, and timestamp where supported by the current DTO;
- save the updated current repair plan;
- append change and event history;
- do not trigger stage execution.

Apply:

- reads `currentRepairPlan`;
- validates the action exists;
- requires approval for high-risk or approval-required actions;
- rejects arbitrary shell or direct command execution;
- dispatches only controlled Orchestrator re-entry:
  - `REPLAN` or `targetStage=PLANNING` -> planning
  - `REGENERATE_CONFIG`, `PATCH_CONFIG`, or `targetStage=CONFIGURATION` ->
    configuration
  - `REEXECUTE` or `targetStage=EXECUTION` -> execution
  - `targetStage=VERIFICATION` -> verification
  - `ASK_USER` -> `WAITING_USER`
  - `ROLLBACK` -> explicit `BusinessException` until controlled version
    switching exists

Selected repair guidance is passed into planning/configuration re-entry as a
minimal traceable user-context hint.

## Tests Run

Phase 8 closeout commands passed:

```bash
mvn -pl mac-tav-healing-agent -am test   # BUILD SUCCESS, 11 healing-agent tests
mvn -pl mac-tav-orchestrator -am test    # BUILD SUCCESS, 29 orchestrator tests
mvn -pl mac-tav-web -am test             # BUILD SUCCESS, 8 web tests
mvn compile                              # BUILD SUCCESS, all 13 modules
```

Automated tests are offline and must not call real external model APIs, real
Nacos, real Qdrant, real Mininet/Ryu, or real Python execution.

## Manual Acceptance Checklist

1. Start Nacos outside Codex.
2. Export `SPRING_AI_DASHSCOPE_API_KEY` or another supported local environment
   variable.
3. Start `mac-tav-healing-agent`.
4. Confirm Nacos registration and Agent Card discovery for `HealingAgent`.
5. Start `mac-tav-web`.
6. Prepare a workspace with a failed / partial / unknown `ValidationReport`,
   `VALIDATION_REPORT` artifact, and upstream artifacts where available.
7. Call `POST /api/v1/repairs/{taskId}/analyze`.
8. Call `GET /api/v1/repairs/{taskId}`.
9. Confirm `RepairPlan`, `currentRepairPlan`, `REPAIR_PLAN` artifact,
   `AgentExecutionRecord`, `WorkspaceEvent`, and `WorkspaceChangeRecord`.
10. Approve and apply a high-risk action, then confirm only controlled
    Orchestrator re-entry happens.
11. Confirm no HealingAgent output is executed as a shell command or applied
    directly to workspace/configuration.

## TODO

- Execute real HealingAgent A2A / Nacos / DashScope manual acceptance.
- Harden persistent storage beyond the current in-memory workspace service.
- Implement controlled rollback/version switching before enabling rollback
  apply behavior.
- Add UI/SSE repair views if a later phase requires them.
- Consider broader end-to-end tests once the real service chain is manually
  accepted.

## Architecture Debt / Doc Mismatch

- Legacy remote invocation classes remain from transition phases. They are an
  architecture debt and must not be copied into new Agent modules.
- `docs/04_DATA_MODELS.md` still describes an older `RepairPlan` shape
  (`repairId`, `validationId`, singular `failureAnalysis`, `repairActions`,
  `status`). Current code uses the Phase 8 DTO shape listed above. This was
  recorded rather than broadly rewriting the older model document in this
  closeout pass.
