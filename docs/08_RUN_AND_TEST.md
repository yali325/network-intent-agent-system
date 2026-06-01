# Run, Test, And Manual Validation

This document defines MAC-TAV build commands, test commands, environment
variables, manual validation paths, and troubleshooting notes. API contracts are
defined in `docs/05_API_DESIGN.md`; DTO contracts are defined in
`docs/04_DATA_MODELS.md`.

## 1. Build And Test

Compile the full reactor:

```bash
mvn compile
```

Run all tests:

```bash
mvn test
```

Run common module tests:

```bash
mvn -pl mac-tav-intent-agent -am test
mvn -pl mac-tav-planning-agent -am test
mvn -pl mac-tav-configuration-agent -am test
mvn -pl mac-tav-execution -am test
mvn -pl mac-tav-orchestrator -am test
mvn -pl mac-tav-web -am test
```

Default automated tests must not call real external model APIs, real Nacos,
real Qdrant, real Mininet/Ryu, or the real Python executor.

If Windows file locks break `mvn clean`, do not repeatedly clean. Prefer
`mvn compile` or targeted module tests.

## 2. Backend Startup

The long-term architecture is A2A multi-agent service mode:

1. Start Nacos.
2. Start the needed professional Agent services.
3. Confirm Agent Cards are registered/discoverable.
4. Start `mac-tav-web`.
5. `mac-tav-web` calls Orchestrator; Orchestrator calls professional agents
   through A2A discovery and invocation.

Start the Web/API entry:

```bash
mvn -pl mac-tav-web -am spring-boot:run
```

Codex must not leave `spring-boot:run`, `npm run dev`, uvicorn, Ryu, or Mininet
running as long-lived foreground processes.

## 3. Environment Variables

Common variables:

| Variable | Purpose |
| --- | --- |
| `aliApi-key` | DashScope API key for real manual Agent validation |
| `DASHSCOPE_CHAT_MODEL` | DashScope model name |
| `SERVER_PORT` | Web server port |
| `VITE_API_BASE_URL` | Frontend API base URL |
| `NACOS_SERVER_ADDR` | Nacos server address for A2A service mode |
| `MACTAV_RUN_MININET_RYU_IT` | Opt-in flag for real Mininet/Ryu Java integration test |

Do not commit API keys, server credentials, SSH keys, tokens, or real public
server addresses.

## 4. Agent Test Rules

Parser / Validator tests must:

- Use fixed fixtures.
- Validate schema-to-DTO conversion.
- Validate legal and illegal outputs.
- Reject cross-stage overreach.
- Avoid real model calls.

Real Agent manual validation may use a real `ChatModel`, but API keys must come
from local environment/private config and must not be logged or committed.

## 5. API Debugging

Long-term API prefix: `/api/v1`.

Common current endpoints:

- `POST /api/v1/tasks`
- `GET /api/v1/workspaces/{taskId}`
- `POST /api/v1/workflows/{taskId}/run`
- `POST /api/v1/workflows/{taskId}/plan`
- `POST /api/v1/workflows/{taskId}/config`
- `POST /api/v1/executions/{taskId}/run`
- `GET /api/v1/executions/{taskId}`

Controller boundaries:

- Controllers call Orchestrator or query services only.
- Controllers do not call concrete Agent beans, `ChatModel`, `ReactAgent`,
  `ExecutionAdapter`, Python executor, or shell commands directly.
- Controllers do not accept arbitrary shell, command, script, or raw execution
  text.

## 6. Phase 6 Mininet/Ryu Native Validation

The validated Phase 6 Mininet/Ryu path is Ubuntu 22.04 native deployment of the
Python executor in `deploy/mininet-ryu-executor`.

Important constraints:

- The executor listens on port `18091`.
- Use private networking or a local tunnel; do not expose `18091` directly to
  the public internet.
- Ryu first version is fixed to `ryu.app.simple_switch_13 + ryu.app.ofctl_rest`.
- The executor is an internal structured execution service, not a public
  business API and not an Agent service.
