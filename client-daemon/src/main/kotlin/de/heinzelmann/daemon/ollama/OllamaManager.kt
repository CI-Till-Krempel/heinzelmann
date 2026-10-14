package de.heinzelmann.daemon.ollama

import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Interface to execute container actions for Ollama service.
 */
interface DockerServiceController {
    fun isContainerRunning(containerName: String): Boolean
    fun startOllamaContainer(config: OllamaConfig): String?
    fun stopOllamaContainer(containerName: String): Boolean
}

/**
 * Interface for Ollama HTTP API communication.
 */
interface OllamaApiClient {
    fun pullModel(config: OllamaConfig, modelName: String): OllamaPullResponse
    fun generateCompletion(config: OllamaConfig, request: OllamaGenerateRequest): OllamaGenerateResponse
    fun listLocalModels(config: OllamaConfig): List<String>
}

/**
 * Default process-based Docker controller.
 */
class ProcessDockerServiceController : DockerServiceController {
    override fun isContainerRunning(containerName: String): Boolean {
        return try {
            val process = ProcessBuilder("docker", "ps", "--filter", "name=$containerName", "--format", "{{.Names}}")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor() == 0 && output.contains(containerName)
        } catch (_: Exception) {
            false
        }
    }

    override fun startOllamaContainer(config: OllamaConfig): String? {
        return try {
            val process = ProcessBuilder(
                "docker", "run", "-d",
                "--name", config.containerName,
                "-p", "${config.port}:11434",
                "-v", "ollama_data:/root/.ollama",
                "--restart", "unless-stopped",
                config.dockerImage
            ).redirectErrorStream(true).start()
            val containerId = process.inputStream.bufferedReader().readText().trim()
            if (process.waitFor() == 0) containerId else null
        } catch (_: Exception) {
            null
        }
    }

