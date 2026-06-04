# CODEX_DOC_INDEX

This index helps Codex choose the smallest useful reading set for each task. Do
not read all `docs/00-09` by default.

## 1. Default Reading Order

For any development task, read these first:

1. `AGENTS.md`
2. `docs/CODEX_CURRENT_STATE.md`
3. `docs/CODEX_DOC_INDEX.md`

Then read only the task-specific documents and directly related source files.

## 2. Task Type To Documents

| Task type | Required documents |
| --- | --- |
| Project positioning / long-term boundaries | `docs/00_PROJECT_BRIEF.md`, `docs/01_SCOPE_AND_BOUNDARIES.md` |
| Maven modules / dependencies / package boundaries | `docs/02_MAVEN_MODULES.md` |
| Module responsibilities / architecture boundaries | `docs/03_MODULE_DESIGN.md`, optionally `docs/02_MAVEN_MODULES.md` |
| DTO / Workspace / Artifact | `docs/04_DATA_MODELS.md` |
| API / Controller / unified response | `docs/05_API_DESIGN.md` |
| Phase plan / acceptance criteria | `docs/06_DEV_PLAN.md` |
| Test scenarios / fixtures / failure cases | `docs/07_TEST_DATA_AND_SCENARIOS.md` |
| Run / startup / manual validation | `docs/08_RUN_AND_TEST.md` |
| Agent / Tool / MCP / A2A / Parser / Validator | `docs/09_AGENT_BUILD_GUIDE.md` |

## 3. Phase Handoffs

| Phase | Handoff document |
| --- | --- |
| Phase 3 | `docs/phase-handoffs/PHASE_03_HANDOFF.md` |
| Phase 4 | `docs/phase-handoffs/PHASE_04_HANDOFF.md` |
| Phase 5 | `docs/phase-handoffs/PHASE_05_HANDOFF.md` |
| Phase 6 P0-P4 notes | `docs/phase-handoffs/PHASE_06_P0_P4_NOTES.md` |
| Phase 6 complete handoff | `docs/phase-handoffs/PHASE_06_HANDOFF.md` |
| Phase 7 | `docs/phase-handoffs/PHASE_07_HANDOFF.md` |
| Phase 8 | `docs/phase-handoffs/PHASE_08_HANDOFF.md` |
| Phase 9 | `docs/phase-handoffs/PHASE_09_HANDOFF.md` |

## 4. Common Task Reading Suggestions

| Common task | Recommended reading |
| --- | --- |
| Agent implementation | Default 3 docs + `docs/09_AGENT_BUILD_GUIDE.md` + current agent module + related DTO sections + needed test scenarios |
| API / Controller task | Default 3 docs + `docs/05_API_DESIGN.md` + related `mac-tav-web` controller/DTO + `WorkflowOrchestrator` interface |
| Maven / dependency task | Default 3 docs + `docs/02_MAVEN_MODULES.md` + root `pom.xml` + relevant module `pom.xml` |
| Workspace / Artifact task | Default 3 docs + `docs/04_DATA_MODELS.md` + related `mac-tav-model-core` services/tests |
| A2A / Nacos task | Default 3 docs + `docs/09_AGENT_BUILD_GUIDE.md` + `docs/08_RUN_AND_TEST.md` + related Agent/Orchestrator A2A configuration |
| Test task | Default 3 docs + `docs/07_TEST_DATA_AND_SCENARIOS.md` + target module tests + needed fixture JSON |
| Code review | Default 3 docs + `git diff` / `git status` + docs and files directly related to changed files |
| New phase handoff | Default 3 docs + latest `docs/phase-handoffs/PHASE_xx_HANDOFF.md` + next phase docs/source |

## 5. Phase 10 Starting Scope

For a new Codex window after Phase 9, read:

1. `AGENTS.md`
2. `docs/CODEX_CURRENT_STATE.md`
3. `docs/CODEX_DOC_INDEX.md`
4. `docs/phase-handoffs/PHASE_09_HANDOFF.md`
5. Task-specific API / model / module docs only as needed.

Phase 9 completed durable persistence, SSE, async job submission, artifact
version switching, and startup job convergence. Later phases should treat MySQL
as authoritative state and Redis as realtime/lock infrastructure only.

## 6. Phase 7 Historical Scope

For a new Codex window implementing Phase 7 VerificationAgent, read:

1. `AGENTS.md`
2. `docs/CODEX_CURRENT_STATE.md`
3. `docs/CODEX_DOC_INDEX.md`
4. `docs/phase-handoffs/PHASE_06_HANDOFF.md`
5. `docs/06_DEV_PLAN.md` Phase 7
6. `docs/03_MODULE_DESIGN.md` Verification / Orchestrator / Web sections
7. `docs/04_DATA_MODELS.md` sections for `NetworkIntent`, `NetworkPlan`, `ConfigSet`, `ExecutionReport`, `ValidationReport`, `NetworkWorkspace`, `NetworkArtifact`
8. `docs/07_TEST_DATA_AND_SCENARIOS.md` verification scenarios
9. `docs/08_RUN_AND_TEST.md` test and manual validation rules
10. `docs/09_AGENT_BUILD_GUIDE.md`

Phase 7 must consume execution facts and decide intent satisfaction in
`ValidationReport`. It must not rerun tests, execute shell, modify configs, or
perform Healing.

## 7. Prohibited Reading Patterns

- Do not read all `docs/00-09` by default.
- Do not scan the whole repository just in case.
- Do not load unrelated specialty docs into context.
- Do not let stale code override `AGENTS.md` or current handoff documents.
- Do not assume a placeholder module means the real stage is implemented.
