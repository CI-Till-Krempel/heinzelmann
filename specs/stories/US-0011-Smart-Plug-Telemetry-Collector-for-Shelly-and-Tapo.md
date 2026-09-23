# User Story

- Story ID: US-0011
- Title: Smart Plug Telemetry Collector for Shelly and Tapo
- Status: Ready
- Priority: Could
- Owner: Scrum Team
- Last Updated: 2026-09-23

## As an energy efficiency researcher, I want the Control Server to collect real-time wattage data from smart plugs connected to client laptops, so that energy consumption during jobs is accurately monitored.

## Acceptance Criteria
- Given a configured smart plug IP and credentials (Shelly Gen2 / Tapo P110), When the Control Server queries the smart plug API, Then it successfully reads instantaneous power (Watts) and cumulative energy (kWh).
- Given mapped smart plugs to node IDs, When power metrics are collected periodically, Then the Control Server records timestamped power telemetry in the node metrics store.

## Notes
Real-time energy measurements provide empirical ground-truth power data for laptop clusters.
- Dependencies: ['EP-0006', 'US-0001']

## Test Approach


### Tasks
- Implement SmartPlugClient interface with Shelly and Tapo adapters
- Create SmartPlugManager service with periodic polling loop
- Add node-to-plug configuration mapping schema
- Add mock API tests for Shelly and Tapo response parsing
