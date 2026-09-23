package de.heinzelmann.client.docker

import kotlinx.serialization.Serializable

@Serializable
data class ContainerRunSpec(
    val jobId: String,
    val image: String,
    val command: List<String> = emptyList(),
    val environment: Map<String, String> = emptyMap(),
    val cpuLimit: Double = 1.0,
    val memoryLimitMb: Long = 512,
    val containerName: String? = null,
    val timeoutSeconds: Long = 3600
)

@Serializable
data class ContainerExecutionResult(
    val jobId: String,
    val containerId: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long
) {
    val isSuccess: Boolean get() = exitCode == 0
}

@Serializable
data class DockerEngineStatus(
    val available: Boolean,
    val version: String = "unknown",
    val socketPath: String = "",
    val errorMessage: String? = null
)
