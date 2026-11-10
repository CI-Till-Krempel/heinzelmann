# User Story

- Story ID: US-0009
- Title: Admin Dashboard Job Submission and Status Pipeline View
- Status: Reviewed
- Priority: Should
- Owner: Scrum Team
- Last Updated: 2026-11-10

## As an IT administrator, I want a Compose for Desktop UI to submit batch job configurations and view active job progress in real time, so that I can manage and monitor cluster workload execution.

## Acceptance Criteria
- Given the Compose for Desktop Admin Dashboard, When the admin enters job parameters (docker image, command, target node tags) and clicks Submit, Then the job is posted to the Control Server and appears in the active queue table.
- Given active running jobs in the cluster, When the admin views the Job Pipeline view, Then each job's live state (Queued, Running, Succeeded, Failed) and execution duration are dynamically updated.

## Notes

- ⚠️ REVIEW DENIED at Tested by QA: The test suite failed to execute because there is no Gradle wrapper (`gradlew`) committed in the repository. Please commit the `gradlew` script and `gradle/wrapper/gradle-wrapper.jar` so the tests can run on CI.

## Test Approach
Unit testing for ViewModel, manual UI checks.

### Tasks
- Build Job Submission form view in Compose for Desktop
- Connect form to POST /api/jobs Control Server endpoint
- Build Job Pipeline data table with status indicators
- Add unit tests for Job Submission ViewModel