    override fun stopOllamaContainer(containerName: String): Boolean {
        return try {
            val process = ProcessBuilder("docker", "stop", containerName).start()
            process.waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * Standard HTTP API client communicating with Ollama REST API.
 */
class HttpOllamaApiClient : OllamaApiClient {

    override fun pullModel(config: OllamaConfig, modelName: String): OllamaPullResponse {
        val endpoint = "${config.baseUrl}/api/pull"
        val payload = """{"name":"$modelName","stream":false}"""
        val responseBody = postJson(endpoint, payload, config.pullTimeoutSeconds * 1000)

        val isSuccess = responseBody.contains("\"status\":\"success\"") || responseBody.contains("\"status\":\"downloading\"")
        val status = if (isSuccess) "success" else "pulling"
        return OllamaPullResponse(status = status)
    }

    override fun generateCompletion(config: OllamaConfig, request: OllamaGenerateRequest): OllamaGenerateResponse {
        val endpoint = "${config.baseUrl}/api/generate"
        val escapedPrompt = escapeJson(request.prompt)
        val payload = """{"model":"${request.model}","prompt":"$escapedPrompt","stream":false}"""
        val responseBody = postJson(endpoint, payload, config.requestTimeoutSeconds * 1000)

        return parseGenerateResponse(request.model, responseBody)
    }

    override fun listLocalModels(config: OllamaConfig): List<String> {
        val endpoint = "${config.baseUrl}/api/tags"
        return try {
            val responseBody = getJson(endpoint, 10000)
            val regex = """"name"\s*:\s*"([^"]+)"""".toRegex()
            regex.findAll(responseBody).map { it.groupValues[1] }.toList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun postJson(urlString: String, json: String, timeoutMs: Long): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 10000
        conn.readTimeout = timeoutMs.toInt()
        conn.doOutput = true

        conn.outputStream.use { os ->
            os.write(json.toByteArray(StandardCharsets.UTF_8))
            os.flush()
        }

        val code = conn.responseCode
        val isStream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = isStream?.bufferedReader(StandardCharsets.UTF_8)?.readText() ?: ""
        if (code !in 200..299) {
            throw RuntimeException("Ollama HTTP request failed with code $code: $response")
        }
        return response
    }

    private fun getJson(urlString: String, timeoutMs: Int): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 5000
        conn.readTimeout = timeoutMs
        val code = conn.responseCode
        if (code !in 200..299) {
            throw RuntimeException("Ollama HTTP GET failed with code $code")
        }
        return conn.inputStream.bufferedReader(StandardCharsets.UTF_8).readText()
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun parseGenerateResponse(model: String, json: String): OllamaGenerateResponse {
        val responseText = extractJsonField(json, "response") ?: ""
        val done = json.contains("\"done\":true") || json.contains("\"done\": true")
        val totalDuration = extractJsonLong(json, "total_duration")
        val loadDuration = extractJsonLong(json, "load_duration")
        val promptEvalCount = extractJsonInt(json, "prompt_eval_count")
        val promptEvalDuration = extractJsonLong(json, "prompt_eval_duration")
        val evalCount = extractJsonInt(json, "eval_count")
        val evalDuration = extractJsonLong(json, "eval_duration")

        return OllamaGenerateResponse(
            model = model,
            response = responseText,
            done = done,
            total_duration = totalDuration,
            load_duration = loadDuration,
            prompt_eval_count = promptEvalCount,
            prompt_eval_duration = promptEvalDuration,
            eval_count = evalCount,
            eval_duration = evalDuration
        )
    }

    private fun extractJsonField(json: String, key: String): String? {
        val regex = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        return regex.find(json)?.groupValues?.get(1)
    }

    private fun extractJsonLong(json: String, key: String): Long? {
        val regex = "\"$key\"\\s*:\\s*(\\d+)".toRegex()
        return regex.find(json)?.groupValues?.get(1)?.toLongOrNull()
    }

    private fun extractJsonInt(json: String, key: String): Int? {
        val regex = "\"$key\"\\s*:\\s*(\\d+)".toRegex()
        return regex.find(json)?.groupValues?.get(1)?.toIntOrNull()
    }
}

/**
 * Core manager orchestrating Ollama container lifecycle, model caching,
 * inference execution, and latency/token throughput performance metrics.
 */
class OllamaManager(
    val config: OllamaConfig = OllamaConfig(),
    private val dockerController: DockerServiceController = ProcessDockerServiceController(),
    private val apiClient: OllamaApiClient = HttpOllamaApiClient()
) {

    /**
     * Verifies that the Ollama container is active, starting it if not running.
     */
    fun ensureContainerRunning(): OllamaContainerStatus {
        val isRunning = dockerController.isContainerRunning(config.containerName)
        val containerId = if (!isRunning) {
            dockerController.startOllamaContainer(config)
        } else {
            config.containerName
        }

        val models = if (isRunning || containerId != null) {
            try {
                apiClient.listLocalModels(config)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        return OllamaContainerStatus(
            isRunning = isRunning || containerId != null,
            containerId = containerId,
            host = config.host,
            port = config.port,
            loadedModels = models
        )
    }

    /**
     * Ensures container is running and initiates pulling target model tag.
     */
    fun prepareModel(modelTag: String): OllamaPullResponse {
        val status = ensureContainerRunning()
        if (!status.isRunning) {
            return OllamaPullResponse(status = "error: container could not be started")
        }
        return apiClient.pullModel(config, modelTag)
    }

    /**
     * Executes an inference prompt against the local Ollama API,
     * calculating prompt tokens, completion tokens, duration, and tokens per second throughput.
     */
    fun executeInference(model: String, prompt: String, systemPrompt: String? = null): OllamaInferenceResult {
        val startTime = System.currentTimeMillis()
        return try {
            val response = apiClient.generateCompletion(
                config = config,
                request = OllamaGenerateRequest(
                    model = model,
                    prompt = prompt,
                    system = systemPrompt
                )
            )

            val totalDurationMs = if (response.total_duration != null && response.total_duration > 0) {
                response.total_duration / 1_000_000.0
            } else {
                (System.currentTimeMillis() - startTime).toDouble().coerceAtLeast(1.0)
            }

            val evalDurationMs = if (response.eval_duration != null && response.eval_duration > 0) {
                response.eval_duration / 1_000_000.0
            } else {
                totalDurationMs
            }

            val promptTokens = response.prompt_eval_count ?: (prompt.length / 4).coerceAtLeast(1)
            val completionTokens = response.eval_count ?: (response.response.length / 4).coerceAtLeast(1)
            val totalTokens = promptTokens + completionTokens

            val tokensPerSec = if (evalDurationMs > 0) {
                (completionTokens / (evalDurationMs / 1000.0))
            } else {
                0.0
            }

            OllamaInferenceResult(
                model = model,
                prompt = prompt,
                completionText = response.response,
                promptTokens = promptTokens,
                completionTokens = completionTokens,
                totalTokens = totalTokens,
                totalDurationMs = totalDurationMs,
                evalDurationMs = evalDurationMs,
                tokensPerSecond = Math.round(tokensPerSec * 100.0) / 100.0,
                success = response.done
            )
        } catch (e: Exception) {
            val elapsed = (System.currentTimeMillis() - startTime).toDouble()
            OllamaInferenceResult(
                model = model,
                prompt = prompt,
                completionText = "",
                promptTokens = 0,
                completionTokens = 0,
                totalTokens = 0,
                totalDurationMs = elapsed,
                evalDurationMs = 0.0,
                tokensPerSecond = 0.0,
                success = false,
                error = e.message ?: "Inference execution failed"
            )
        }
    }
}
