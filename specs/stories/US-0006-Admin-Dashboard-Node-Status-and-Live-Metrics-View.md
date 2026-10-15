# User Story

- Story ID: US-0006
- Title: Admin Dashboard Node Status and Live Metrics View
- Status: Reviewed
- Priority: Must
- Owner: Scrum Team
- Last Updated: 2026-10-15

## As an IT administrator, I want a desktop dashboard showing real-time status and hardware metrics of all cluster nodes, so that I can easily monitor cluster health and capacity at a glance.

## Acceptance Criteria
- Given the Compose for Desktop Admin Dashboard connected to Control Server, When viewing the Nodes tab, Then all registered nodes are listed with their ID, OS, status (Online/Offline/Busy), CPU, and RAM metrics.
- Given changes in node status or metrics on the server, When updates arrive, Then the dashboard UI updates within 5 seconds without requiring manual refresh.

## Notes


## Test Approach
Unit tests verifying AdminDashboardClient response parsing, NodeStatusViewModel state emissions, and UI metric rendering rules.

### Tasks
- Define NodeStatusViewModel and NodeUiState models representing node inventory and real-time CPU/RAM vitals
- Implement AdminDashboardClient interacting with Ktor Control Server /api/nodes endpoint
- Build Compose for Desktop NodesTab UI component rendering node status table, badges, and metrics
- Implement automated polling refresh mechanism updating state within 5 seconds of server updates
- Write unit tests verifying ViewModel state transitions, node list rendering logic, and metric parsing
