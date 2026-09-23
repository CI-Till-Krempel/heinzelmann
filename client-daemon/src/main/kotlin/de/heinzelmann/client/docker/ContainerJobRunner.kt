package de.heinzelmann.client.docker

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

interface JobResultReporter {
    fun reportResult(jobId: String, result: ContainerExecutionResult): Boolean
}

class ContainerJobRunner(
    private val dockerClient: DockerEngineClient,
    private val resultReporter: JobResultReporter? = null,
    private val powerAssertionHook: ((Boolean) -> Unit)? = null
) {
    private val logger = LoggerFactory.getLogger(ContainerJobRunner::class.java)
    private val runningJobs = ConcurrentHashMap<String, ContainerRunSpec>()
    private val isExecuting = AtomicBoolean(false)

    fun isBusy(): Boolean = runningJobs.isNotEmpty()

    fun executeJob(spec: ContainerRunSpec): ContainerExecutionResult {
        logger.info("Starting container execution for job: ${spec.jobId} (${spec.image})")
        runningJobs[spec.jobId] = spec

        // Acquire sleep-prevention power assertion while running job
        powerAssertionHook?.invoke(true)

        val result = try {
            dockerClient.runContainer(spec)
        } finally {
            runningJobs.remove(spec.jobId)
            // Release power assertion when no jobs are executing
            if (runningJobs.isEmpty()) {
                powerAssertionHook?.invoke(false)
            }
        }

        logger.info("Job ${spec.jobId} finished with exit code ${result.exitCode} in ${result.durationMs}ms")

        // Report result back to Control Server if reporter configured
        resultReporter?.let { reporter ->
            val reported = reporter.reportResult(spec.jobId, result)
            if (!reported) {
                logger.warn("Failed to report job ${spec.jobId} result to Control Server")
            }
        }

        return result
    }

    fun cancelJob(jobId: String): Boolean {
        val spec = runningJobs[jobId] ?: return false
        val containerName = spec.containerName ?: "job-$jobId"
        return dockerClient.stopContainer(containerName)
    }

    fun getActiveJobIds(): List<String> = runningJobs.keys().toList()
}
