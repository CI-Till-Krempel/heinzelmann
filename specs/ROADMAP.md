# Product Roadmap

Use this living roadmap to plan releases and track user stories across states. It doubles as a lightweight task board and a release planning tool.

## How to use
- Story IDs should match files in `specs/stories/` (e.g., `ST-001` corresponds to `specs/stories/ST-001-some-title.md`).
- Move story references between states as work progresses.
- Keep titles short; full details live in the story file.
- For each planned version, list goals and the set of stories targeted for that release.
- When a release is cut, freeze the section by adding the actual tag (e.g., `v0.1.0`) and dates.

Legend
- `[ST-###] Title` → a user story reference and its short title
- Checkbox states: `- [ ]` To Do, `- [~]` In Progress (use `- [~]` to signal WIP), `- [R]` In Review, `- [x]` Done

Tip: If you prefer standard checkboxes only, use the Kanban tables below and keep raw lists unchecked.

---

## Release plan (versions → stories)

### v0.1 — MVP (target: YYYY-MM)
Goals
- Establish Ktor Control Server with node registration and heartbeat tracking
- Deliver lightweight background Client Daemon for macOS and Windows
- Implement Power Assertion / keep-awake to prevent sleep during active jobs
- Enable isolated Docker container job execution and central job dispatching
Stories
- [US-0001] Control Server Node Registration & Heartbeat API
  - [x] DRAFT
  - [x] READY
  - [x] IMPLEMENTED
  - [x] REVIEWED
  - [x] TESTED
  - [x] ACCEPTED
- [US-0004] Docker Container Job Runner on Client Daemon
  - [ ] DRAFT
  - [ ] READY
  - [ ] IMPLEMENTED
  - [ ] REVIEWED
  - [ ] TESTED
  - [ ] ACCEPTED
- [US-0002] Client Daemon Lifecycle & Metrics Collector
  - [x] DRAFT
  - [x] READY
  - [ ] IMPLEMENTED
  - [ ] REVIEWED
  - [ ] TESTED
  - [ ] ACCEPTED
- [US-0003] Client Daemon Power Assertion and Sleep Prevention
  - [ ] DRAFT
  - [ ] READY
  - [ ] IMPLEMENTED
  - [ ] REVIEWED
  - [ ] TESTED
  - [ ] ACCEPTED
- [US-0005] Control Server Job Queue and Dispatcher
  - [ ] DRAFT
  - [ ] READY
  - [ ] IMPLEMENTED
  - [ ] REVIEWED
  - [ ] TESTED
  - [ ] ACCEPTED

### v0.2 — Next iteration (target: YYYY-MM)
Goals
- Integrate Ray and Ollama for distributed LLM coding agent inference
- Provide Compose for Desktop Admin Dashboard for live monitoring, job submission, and pipeline tracking
- Define and enforce scheduling policies (night-time windows, battery and thermal limits)
- Integrate smart plug telemetry (Shelly/Tapo) and per-job energy consumption reporting
Stories
- [US-0006] Admin Dashboard Node Status and Live Metrics View
  - [ ] DRAFT
  - [ ] READY
  - [ ] IMPLEMENTED
  - [ ] REVIEWED
  - [ ] TESTED
  - [ ] ACCEPTED
- [US-0007] Distributed Ray Cluster Bootstrap on Client Nodes
  - [x] DRAFT
  - [x] READY
  - [ ] IMPLEMENTED
  - [ ] REVIEWED
  - [ ] TESTED
  - [ ] ACCEPTED
- [US-0008] Ollama Model Runner and Local Inference Execution
  - [x] DRAFT
  - [x] READY
  - [ ] IMPLEMENTED
  - [ ] REVIEWED
  - [ ] TESTED
  - [ ] ACCEPTED
- [US-0009] Admin Dashboard Job Submission and Status Pipeline View
  - [x] DRAFT
  - [x] READY
  - [ ] IMPLEMENTED
  - [ ] REVIEWED
  - [ ] TESTED
  - [ ] ACCEPTED
- [US-0010] Night-Time Scheduling Windows and Thermal Guard Policy
  - [x] DRAFT
  - [x] READY
  - [ ] IMPLEMENTED
  - [ ] REVIEWED
  - [ ] TESTED
  - [ ] ACCEPTED
- [US-0011] Smart Plug Telemetry Collector for Shelly and Tapo
  - [x] DRAFT
  - [x] READY
  - [ ] IMPLEMENTED
  - [ ] REVIEWED
  - [ ] TESTED
  - [ ] ACCEPTED
- [US-0012] Energy Consumption Correlation and Reporting per Job
  - [x] DRAFT
  - [x] READY
  - [ ] IMPLEMENTED
  - [ ] REVIEWED
  - [ ] TESTED
  - [ ] ACCEPTED

### Backlog (unplanned)


Stories
- [ISSUE-0002] Token budget (1,000,000 tokens) was exhausted during initial backlog refinement and architecture modeling before Sprint 1 stories could be estimated or advanced.
  - [ ] DRAFT
  - [ ] READY
  - [ ] IMPLEMENTED
  - [ ] REVIEWED
  - [ ] TESTED
  - [ ] ACCEPTED
- [ISSUE-0001] test
  - [ ] DRAFT
  - [ ] READY
  - [ ] IMPLEMENTED
  - [ ] REVIEWED
  - [ ] TESTED
  - [ ] ACCEPTED
- [ISSUE-0003] Streamline ProductOwner specification drafting and backlog grooming sessions with strict prompt timeboxes and pre-structured templates to keep PO token usage under 30% of sprint budget.
  - [ ] DRAFT
  - [ ] READY
  - [ ] IMPLEMENTED
  - [ ] REVIEWED
  - [ ] TESTED
  - [ ] ACCEPTED
- [ISSUE-0004] Token budget limit of 6,000,000 was exceeded (reaching 6,073,060 tokens) with ProductOwner accounting for over 50% of token usage and feature implementation taking only 34% of total tokens.
  - [ ] DRAFT
  - [ ] READY
  - [ ] IMPLEMENTED
  - [ ] REVIEWED
  - [ ] TESTED
  - [ ] ACCEPTED
---

## Task board (Kanban)

Use either the per-version boards below or one global board; duplicate as needed for each active version.

### v0.1 Kanban

| To Do | In Progress | In Review | Done |
|------|-------------|-----------|------|

Notes
- Update this table in PRs alongside code changes.
- Keep the board limited to the current sprint scope if you’re also running sprints.

### v0.2 Kanban

| To Do | In Progress | In Review | Done |
|------|-------------|-----------|------|

---

## Cross-cutting initiatives (optional)
Track broader themes/epics that span multiple versions. Link constituent stories.

---

## Release checklist (for when cutting a release)
- [ ] All included stories are in `Done` and meet Definition of Done
- [ ] Docs updated (stories, PRD/SRS, ADRs as needed)
- [ ] Version/tag created (e.g., `v0.1.0`) and changelog drafted
- [ ] Known issues captured and follow-ups added to backlog

---

## Index of story references
Group story references by planned version for quick scanning.

- v0.1
- v0.2
- Unplanned

Replace placeholders with your actual story IDs and titles. Keep this file updated in the same PRs that move work forward.