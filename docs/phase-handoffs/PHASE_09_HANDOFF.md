# Phase 9 Handoff - Persistence, Async Jobs, SSE, And Recovery

## Scope

Phase 9 hardens MAC-TAV into a durable long-running service path:

```text
MySQL durable state
  + Redis realtime events / SSE / task locks
  + async workflow jobs
  + artifact version history and current-pointer switching
```

This phase does not modify concrete Agent modules, does not change the
`ResponseSchema -> Parser -> DTO -> Validator` chain, does not add hand-written
A2A controllers or Agent Card publishers, and does not execute real rollback
commands.

## Persistence And Initialization

- Model Core uses MyBatis + MySQL + manually executed SQL.
- Schema file: `deploy/mysql/phase9_schema.sql`.
- Flyway is not used. JPA/Hibernate is not used.
- The schema intentionally has no foreign keys.
- MySQL is the authoritative source for tasks, workspace current state,
  artifacts, execution records, events, changes, and workflow jobs.
- Redis is used for realtime event broadcast, SSE fan-out, and task locks only.

Production defaults expect:

- `mactav.persistence.type=mysql`
- `mactav.events.publisher=redis`
- `mactav.task-lock.type=redis`

Tests and explicit local in-memory runs use:

- `mactav.persistence.type=inmemory`
- `mactav.task-lock.type=inmemory` or `noop`
- `mactav.events.publisher=noop`

## Artifact Version Switch

Endpoint:

```text
POST /api/v1/artifacts/{taskId}/{artifactId}/switch
```

Request:

```json
{
  "artifactType": "CONFIG_SET",
  "reason": "manual switch to known good artifact",
  "actor": "operator"
}
```

Switch behavior:

- validates target artifact exists;
- validates target task id and artifact type;
- updates only `current_artifact_refs_json` and the matching
  `current*Version` field;
- never modifies artifact `payloadJson`;
- never executes shell;
- never calls `ExecutionAdapter`;
- never pushes configuration to devices;
- records `WorkspaceChangeRecord.changeType=VERSION_SWITCH`;
- appends `artifact.version_switched`.

## Event, SSE, And History

- MySQL `workspace_event` is the authoritative event history.
- Redis publishes realtime event summaries after MySQL event append succeeds.
- SSE endpoint: `GET /api/v1/events/{taskId}` with `text/event-stream`.
- Event history endpoint: `GET /api/v1/events/{taskId}/history`.
- SSE data includes only safe event summary fields and never includes prompt,
  API key, full exception stack, full A2A request/response, model raw output, or
  artifact payload JSON.

## Async Workflow Jobs

Public long-running POST APIs submit jobs and return `jobId`; they no longer
return stage DTOs synchronously.

Submit sequence:

1. Check MySQL for active `PENDING` / `RUNNING` job.
2. Acquire Redis token lock.
3. Check MySQL active job again.
4. Create `workflow_job=PENDING`.
5. Start async worker.
6. On create/start failure, release the lock owned by this token.

`WorkflowAsyncExecutor` handles only job status, workflow-level events, lock
release, and calls to existing synchronous Orchestrator methods.

## Startup Recovery

`WorkflowJobRecoveryService` runs on startup when
`mactav.async.recovery.enabled=true` or the property is missing.

Recovery behavior:

- scans `workflow_job` for `PENDING` / `RUNNING`;
- checks whether a task lock is still active;
- if no lock exists, marks the job `INTERRUPTED`;
- appends `workflow.interrupted`;
- does not rerun Agents;
- does not call `WorkflowAsyncExecutor`;
- does not call `ExecutionAdapter`;
- leaves manual recovery to later `rerun` / `continue-from` calls.

Redis lock renewal is not implemented in Phase 9.

## Repair Rollback Boundary

`RepairAction` currently has no explicit target artifact id. `ROLLBACK` apply
therefore remains rejected with a clear `BusinessException`; the system must not
guess rollback target versions from natural language.

Operators can use the explicit Artifact switch API when they know the exact
target artifact id. This is still only a workspace current-pointer switch, not
real network rollback.

## Tests Run

Phase 9 closeout commands passed:

```bash
mvn -pl mac-tav-model-core -am test
mvn -pl mac-tav-orchestrator -am test
mvn -pl mac-tav-web -am test
mvn compile
```

Automated tests remain offline and do not require real MySQL, Redis, Nacos,
Qdrant, Mininet/Ryu, model APIs, or devices.

## Manual Validation

1. Create MySQL database `mac_tav`.
2. Execute `deploy/mysql/phase9_schema.sql`.
3. Start local Redis.
4. Configure local private MySQL/Redis values through environment variables or
   uncommitted local config.
5. Start `mac-tav-web` manually when needed.
6. Submit a long-running API and capture `jobId`.
7. Query `GET /api/v1/workflows/jobs/{jobId}` and
   `GET /api/v1/events/{taskId}/history`.
8. Connect `GET /api/v1/events/{taskId}` for SSE summaries.
9. Test artifact switch only with an explicit target `artifactId`.

Do not write real passwords, API keys, tokens, public IPs, or private
credentials into docs, source files, fixtures, or logs.

## Remaining TODO

- Real MySQL + Redis manual validation.
- Real A2A / Nacos / DashScope full-chain validation.
- Redis task-lock renewal for very long jobs.
- Operator/UI workflow for interrupted jobs.
- UI for artifacts, timeline, SSE, job status, and version switching.

## Known Doc Mismatch

`docs/04_DATA_MODELS.md` still contains old RepairPlan fields. Current Java DTOs
and `docs/phase-handoffs/PHASE_08_HANDOFF.md` are the authority.
