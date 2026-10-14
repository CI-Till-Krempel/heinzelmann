package de.heinzelmann.daemon.ollama

import kotlinx.serialization.Serializable

/**
 * Configuration for Ollama container and HTTP service.
 */
@Serializable
data class OllamaConfig(
    val host: String = "localhost",
    val port: Int = 11434,
    val containerName: String = "ollama-service",
    val dockerImage: String = "ollama/ollama:latest",
    val defaultModel: String = "qwen2.5-coder:7b",
    val pullTimeoutSeconds: Long = 600,
    val requestTimeoutSeconds: Long = 120
) {
    val baseUrl: String get() = "http://$host:$port"
}

/**
 * Model pull request payload.
 */
@Serializable
data class OllamaPullRequest(
    val name: String,
    val insecure: Boolean = false,
    val stream: Boolean = false
)

/**
 * Status returned during or after pulling a model.
 */
@Serializable
data class OllamaPullResponse(
    val status: String,
    val digest: String? = null,
    val total: Long? = null,
    val completed: Long? = null
)

/**
 * Request payload for prompt completion.
 */
@Serializable
data class OllamaGenerateRequest(
    val model: String,
    val prompt: String,
    val system: String? = null,
    val stream: Boolean = false,
    val options: Map<String, String>? = null
)

/**
 * Response payload from Ollama generate API.
 */
@Serializable
data class OllamaGenerateResponse(
    val model: String,
    val response: String,
    val done: Boolean,
    val total_duration: Long? = null,
    val load_duration: Long? = null,
    val prompt_eval_count: Int? = null,
    val prompt_eval_duration: Long? = null,
    val eval_count: Int? = null,
    val eval_duration: Long? = null
)

/**
 * Structured inference result with calculated throughput metrics.
 */
@Serializable
data class OllamaInferenceResult(
    val model: String,
    val prompt: String,
    val completionText: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val totalDurationMs: Double,
    val evalDurationMs: Double,
    val tokensPerSecond: Double,
    val success: Boolean,
    val error: String? = null
)

/**
 * Ollama container runtime state.
 */
@Serializable
data class OllamaContainerStatus(
    val isRunning: Boolean,
    val containerId: String?,
    val host: String,
    val port: Int,
    val loadedModels: List<String> = emptyList()
)
