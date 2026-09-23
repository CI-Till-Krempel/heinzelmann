package de.heinzelmann.client.ollama

import kotlinx.coroutines.runBlocking
import kotlin.test.*

class OllamaManagerTest {

    private class MockContainerService : OllamaContainerService {
        var isRunning = false
        var startedConfigs = mutableListOf<OllamaContainerConfig>()

        override fun isContainerRunning(containerName: String): Boolean = isRunning

        override fun startContainer(config: OllamaContainerConfig): String {
            isRunning = true
            startedConfigs.add(config)
            return "mock-ollama-cid"
        }

        override fun stopContainer(containerName: String): Boolean {
            isRunning = false
            return true
        }
    }

    private class MockApiClient : OllamaApiClient {
        val pulledModels = mutableListOf<String>()
        var pullShouldSucceed = true
        var generateResult: OllamaInferenceResult? = null

        override fun listModels(): List<OllamaModelTag> {
            return pulledModels.map { OllamaModelTag(name = it, tag = "latest") }
        }

        override suspend fun pullModel(modelTag: String): Boolean {
            if (!pullShouldSucceed) return false
            pulledModels.add(modelTag)
            return true
        }

        override suspend fun generate(request: OllamaModelRequest): OllamaInferenceResult {
            return generateResult ?: OllamaInferenceResult(
                model = request.model,
                response = "def solve(): return 42",
                totalTokens = 128,
                promptTokens = 28,
                completionTokens = 100,
                evalDurationNs = 2_000_000_000L, // 2 seconds
                tokensPerSecond = 50.0,
                successful = true
            )
        }
    }

    @Test
    fun testPrepareModelStartsContainerAndPullsModel() = runBlocking {
        val containerService = MockContainerService()
        val apiClient = MockApiClient()
        val manager = OllamaManager(
            containerService = containerService,
            apiClient = apiClient
        )

        assertFalse(containerService.isRunning)

        val result = manager.prepareModel("qwen2.5-coder:7b")
        assertTrue(result.isSuccess)
        assertTrue(containerService.isRunning)
        assertTrue(apiClient.pulledModels.contains("qwen2.5-coder:7b"))
    }

    @Test
    fun testPrepareModelFailureHandling() = runBlocking {
        val containerService = MockContainerService()
        val apiClient = MockApiClient().apply { pullShouldSucceed = false }
        val manager = OllamaManager(
            containerService = containerService,
            apiClient = apiClient
        )

        val result = manager.prepareModel("invalid-model:tag")
        assertTrue(result.isFailure)
    }

    @Test
    fun testExecuteInferenceReturnsCompletionAndMetrics() = runBlocking {
        val containerService = MockContainerService().apply { isRunning = true }
        val apiClient = MockApiClient()
        val manager = OllamaManager(
            containerService = containerService,
            apiClient = apiClient
        )

        val request = OllamaModelRequest(
            model = "deepseek-coder:6.7b",
            prompt = "Write a function in Kotlin that reverses a string."
        )

        val result = manager.executeInference(request)
        assertTrue(result.isSuccess)

        val inference = result.getOrThrow()
        assertEquals("deepseek-coder:6.7b", inference.model)
        assertEquals("def solve(): return 42", inference.response)
        assertEquals(128, inference.totalTokens)
        assertEquals(50.0, inference.tokensPerSecond)
        assertTrue(inference.successful)
    }

    @Test
    fun testExecuteInferencePropagatesError() = runBlocking {
        val containerService = MockContainerService().apply { isRunning = true }
        val apiClient = MockApiClient().apply {
            generateResult = OllamaInferenceResult(
                model = "test-model",
                response = "",
                totalTokens = 0,
                promptTokens = 0,
                completionTokens = 0,
                evalDurationNs = 0L,
                tokensPerSecond = 0.0,
                successful = false,
                error = "CUDA out of memory"
            )
        }
        val manager = OllamaManager(
            containerService = containerService,
            apiClient = apiClient
        )

        val request = OllamaModelRequest(model = "test-model", prompt = "hello")
        val result = manager.executeInference(request)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("CUDA out of memory") == true)
    }
}
