# User Story

- Story ID: US-0003
- Title: Client Daemon Power Assertion and Sleep Prevention
- Status: Ready
- Priority: Must
- Owner: Scrum Team
- Last Updated: 2026-09-23

## As a system operator, I want the client daemon to prevent laptops from entering sleep mode while executing assigned batch jobs, so that long-running tasks complete without interruption.

## Acceptance Criteria
- Given a client laptop on AC power running an assigned workload, When a job is active, Then the daemon creates an OS power assertion (IOPMAssertion on macOS / SetThreadExecutionState on Windows) preventing sleep.
- Given an active job completion or cancellation, When no more jobs are queued on the node, Then the daemon releases the power assertion and restores default OS sleep policies.

## Notes
Laptops stay awake during active night jobs and resume normal sleep behavior once jobs finish.
- Dependencies: ['EP-0001', 'US-0002']

## Test Approach


### Tasks
- Implement macOS IOPMAssertionCreateWithName binding via Kotlin/Native or JNA
- Implement Windows SetThreadExecutionState binding
- Create PowerAssertionManager lifecycle controller
- Add automated unit/mock tests for assertion creation and release
