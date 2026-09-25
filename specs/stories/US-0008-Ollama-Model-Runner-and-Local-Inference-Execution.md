# User Story

- Story ID: US-0008
- Title: Ollama Model Runner and Local Inference Execution
- Status: Ready
- Priority: Must
- Owner: Scrum Team
- Last Updated: 2026-09-25

## As a developer running LLM coding agents, I want client nodes to pull designated Ollama models and execute local inference workloads, so that code analysis tasks run directly on idle client hardware.

## Acceptance Criteria
- Given a client daemon with Docker support, When instructed to prepare an Ollama model, Then it ensures the Ollama container is running and initiates pulling the target model tag.
- Given a loaded Ollama model, When an inference prompt request is forwarded to the client daemon, Then the local Ollama API generates the completion response and returns metrics (tokens/sec, total tokens).

## Notes


## Test Approach
Unit tests with simulated HTTP client responses validating completion parsing and metrics calculation.

### Tasks
- Define Ollama request/response data models and metrics DTOs
- Implement OllamaManager with container lifecycle check, model pull, and completion inference methods
- Compute tokens/sec and total duration metrics on completion response
- Write unit tests covering model pull, inference generation, and metrics calculation
