# Architecture Decision Record (ADR)

- ADR-ID: ADR-0001
- Title: Adopt Kotlin Multiplatform (KMP) for Cross-Platform Daemons and Shared Logic
- Status: Accepted
- Date: 2026-09-23
- Owners: Architect

## Context
Heinzelmann requires cross-platform services: a daemon running as a low-overhead background service on macOS (Apple Silicon/Intel) and Windows, a central backend control server, and an administrator desktop GUI. Maintaining disparate codebases (e.g. Swift + C# + Go) would cause high maintenance overhead and divergence in telemetry models.

## Decision
Adopt Kotlin Multiplatform (KMP) across all system tiers: JVM for the Ktor Control Server and Compose for Desktop Admin UI, and Kotlin/Native (or lightweight JVM) for the client background daemons.

## Options Considered
- Option A — pros/cons
- Option B — pros/cons
- Option C — pros/cons

## Consequences
Positive: High code reuse (>70%) across daemon logic, networking models, and shared telemetry serialization. Consistent tooling with Gradle and Kotlin ecosystem. Native binaries for low-footprint background services on macOS and Windows.
Negative: Kotlin/Native ecosystem has specific concurrency rules and interoperability nuances with platform C APIs (IOPM / Win32). Requires dedicated platform bindings.

## References
- Links to related PRs, stories, requirements
- Diagrams or documents
