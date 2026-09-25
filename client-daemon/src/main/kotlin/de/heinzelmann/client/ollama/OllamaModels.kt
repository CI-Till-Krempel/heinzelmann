package de.heinzelmann.client.ollama

import kotlinx.serialization.Serializable

@Serializable
data class OllamaContainerConfig(
    val containerName: String = "heinzelmann-ollama",
    val image: String = "ollama/ollama:latest",
    val hostPort: Int = 11434,
    val containerPort: Int = 11434,
    val gpuEnabled: Boolean = true,
    val volumePath: String? = null
)

@Serializable
data class OllamaModelRequest(
    val model: String,
    val prompt: String,
    val systemPrompt: String? = null,
    val stream: Boolean = false,
    val temperature: Double? = null,
    val maxTokens: Int? = null
)

@Serializable
data class OllamaInferenceResult(
    val model: String,
    val response: String,
    val totalTokens: Int,
    val promptTokens: Int,
    val completionTokens: Int,
    val evalDurationNs: Long,
    val tokensPerSecond: Double,
    val successful: Boolean,
    val error: String? = null
)

@Serializable
data class OllamaModelTag(
    val name: String,
    val tag: String,
    val sizeBytes: Long = 0L,
    val digest: String = "",
    val isReady: Boolean = true
)
