package de.heinzelmann.client.docker

import org.slf4j.LoggerFactory
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

interface DockerEngineClient {
    fun checkEngineStatus(): DockerEngineStatus
    fun runContainer(spec: ContainerRunSpec): ContainerExecutionResult
    fun stopContainer(containerId: String): Boolean
}

open class DefaultDockerEngineClient(
    private val dockerExecutable: String = "docker",
    private val processBuilderFactory: (List<String>) -> ProcessBuilder = { ProcessBuilder(it) }
) : DockerEngineClient {

    private val logger = LoggerFactory.getLogger(DefaultDockerEngineClient::class.java)

    val defaultSocketPath: String by lazy {
        val os = System.getProperty("os.name", "").lowercase()
        if (os.contains("win")) {
            "//./pipe/docker_engine"
        } else {
            "/var/run/docker.sock"
        }
    }

    override fun checkEngineStatus(): DockerEngineStatus {
        return try {
            val pb = processBuilderFactory(listOf(dockerExecutable, "info", "--format", "{{.ServerVersion}}"))
            val process = pb.start()
            val finished = process.waitFor(5, TimeUnit.SECONDS)
            if (finished && process.exitValue() == 0) {
                val version = process.inputStream.bufferedReader().readText().trim()
                DockerEngineStatus(available = true, version = version, socketPath = defaultSocketPath)
            } else {
                DockerEngineStatus(
                    available = false,
                    socketPath = defaultSocketPath,
                    errorMessage = "Docker engine check failed with exit code ${process.exitValue()}"
                )
            }
        } catch (e: Exception) {
            logger.warn("Docker engine is not available: ${e.message}")
            DockerEngineStatus(available = false, socketPath = defaultSocketPath, errorMessage = e.message)
        }
    }

    override fun runContainer(spec: ContainerRunSpec): ContainerExecutionResult {
        val containerName = spec.containerName ?: "job-${spec.jobId}-${UUID.randomUUID().toString().take(8)}"
        val startTime = System.currentTimeMillis()

        val cmd = mutableListOf(
            dockerExecutable, "run",
            "--rm",
            "--name", containerName,
            "--cpus", spec.cpuLimit.toString(),
            "--memory", "${spec.memoryLimitMb}m"
        )

        for ((k, v) in spec.environment) {
            cmd.add("-e")
            cmd.add("$k=$v")
        }

        cmd.add(spec.image)
        cmd.addAll(spec.command)

        logger.info("Executing container for job ${spec.jobId} with limits: ${spec.cpuLimit} CPUs, ${spec.memoryLimitMb}MB")

        return try {
            val pb = processBuilderFactory(cmd)
            val process = pb.start()

            val stdoutFuture = java.util.concurrent.CompletableFuture.supplyAsync {
                process.inputStream.bufferedReader().readText()
            }
            val stderrFuture = java.util.concurrent.CompletableFuture.supplyAsync {
                process.errorStream.bufferedReader().readText()
            }

            val completed = process.waitFor(spec.timeoutSeconds, TimeUnit.SECONDS)
            val duration = System.currentTimeMillis() - startTime

            if (!completed) {
                process.destroyForcibly()
                stopContainer(containerName)
                ContainerExecutionResult(
                    jobId = spec.jobId,
                    containerId = containerName,
                    exitCode = 124, // Timeout standard exit code
                    stdout = runCatching { stdoutFuture.get(500, TimeUnit.MILLISECONDS) }.getOrDefault(""),
                    stderr = "Container execution timed out after ${spec.timeoutSeconds}s",
                    durationMs = duration
                )
            } else {
                val exitCode = process.exitValue()
                val stdout = stdoutFuture.get()
                val stderr = stderrFuture.get()
                ContainerExecutionResult(
                    jobId = spec.jobId,
                    containerId = containerName,
                    exitCode = exitCode,
                    stdout = stdout,
                    stderr = stderr,
                    durationMs = duration
                )
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            logger.error("Failed to run container for job ${spec.jobId}", e)
            ContainerExecutionResult(
                jobId = spec.jobId,
                containerId = containerName,
                exitCode = 1,
                stdout = "",
                stderr = "Execution exception: ${e.message}",
                durationMs = duration
            )
        }
    }

    override fun stopContainer(containerId: String): Boolean {
        return try {
            val pb = processBuilderFactory(listOf(dockerExecutable, "stop", "-t", "5", containerId))
            val process = pb.start()
            val finished = process.waitFor(10, TimeUnit.SECONDS)
            finished && process.exitValue() == 0
        } catch (e: Exception) {
            logger.warn("Failed to stop container $containerId: ${e.message}")
            false
        }
    }
}
