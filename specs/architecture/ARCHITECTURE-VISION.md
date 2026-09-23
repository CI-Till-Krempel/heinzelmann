# Architecture Vision: Heinzelmann Distributed LLM & Compute Cluster

## System Overview & Target Shape
Heinzelmann is a distributed, energy-aware compute cluster leveraging idle laptop hardware (macOS Apple Silicon & Windows x86/ARM) for batch job execution and local LLM inference (e.g. StarCoder 2, CodeLlama, Llama 3 coding agents).

The system consists of three primary components:
1. **Control Server (Ktor / Kotlin Multiplatform Backend)**:
   - Central coordinator maintaining real-time node registry, health/heartbeat telemetry, and job queue.
   - Intelligent scheduler distributing containerized batch tasks and LLM inference workloads based on node capacity, battery level, and thermal headroom.
   - Integrates with external smart plugs (Shelly, Tapo) for physical energy consumption tracking.
2. **Client Daemons (macOS LaunchDaemon & Windows Service)**:
   - Ultra-low footprint background agent (<1% CPU, <50MB RAM idle) running on participant laptops.
   - Periodic telemetry sampler (CPU, GPU, RAM, battery, thermal status).
   - Manages OS-level Power Assertions (`IOPMAssertion` on macOS, `SetThreadExecutionState` on Windows) to prevent standby during active overnight jobs.
   - Orchestrates isolated Docker container execution for jobs and Ray/Ollama workers.
3. **Admin Dashboard (Compose for Desktop)**:
   - Cross-platform desktop interface for cluster administrators.
   - Real-time fleet visibility (online/offline status, resource metrics).
   - Interactive job submission, pipeline tracking, and scheduling policy configuration.

## Key Quality Attributes
- **Zero Daytime Disruption**: Daemons operate silently and yield resources immediately during working hours or battery discharge.
- **Hardware Safety**: Strict thermal limits (<85°C) and battery minimums (>50%) trigger graceful job pausing and sleep policy restoration.
- **Reproducibility & Isolation**: All workloads execute inside Docker containers with enforced memory and CPU limits.
- **Cross-Platform Consistency**: Single Kotlin Multiplatform codebase maximizes shared logic across server, desktop, and daemons.
- **Security**: Mutual TLS (mTLS) for all control plane communication and token-authenticated API endpoints.

## Tech Stack Guardrails
- **Language & Runtime**: Kotlin Multiplatform (JVM for Server & Admin UI, Native/JVM for Daemons).
- **Control Server**: Ktor HTTP/WebSocket engine with kotlinx.serialization and Exposed/SQLite persistence.
- **Client Daemons**: KMP Native/JVM daemon integrating with platform power APIs and Docker daemon socket.
- **Inference & Compute**: Docker engine running Ray cluster workers and local Ollama model instances.
- **Desktop UI**: Jetpack Compose for Desktop with Coroutines/StateFlow reactive state.
