# User Story

- Story ID: US-0002
- Title: Client Daemon Lifecycle & Metrics Collector
- Status: Implemented
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
Unit tests for daemon lifecycle state machine, metrics gathering and serialization, and mock server integration tests.

### Tasks
- Create ClientDaemon core lifecycle controller (start, stop, heartbeat loop)
- Implement SystemMetricsCollector interface and mock/native metric providers for CPU, RAM, battery, temperature
- Implement DaemonApiClient for registering and sending heartbeats to Control Server
- Implement comprehensive unit and integration tests for metrics sampling and heartbeat dispatch
