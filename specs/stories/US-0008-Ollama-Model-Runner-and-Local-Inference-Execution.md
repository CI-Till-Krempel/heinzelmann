# User Story

- Story ID: US-0008
- Title: Ollama Model Runner and Local Inference Execution
- Status: Implemented
- Priority: Must
- Owner: Scrum Team
- Last Updated: 2026-09-23

## As a developer running LLM coding agents, I want client nodes to pull designated Ollama models and execute local inference workloads, so that code analysis tasks run directly on idle client hardware.

## Acceptance Criteria
- Given a client daemon with Docker support, When instructed to prepare an Ollama model, Then it ensures the Ollama container is running and initiates pulling the target model tag.
- Given a loaded Ollama model, When an inference prompt request is forwarded to the client daemon, Then the local Ollama API generates the completion response and returns metrics (tokens/sec, total tokens).

## Notes
Client nodes can execute LLM coding queries locally, eliminating central cloud inference costs.
- Dependencies: ['EP-0004', 'US-0004']

## Test Approach
Unit tests validating JSON serialization, API error handling, and metrics calculation.

### Tasks
- Define Ollama models, request/response DTOs, and configuration
- Implement OllamaManager to pull models and execute inference requests
- Calculate and return performance metrics (tokens/sec, total tokens)
- Add unit tests with simulated Ollama HTTP API responses
