# MAC-TAV Mininet/Ryu Executor

This directory contains the Phase 6 P9 Python executor for native Ubuntu 22.04
deployment. The service exposes a structured FastAPI API on port `18091` and
executes bounded Mininet/Ryu operations only. Docker is not the deployment path
for this executor.

The first Ryu version is fixed to:

```bash
ryu-manager ryu.app.simple_switch_13 ryu.app.ofctl_rest
```

External requests cannot choose Ryu apps, pass shell text, upload topology
scripts, execute Huawei CLI, or decide whether the original business intent was
satisfied. Verification remains a later MAC-TAV phase.

## Ubuntu 22.04 Native Deployment

Install expected native dependencies on the server:

```bash
sudo apt-get update
sudo apt-get install -y python3 python3-venv python3-pip mininet openvswitch-switch ryu iputils-ping traceroute iperf3 curl
```

Create a virtual environment from this directory:

```bash
python3 -m venv .venv
. .venv/bin/activate
pip install -r requirements.txt
```

Start Ryu manually in one terminal:

```bash
ryu-manager ryu.app.simple_switch_13 ryu.app.ofctl_rest
```

Start the executor in another terminal:

```bash
EXECUTOR_HOST=0.0.0.0 EXECUTOR_PORT=18091 uvicorn app.main:app --host 0.0.0.0 --port 18091
```

`RYU_AUTO_START=false` is the default. When `RYU_AUTO_START=true`, the executor
may start only the fixed Ryu command shown above, and it will only stop the Ryu
process that it started itself.

Small 2C2G cloud servers should run only small topologies. The first hard
limits are `hosts <= 8`, `switches <= 4`, and `links <= 16`; lower values are
recommended when memory pressure is visible.

## Health And Execution

Check executor and environment status:

```bash
curl http://127.0.0.1:18091/health
```

Run a minimal h1-s1-h2 topology:

```bash
curl -X POST http://127.0.0.1:18091/api/v1/executions/run \
  -H 'Content-Type: application/json' \
  -d '{
    "executionId": "exec-h1-s1-h2",
    "taskId": "task-demo",
    "planId": "plan-demo",
    "configSetId": "config-demo",
    "executionVersion": 1,
    "topology": {
      "nodes": [
        {"id": "h1", "nodeType": "host", "ipAddress": "10.0.0.1/24"},
        {"id": "s1", "nodeType": "switch"},
        {"id": "h2", "nodeType": "host", "ipAddress": "10.0.0.2/24"}
      ],
      "links": [
        {"id": "l1", "sourceNode": "h1", "targetNode": "s1"},
        {"id": "l2", "sourceNode": "s1", "targetNode": "h2"}
      ]
    },
    "actions": [
      {
        "actionId": "action-ryu-check",
        "actionType": "RYU_CONTROLLER_CHECK",
        "traceRefs": {"planElementIds": ["plan-demo"]}
      },
      {
        "actionId": "action-topology-check",
        "actionType": "TOPOLOGY_STATE_CHECK",
        "traceRefs": {"planElementIds": ["plan-demo"]}
      }
    ],
    "testCommands": [
      {
        "testId": "test-ping-h1-h2",
        "testType": "PING",
        "sourceNode": "h1",
        "targetNode": "h2",
        "parameters": {"count": 3},
        "traceRefs": {"testIds": ["test-ping-h1-h2"]}
      }
    ],
    "traceRefs": {"planElementIds": ["plan-demo"]}
  }'
```

Run controlled cleanup:

```bash
curl -X POST http://127.0.0.1:18091/api/v1/executions/cleanup \
  -H 'Content-Type: application/json' \
  -d '{"executionId":"cleanup-demo","traceRefs":{"planElementIds":["plan-demo"]}}'
```

## Local Tunnel

Do not expose `18091` directly to the public internet. Prefer a firewall rule,
private network, or a local tunnel. A generic SSH local-forward pattern is:

```bash
ssh -L 18091:127.0.0.1:18091 user@server
```

Replace placeholders locally. Do not commit real server IPs, usernames,
passwords, SSH keys, API keys, or tokens.

## Java Manual Integration

Java-side Mininet/Ryu execution is enabled explicitly for manual validation. The
default CI path remains structure validation and does not connect to `18091`.

Use the following properties for a local tunnel or private executor endpoint:

```properties
mactav.execution.mode=MININET_RYU
mactav.execution.mininet-ryu.enabled=true
mactav.execution.mininet-ryu.base-url=http://127.0.0.1:18091
mactav.execution.mininet-ryu.connect-timeout-ms=3000
mactav.execution.mininet-ryu.read-timeout-ms=60000
```

Before running the Java manual integration test, check health:

```bash
curl http://127.0.0.1:18091/health
```

Then run the opt-in test from the repository root:

```powershell
$env:MACTAV_RUN_MININET_RYU_IT="true"
mvn -pl mac-tav-execution -am -Dtest=MininetRyuExecutorManualIT "-Dsurefire.failIfNoSpecifiedTests=false" test
```

The test uses `MininetRyuExecutionAdapter` with a minimal h1-s1-h2 topology and
calls the executor cleanup endpoint after the run. Do not run it in default CI,
and do not commit real server addresses or credentials.

## API Summary

- `GET /health`
- `GET /api/v1/ryu/status`
- `GET /api/v1/mininet/status`
- `POST /api/v1/executions/run`
- `POST /api/v1/executions/cleanup`

`POST /api/v1/executions/run` accepts structured JSON around an ExecutionPlan:

- `executionId`
- `taskId`
- `planId`
- `configSetId`
- `executionVersion`
- `topology`
- `actions`
- `cleanupActions`
- `testCommands`
- `timeoutSeconds`
- `traceRefs`

Safety validation rejects free-form shell command fields such as `command`,
`cmd`, `shell`, `script`, `cli`, or `rawCommand`, and rejects dangerous text
tokens such as generic shell execution, Huawei CLI markers, and arbitrary
download commands.

## Current Limits

- Java MininetRyuExecutorClient and MininetRyuExecutionAdapter exist for Phase 6
  manual validation, but are disabled by default unless the execution properties
  above explicitly enable `MININET_RYU`.
- Web/Orchestrator integration remains controlled by MAC-TAV configuration; this
  executor remains an internal structured execution service, not a public
  business API.
- Flow support is read-only through Ryu `ofctl_rest` query endpoints.
- The executor runs one execution at a time in process and returns
  `EXECUTOR_BUSY` for concurrent run requests.
