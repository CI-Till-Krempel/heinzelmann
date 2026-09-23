# User Story

- Story ID: US-0012
- Title: Energy Consumption Correlation and Reporting per Job
- Status: Ready
- Priority: Could
- Owner: Scrum Team
- Last Updated: 2026-09-23

## As an administrator, I want the Control Server to correlate smart plug energy data with individual job runs, so that the total kilowatt-hour cost of batch LLM inference jobs can be analyzed and reported.

## Acceptance Criteria
- Given a completed job run with start and end timestamps and an assigned node, When the job completes, Then the Control Server computes the integrated energy consumption (kWh) over the job duration from the smart plug metrics.
- Given computed job energy metrics, When the admin requests job execution details via the API or dashboard, Then the total energy consumed (kWh) and average power draw (W) are returned in the job report.

## Notes
Enables transparent cost and carbon footprint attribution for AI workloads running on idle hardware.
- Dependencies: ['EP-0006', 'US-0005', 'US-0011']

## Test Approach


### Tasks
- Implement energy integration calculator (trapezoidal rule over wattage timeseries)
- Store job energy summary in JobResult entity
- Expose GET /api/jobs/{id}/energy endpoint in Control Server
- Add unit test validating energy calculation against known wattage traces
