package de.heinzelmann.daemon.ollama

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OllamaManagerTest {

    private class MockDockerController(
        var running: Boolean = false,
        var startReturnId: String? = "mock-ollama-container-id"
    ) : DockerServiceController {
        var startCalls = 0
        var stopCalls = 0

        override fun isContainerRunning(containerName: String): Boolean = running

        override fun startOllamaContainer(config: OllamaConfig): String? {
            startCalls++
            running = true
            return startReturnId
        }

        override fun stopOllamaContainer(containerName: String): Boolean {
            stopCalls++
            running = false
            return true
        }
    }

    private class MockApiClient(
        var pullResult: OllamaPullResponse = OllamaPullResponse(status = "success"),
        var generateResult: OllamaGenerateResponse = OllamaGenerateResponse(
            model = "qwen2.5-coder:7b",
            response = "fun helloWorld() = println(\"Hello\")",
            done = true,
            total_duration = 2_000_000_000L, // 2000 ms
            eval_duration = 1_000_000_000L,  // 1000 ms
            prompt_eval_count = 15,
            eval_count = 30
        ),
        var localModels: List<String> = listOf("qwen2.5-coder:7b", "llama3.2:3b"),
        var throwOnGenerate: Boolean = false
    ) : OllamaApiClient {
        var lastPulledModel: String? = null
        var lastGenerateRequest: OllamaGenerateRequest? = null

        override fun pullModel(config: OllamaConfig, modelName: String): OllamaPullResponse {
            lastPulledModel = modelName
            return pullResult
        }

        override fun generateCompletion(config: OllamaConfig, request: OllamaGenerateRequest): OllamaGenerateResponse {
            if (throwOnGenerate) throw RuntimeException("Network timeout during inference")
            lastGenerateRequest = request
            return generateResult
        }

        override fun listLocalModels(config: OllamaConfig): List<String> = localModels
    }

    @Test
    fun testEnsureContainerRunningStartsWhenNotRunning() {
        val docker = MockDockerController(running = false)
        val api = MockApiClient()
        val manager = OllamaManager(
            config = OllamaConfig(containerName = "test-ollama"),
            dockerController = docker,
            apiClient = api
        )

        val status = manager.ensureContainerRunning()

        assertTrue(status.isRunning)
        assertEquals("mock-ollama-container-id", status.containerId)
        assertEquals(1, docker.startCalls)
        assertEquals(listOf("qwen2.5-coder:7b", "llama3.2:3b"), status.loadedModels)
    }

    @Test
    fun testEnsureContainerRunningAlreadyRunning() {
        val docker = MockDockerController(running = true)
        val api = MockApiClient()
        val manager = OllamaManager(
            config = OllamaConfig(containerName = "test-ollama"),
            dockerController = docker,
            apiClient = api
        )

        val status = manager.ensureContainerRunning()

        assertTrue(status.isRunning)
        assertEquals(0, docker.startCalls)
    }

    @Test
    fun testPrepareModelPullsTargetTag() {
        val docker = MockDockerController(running = true)
        val api = MockApiClient()
        val manager = OllamaManager(dockerController = docker, apiClient = api)

        val result = manager.prepareModel("starcoder2:7b")

        assertEquals("success", result.status)
        assertEquals("starcoder2:7b", api.lastPulledModel)
    }

    @Test
    fun testExecuteInferenceComputesMetrics() {
        val docker = MockDockerController(running = true)
        val api = MockApiClient()
        val manager = OllamaManager(dockerController = docker, apiClient = api)

        val result = manager.executeInference(
            model = "qwen2.5-coder:7b",
            prompt = "Write a hello world function in Kotlin"
        )

        assertTrue(result.success)
        assertEquals("fun helloWorld() = println(\"Hello\")", result.completionText)
        assertEquals(15, result.promptTokens)
        assertEquals(30, result.completionTokens)
        assertEquals(45, result.totalTokens)
        assertEquals(2000.0, result.totalDurationMs)
        assertEquals(1000.0, result.evalDurationMs)
        // 30 tokens in 1 second (1000ms) = 30.0 tokens/sec
        assertEquals(30.0, result.tokensPerSecond)
    }

    @Test
    fun testExecuteInferenceHandlesFailureGracefully() {
        val docker = MockDockerController(running = true)
        val api = MockApiClient(throwOnGenerate = true)
        val manager = OllamaManager(dockerController = docker, apiClient = api)

        val result = manager.executeInference(
            model = "qwen2.5-coder:7b",
            prompt = "Failing prompt"
        )

        assertFalse(result.success)
        assertEquals(0, result.totalTokens)
        assertEquals("Network timeout during inference", result.error)
    }
}
