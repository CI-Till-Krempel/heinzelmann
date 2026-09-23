# User Story

- Story ID: US-0009
- Title: Admin Dashboard Job Submission and Status Pipeline View
- Status: Ready
- Priority: Should
- Owner: Scrum Team
- Last Updated: 2026-09-23

## As an IT administrator, I want a Compose for Desktop UI to submit batch job configurations and view active job progress in real time, so that I can manage and monitor cluster workload execution.

## Acceptance Criteria
- Given the Compose for Desktop Admin Dashboard, When the admin enters job parameters (docker image, command, target node tags) and clicks Submit, Then the job is posted to the Control Server and appears in the active queue table.
- Given active running jobs in the cluster, When the admin views the Job Pipeline view, Then each job's live state (Queued, Running, Succeeded, Failed) and execution duration are dynamically updated.

## Notes
Operators can dispatch jobs and monitor progress through an intuitive desktop GUI without CLI scripting.
- Dependencies: ['EP-0005', 'US-0005']

## Test Approach


### Tasks
- Build Job Submission form view in Compose for Desktop
- Connect form to POST /api/jobs Control Server endpoint
- Build Job Pipeline data table with status indicators
- Add unit tests for Job Submission ViewModel
