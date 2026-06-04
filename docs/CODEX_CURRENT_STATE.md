# CODEX_CURRENT_STATE

Last updated: Phase 9 closeout.

## 1. Current Project Phase

MAC-TAV has completed Phase 9 code scope for durable persistence, artifact
history APIs, event/SSE, async workflow jobs, controlled artifact version
switching, and startup job convergence.

Phase 9 moves the default service path from in-memory state to:

```text
Spring Web -> Orchestrator -> Model Core -> MyBatis -> MySQL
Redis -> event broadcast / SSE fan-out / task runtime lock
```

MySQL is the authoritative state source. Redis is not durable history.

## 2. Phase 9 Completed Capabilities

- MyBatis + MySQL persistence for tasks, workspace state, artifacts, events,
  change records, execution records, and workflow jobs.
- Manual SQL initialization through `deploy/mysql/phase9_schema.sql`. Flyway and
  JPA/Hibernate are not used.
- MySQL services are production default with
  `mactav.persistence.type=mysql`. In-memory services require explicit
  `mactav.persistence.type=inmemory` or `test` / `inmemory` profile.
- Artifact APIs expose summaries, payload lookup, current artifact lookup,
  version history, diff, and controlled current-pointer switch.
- Workspace timeline / changes and Event history APIs read from MySQL.
- Event types are standardized in `WorkspaceEventTypes`.
- Redis event publishing happens after MySQL event persistence; Redis failure
  does not erase durable event history.
- Web SSE is available at `GET /api/v1/events/{taskId}` and pushes only safe
  event summaries.
- Public long-running POST APIs now submit workflow jobs and return `jobId`.
- `WorkflowJobService`, `WorkflowAsyncService`, `WorkflowAsyncExecutor`, and
  Redis token task locks are implemented.
- Startup recovery scans active `PENDING` / `RUNNING` jobs and marks jobs
  `INTERRUPTED` when no active task lock exists. It records
  `workflow.interrupted` and does not re-execute agents or execution adapters.
- Artifact version switch updates only workspace current artifact references and
  current version fields. It records `VERSION_SWITCH` and
  `artifact.version_switched`.

## 3. Current API Notes

Long-running POST APIs return `WorkflowJobSubmitResponse` with `jobId`:

- `POST /api/v1/workflows/{taskId}/start`
- `POST /api/v1/workflows/{taskId}/run`
- `POST /api/v1/workflows/{taskId}/plan`
- `POST /api/v1/workflows/{taskId}/config`
- `POST /api/v1/workflows/{taskId}/rerun/{stage}`
- `POST /api/v1/workflows/{taskId}/continue-from/{stage}`
- `POST /api/v1/executions/{taskId}/run`
- `POST /api/v1/validations/{taskId}/run`
- `POST /api/v1/repairs/{taskId}/analyze`
- `POST /api/v1/repairs/{taskId}/actions/{actionId}/apply`

Job queries:

- `GET /api/v1/workflows/jobs/{jobId}`
- `GET /api/v1/tasks/{taskId}/jobs`

Repair approve / reject remain synchronous.

Artifact version switch:

```text
POST /api/v1/artifacts/{taskId}/{artifactId}/switch
```

Request body:

```json
{
  "artifactType": "CONFIG_SET",
  "reason": "manual rollback to known good config artifact",
  "actor": "operator"
}
```

This is a workspace current-pointer switch only. It does not execute shell,
does not call `ExecutionAdapter`, and does not push configuration to devices.

## 4. Runtime Configuration

Production defaults expect MySQL and Redis to be available:

- `mactav.persistence.type=mysql`
- `mactav.events.publisher=redis`
- `mactav.task-lock.type=redis`
- `mactav.async.recovery.enabled=true`

Tests and explicit local in-memory runs must set:

- `mactav.persistence.type=inmemory`
- `mactav.task-lock.type=inmemory` or `noop`
- `mactav.events.publisher=noop`

Do not commit real passwords, Redis credentials, public IPs, API keys, or
tokens. Local private credentials can be supplied through environment variables
or uncommitted local config.

## 5. Current TODO

- Manually validate MySQL + Redis runtime after applying
  `deploy/mysql/phase9_schema.sql`.
- Manually validate real A2A / Nacos / DashScope Agent chain.
- Add long-running Redis lock renewal before running very long production jobs.
- Add optional recovery UI/operator workflow for manually deciding rerun vs
  continue-from after startup interruption.
- Build UI views for artifacts, timeline, SSE, jobs, and version switching if a
  later phase requires them.

## 6. Known Architecture Debt / Doc Mismatch

- Legacy remote invocation classes still exist from transition phases. They are
  architecture debt and must not be copied into new Agent modules.
- `docs/04_DATA_MODELS.md` still contains old RepairPlan field names. Current
  code and `docs/phase-handoffs/PHASE_08_HANDOFF.md` remain the authority for
  `RepairPlan`, `RepairAction`, and `FailureAnalysis`.
- `RepairAction` currently has no explicit target artifact id for rollback.
  `ROLLBACK` repair apply remains rejected unless the user calls the explicit
  Artifact version switch API with an exact target artifact.