- Docker is not the validated executor deployment path for Phase 6.
- Java and Python exchange structured request/response JSON only; no arbitrary
  shell is passed.

Health checks:

```bash
curl http://127.0.0.1:18091/health
curl http://127.0.0.1:18091/api/v1/ryu/status
curl http://127.0.0.1:18091/api/v1/mininet/status
```

Java manual execution properties:

```properties
mactav.execution.mode=MININET_RYU
mactav.execution.mininet-ryu.enabled=true
mactav.execution.mininet-ryu.base-url=http://127.0.0.1:18091
mactav.execution.mininet-ryu.connect-timeout-ms=3000
mactav.execution.mininet-ryu.read-timeout-ms=60000
```

Default tests stay offline:

```bash
mvn compile
mvn -pl mac-tav-execution -am test
```

Run the real Java-to-Python Mininet/Ryu integration test only when explicitly
requested:

```powershell
$env:MACTAV_RUN_MININET_RYU_IT="true"
mvn -pl mac-tav-execution -am -Dtest=MininetRyuExecutorManualIT "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Successful Phase 6 validation covered:

- `MininetRyuExecutionAdapter -> MininetRyuExecutorClient -> Python executor -> Ryu/Mininet`.
- Minimal h1-s1-h2 topology.
- `RYU_CONTROLLER_CHECK`, `TOPOLOGY_STATE_CHECK`, and `PING_TEST` passed.
- Cleanup succeeded.
- Follow-up Mininet status reported `networkRunning=false`.

Execution reports facts only. Whether those facts satisfy the original business
intent is Phase 7 VerificationAgent responsibility.

## 7. Python Executor Manual Commands

See `deploy/mininet-ryu-executor/README.md` for full setup.

Native Ubuntu 22.04 dependency example:

```bash
sudo apt-get update
sudo apt-get install -y python3 python3-venv python3-pip mininet openvswitch-switch ryu iputils-ping traceroute iperf3 curl
```

Start Ryu:

```bash
ryu-manager ryu.app.simple_switch_13 ryu.app.ofctl_rest
```

Start executor:

```bash
EXECUTOR_HOST=0.0.0.0 EXECUTOR_PORT=18091 uvicorn app.main:app --host 0.0.0.0 --port 18091
```

Codex must not start or leave these processes running during normal coding
tasks unless the user explicitly asks for manual runtime work.

## 8. Troubleshooting

Mininet/Ryu connection failure:

- Check Ryu REST is reachable at `http://127.0.0.1:8080` from the executor
  environment.
- Check Ryu was started with `ryu.app.simple_switch_13 ryu.app.ofctl_rest`.
- Check Mininet and Open vSwitch are installed and usable.
- Check the local tunnel or private network is forwarding `18091`.
- Check failures are captured in `ExecutionReport.errors`.

Maven targeted manual IT with `-am`:

- If upstream modules do not contain the specified test, include
  `"-Dsurefire.failIfNoSpecifiedTests=false"` in PowerShell.

A2A / Nacos failure:

- Check Nacos is running.
- Check professional Agent services are registered.
- Check Agent Cards include capability, input/output contract, and endpoint.
- Do not fall back to direct local Agent bean calls as the long-term path.

API key missing:

- Check local private environment variables.
- Confirm no key is committed or printed in logs.

## 9. Long-Term Acceptance Checklist

- Nacos is available.
- Professional Agents can run independently.
- Agent Cards are discoverable.
- Orchestrator can call professional Agents through A2A.
- `NetworkIntent`, `NetworkPlan`, `ConfigSet`, `ExecutionReport`,
  `ValidationReport`, and `RepairPlan` can each be produced by their owning
  phase.
- All stage artifacts are written by Orchestrator / Model Core.
- Web remains an API/UI entry and does not directly call concrete Agents or
  execution adapters.
- Execution uses structured adapters and safety policy, not arbitrary shell.
- Verification, not Execution, decides whether business intent is satisfied.
