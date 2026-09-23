# User Story

- Story ID: US-0002
- Title: Client Daemon Lifecycle & Metrics Collector
- Status: Ready
- Priority: Must
- Owner: Scrum Team
- Last Updated: 2026-09-23

## As a cluster administrator, I want a lightweight daemon running on client laptops that collects system metrics, so that the Control Server knows each node's capacity and current load.

## Acceptance Criteria
- Given a client machine (macOS or Windows), When the daemon service starts at boot, Then it runs as a low-footprint background process consuming <1% CPU and <50MB RAM in idle.
- Given a running daemon, When collecting system telemetry, Then it samples CPU usage, RAM utilization, battery charge level, and temperature, transmitting them via heartbeat.

## Notes
Client nodes reliably report system resource availability without impacting normal daytime laptop usability.
- Dependencies: ['EP-0001', 'US-0001']

## Test Approach


### Tasks
- Implement KMP platform daemon entry point for macOS (LaunchDaemon) and Windows Service
- Implement platform-specific hardware metrics collectors (sysctl / OSHI)
- Implement periodic reporting client to Control Server
- Add unit tests for metrics serialization and collector thresholds
