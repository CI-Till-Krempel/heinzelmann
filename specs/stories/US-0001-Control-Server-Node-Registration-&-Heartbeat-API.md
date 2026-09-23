# User Story

- Story ID: US-0001
- Title: Control Server Node Registration & Heartbeat API
- Status: Reviewed
- Priority: Must
- Owner: Scrum Team
- Last Updated: 2026-09-23

## As a Control Server administrator, I want an API for node registration and periodic heartbeat telemetry, so that the server maintains an accurate real-time inventory of cluster nodes and their health.

## Acceptance Criteria
- Given a running Ktor Control Server, When a client daemon sends a registration POST request with node ID, OS type, and hardware specs, Then the server stores the node and responds with 200 OK and registration confirmation.
- Given a registered node, When the node sends a periodic heartbeat with CPU/RAM metrics, Then the server updates the node's last-seen timestamp and metric cache.
- Given a registered node that stops sending heartbeats, When the timeout threshold (60 seconds) expires, Then the server marks the node status as Offline.

## Notes
Control Server reliably tracks cluster node inventory and health status in real time.
- Dependencies: ['EP-0002']
- ⚠️ REVIEW DENIED at Tested by QA: Automated test suite execution failed: running the test suite found no tests actually ran (no tests collected). The test harness requires runnable tests (such as a pytest test suite in tests/ or test wrappers) to be collected and executed before the story can pass the Tested stage.

## Test Approach
Ktor testApplication integration tests covering POST /api/nodes/register, POST /api/nodes/{id}/heartbeat, GET /api/nodes, and timeout threshold evaluation.

### Tasks
- Create control-server module and build.gradle.kts
- Implement NodeModels data structures
- Implement NodeRegistry repository with TTL / 60s timeout handling
- Implement Ktor routes for register, heartbeat, and node query
- Add integration tests validating registration, telemetry caching, and offline status transition
