# Phase 9 Plan - Persistence, Async Jobs, and SSE

## Scope

Phase 9 hardens MAC-TAV from an in-memory/offline workflow into a durable,
long-running service path.

This P0-P3 pass only records the technical decision, dependency boundary,
runtime configuration keys, and the first manual MySQL schema file. It does not
add Java entity, mapper, repository, assembler, service implementation,
controller, SSE, async executor, workflow job model, or Redis business code.

## Technical Decisions

- MySQL is the authoritative workflow state source for tasks, current workspace
  pointers, artifacts, execution records, events, change records, and later
  workflow jobs.
- Redis is reserved only for realtime events, short-lived cache, SSE messages,
  and workflow/job locks. It must not become the authoritative source for
  durable workflow state.
- Default business state must not rely on in-memory storage after the durable
  Model Core implementation lands.
- In-memory storage may remain only for tests or an explicit local profile. It
  is not the default production workflow path.
- Phase 9 uses MyBatis + MySQL + manually executed SQL initialization scripts +
  Redis.
- Flyway is not used. JPA/Hibernate is not used.
- DTOs remain separate from future database entity/row models.
- Controllers still call only Orchestrator or query services. They must not call
  concrete Agents, model APIs, execution clients, or shell commands.
- Public long-running APIs should later become asynchronous and return `jobId`
  directly. Do not add new public `/sync` workflow APIs.
- Existing Orchestrator synchronous stage methods stay as the core execution
  unit for the future async executor and as unit-test entry points.
- MySQL / Redis absence must fail visibly once the durable path is enabled. It
  must not silently downgrade to the in-memory main path.

## Dependency Boundary

- The root `pom.xml` manages `mybatis-spring-boot-starter` under
  `dependencyManagement`.
- Spring Boot parent dependency management continues to manage
  `mysql-connector-j` and Redis starter versions.
- `mac-tav-model-core` is the first module to depend on MyBatis and the MySQL
  driver because future Mapper/Repository code belongs there.
- Redis Java dependencies are not added to a child module in P0-P3 because this
  pass does not add Redis imports or Redis business code. Runtime configuration
  is reserved in `mac-tav-web/src/main/resources/application.yml`.
- `mac-tav-web` keeps existing A2A/Nacos/Execution/Agent invocation settings and
  only appends MySQL, Redis, MyBatis, async, and SSE configuration keys.

## Manual SQL Initialization

The first schema file is:

```text
deploy/mysql/phase9_schema.sql
```

Codex does not connect to or operate the user's MySQL instance. Create the
database and apply the schema manually:

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS mac_tav DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p mac_tav < deploy/mysql/phase9_schema.sql
```

The schema intentionally has no foreign keys. Data consistency will be enforced
later by Repository / Service transactions and Orchestrator boundaries.

## Runtime Configuration

Default local values are safe placeholders, not credentials:

```text
MACTAV_MYSQL_URL=jdbc:mysql://127.0.0.1:3306/mac_tav?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
MACTAV_MYSQL_USERNAME=root
MACTAV_MYSQL_PASSWORD=
MACTAV_REDIS_HOST=127.0.0.1
MACTAV_REDIS_PORT=6379
MACTAV_REDIS_PASSWORD=
MACTAV_REDIS_DATABASE=0
```

Existing Nacos/A2A and model API key rules remain unchanged. Do not commit real
database passwords, Redis passwords, model API keys, or service credentials.

## P4-P6 Implementation Status

P4-P6 now add the first MyBatis persistence implementation in
`mac-tav-model-core`:

- DTO / entity / mapper / repository / service remain separated.
- MySQL/MyBatis services are the default when `mactav.persistence.type=mysql`
  or the property is missing.
- In-memory services are limited to `mactav.persistence.type=inmemory` or the
  `test` / `inmemory` profile.
- `saveStageArtifact` is transactional and uses `max(version)+1` with
  `uk_artifact_task_type_version` as the final concurrency guard.
- `getWorkspace` / `findWorkspace` rebuild current workspace DTOs from
  `network_task`, `network_workspace_state`, and current artifact payload JSON.
- `workflow_job` has only entity / mapper / repository CRUD; no async job
  service, executor, lock service, domain model, or job enums are added.

## P7+ TODO

- Add Artifact API/history implementation if needed by the UI path.
- Add async workflow job execution and `jobId` API responses.
- Add Redis-backed SSE publishing/subscription and event history recovery.
- Add visible startup/runtime failure behavior for missing required MySQL or
  Redis in durable profiles.

## Known Mismatch

`docs/04_DATA_MODELS.md` still carries older RepairPlan field names. Phase 9
continues to use the current Java DTO and Phase 8 handoff contract instead:
`taskId`, `validationVersion`, `repairVersion`, `overallRepairStrategy`,
list-based `failureAnalysis`, `actions`, `requiresUserConfirmation`,
`stageStatus`, and `createTime`.
