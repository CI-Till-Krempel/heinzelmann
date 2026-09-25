package de.heinzelmann.client.ollama

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface OllamaContainerService {
    fun isContainerRunning(containerName: String): Boolean
    fun startContainer(config: OllamaContainerConfig): String
    fun stopContainer(containerName: String): Boolean
}

interface OllamaApiClient {
    suspend fun listModels(): List<OllamaModelTag>
    suspend fun pullModel(modelTag: String): Boolean
    suspend fun generate(request: OllamaModelRequest): OllamaInferenceResult
}

class DefaultOllamaContainerService : OllamaContainerService {
    private val runningContainers = mutableMapOf<String, Boolean>()

    override fun isContainerRunning(containerName: String): Boolean {
        return runningContainers[containerName] == true
    }

    override fun startContainer(config: OllamaContainerConfig): String {
        runningContainers[config.containerName] = true
        return "cid-${config.containerName}"
    }

    override fun stopContainer(containerName: String): Boolean {
        return runningContainers.remove(containerName) != null
    }
}

class OllamaManager(
    private val containerConfig: OllamaContainerConfig = OllamaContainerConfig(),
    private val containerService: OllamaContainerService = DefaultOllamaContainerService(),
    private val apiClient: OllamaApiClient
) {
    private val mutex = Mutex()
    private val loadedModels = mutableSetOf<String>()

    fun isServiceReady(): Boolean {
        return containerService.isContainerRunning(containerConfig.containerName)
    }

    suspend fun ensureOllamaRunning(): Boolean = mutex.withLock {
        if (!containerService.isContainerRunning(containerConfig.containerName)) {
            val cid = containerService.startContainer(containerConfig)
            return cid.isNotBlank()
        }
        return true
    }

    suspend fun prepareModel(modelTag: String): Result<Boolean> {
        val running = ensureOllamaRunning()
        if (!running) {
            return Result.failure(IllegalStateException("Failed to start Ollama container ${containerConfig.containerName}"))
        }

        return try {
            val success = apiClient.pullModel(modelTag)
            if (success) {
                mutex.withLock { loadedModels.add(modelTag) }
                Result.success(true)
            } else {
                Result.failure(RuntimeException("Ollama API failed to pull model $modelTag"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun executeInference(request: OllamaModelRequest): Result<OllamaInferenceResult> {
        val running = ensureOllamaRunning()
        if (!running) {
            return Result.failure(IllegalStateException("Ollama container is not active"))
        }

        return try {
            val result = apiClient.generate(request)
            if (result.successful) {
                mutex.withLock { loadedModels.add(request.model) }
                Result.success(result)
            } else {
                Result.failure(RuntimeException(result.error ?: "Ollama inference execution failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listAvailableModels(): List<OllamaModelTag> {
        if (!isServiceReady()) return emptyList()
        return try {
            apiClient.listModels()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
