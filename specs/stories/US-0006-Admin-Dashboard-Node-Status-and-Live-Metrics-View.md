# User Story

- Story ID: US-0006
- Title: Admin Dashboard Node Status and Live Metrics View
- Status: Ready
- Priority: Should
- Owner: Scrum Team
- Last Updated: 2026-09-23

## As an IT administrator, I want a desktop dashboard showing real-time status and hardware metrics of all cluster nodes, so that I can easily monitor cluster health and capacity at a glance.

## Acceptance Criteria
- Given the Compose for Desktop Admin Dashboard connected to Control Server, When viewing the Nodes tab, Then all registered nodes are listed with their ID, OS, status (Online/Offline/Busy), CPU, and RAM metrics.
- Given changes in node status or metrics on the server, When updates arrive, Then the dashboard UI updates within 5 seconds without requiring manual refresh.

## Notes
IT administrators gain continuous operational visibility into fleet health without manual server querying.
- Dependencies: ['EP-0005', 'US-0001']

## Test Approach


### Tasks
- Set up Compose for Desktop client module and Ktor HTTP/WebSocket client
- Build Node List and Node Detail views in Compose
- Implement reactive state management using Kotlin Coroutines StateFlow
- Add UI unit tests verifying node state rendering and status badge colors
