# User Story

- Story ID: US-0006
- Title: Admin Dashboard Node Status and Live Metrics View
- Status: Ready
- Priority: Must
- Owner: Scrum Team
- Last Updated: 2026-10-15

## As an IT administrator, I want a desktop dashboard showing real-time status and hardware metrics of all cluster nodes, so that I can easily monitor cluster health and capacity at a glance.

## Acceptance Criteria
- Given the Compose for Desktop Admin Dashboard connected to Control Server, When viewing the Nodes tab, Then all registered nodes are listed with their ID, OS, status (Online/Offline/Busy), CPU, and RAM metrics.
- Given changes in node status or metrics on the server, When updates arrive, Then the dashboard UI updates within 5 seconds without requiring manual refresh.

## Notes


## Test Approach

