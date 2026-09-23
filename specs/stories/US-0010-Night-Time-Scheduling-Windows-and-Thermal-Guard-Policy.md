# User Story

- Story ID: US-0010
- Title: Night-Time Scheduling Windows and Thermal Guard Policy
- Status: Ready
- Priority: Should
- Owner: Scrum Team
- Last Updated: 2026-09-23

## As a system operator, I want to enforce strict night-time execution windows and pause jobs when battery falls below 50% or thermal throttling occurs, so that client hardware is protected and daytime work is undisturbed.

## Acceptance Criteria
- Given a scheduled batch job and a night-time execution window (22:00 to 06:00), When the current time is outside the allowed window, Then the dispatcher holds the job in Queued state without assigning it to nodes.
- Given a running job on a battery-powered client node, When battery level drops below 50% or CPU temperature exceeds 85°C, Then the daemon signals the Control Server, pauses or evicts the job container, and relinquishes the power assertion.

## Notes
Automated policy enforcement prevents battery degradation and overheating during unattended overnight runs.
- Dependencies: ['EP-0005', 'US-0002', 'US-0003']

## Test Approach


### Tasks
- Define ClusterPolicy schema (time windows, battery floor, thermal ceiling)
- Implement policy validation in Control Server dispatcher
- Implement hardware guard checks in client daemon heartbeat loop
- Add unit tests for policy evaluations and threshold triggers
