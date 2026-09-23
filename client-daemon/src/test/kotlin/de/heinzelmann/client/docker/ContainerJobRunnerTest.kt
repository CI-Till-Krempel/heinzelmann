package de.heinzelmann.client.docker

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class ContainerJobRunnerTest {

    private class MockDockerEngineClient(
        var simulatedExitCode: Int = 0,
        var simulatedStdout: String = "All tests passed",
        var simulatedStderr: String = "",
        var shouldTimeout: Boolean = false
    ) : DockerEngineClient {
        val executedCommands = mutableListOf<List<String>>()
        val stoppedContainers = mutableListOf<String>()

        override fun checkEngineStatus(): DockerEngineStatus {
            return DockerEngineStatus(available = true, version = "24.0.7", socketPath = "/var/run/docker.sock")
        }

        override fun runContainer(spec: ContainerRunSpec): ContainerExecutionResult {
            executedCommands.add(listOf(
                "docker", "run", "--cpus", spec.cpuLimit.toString(),
                "--memory", "${spec.memoryLimitMb}m", spec.image
            ) + spec.command)

            val exitCode = if (shouldTimeout) 124 else simulatedExitCode
            val stderr = if (shouldTimeout) "Execution timed out" else simulatedStderr

            return ContainerExecutionResult(
                jobId = spec.jobId,
                containerId = spec.containerName ?: "job-${spec.jobId}",
                exitCode = exitCode,
                stdout = simulatedStdout,
                stderr = stderr,
                durationMs = 150L
            )
        }

        override fun stopContainer(containerId: String): Boolean {
            stoppedContainers.add(containerId)
            return true
        }
    }

    private class MockJobResultReporter : JobResultReporter {
        val reportedResults = mutableListOf<Pair<String, ContainerExecutionResult>>()

        override fun reportResult(jobId: String, result: ContainerExecutionResult): Boolean {
            reportedResults.add(jobId to result)
            return true
        }
    }

    private lateinit var mockDockerClient: MockDockerEngineClient
    private lateinit var mockReporter: MockJobResultReporter
    private lateinit var powerAssertionState: AtomicBoolean
    private lateinit var runner: ContainerJobRunner

    @BeforeEach
    fun setup() {
        mockDockerClient = MockDockerEngineClient()
        mockReporter = MockJobResultReporter()
        powerAssertionState = AtomicBoolean(false)
        runner = ContainerJobRunner(
            dockerClient = mockDockerClient,
            resultReporter = mockReporter,
            powerAssertionHook = { awake -> powerAssertionState.set(awake) }
        )
    }

    @Test
    fun testJobExecutionWithResourceLimits() {
        val spec = ContainerRunSpec(
            jobId = "job-42",
            image = "alpine:3.19",
            command = listOf("echo", "hello world"),
            environment = mapOf("ENV_MODE" to "production"),
            cpuLimit = 2.5,
            memoryLimitMb = 4096
        )

        val result = runner.executeJob(spec)

        // Verify execution result
        assertTrue(result.isSuccess)
        assertEquals(0, result.exitCode)
        assertEquals("All tests passed", result.stdout)
        assertEquals("job-42", result.jobId)

        // Verify resource limits were passed to engine
        assertEquals(1, mockDockerClient.executedCommands.size)
        val cmd = mockDockerClient.executedCommands.first()
        assertTrue(cmd.contains("--cpus"))
        assertTrue(cmd.contains("2.5"))
        assertTrue(cmd.contains("--memory"))
        assertTrue(cmd.contains("4096m"))
        assertTrue(cmd.contains("alpine:3.19"))
    }

    @Test
    fun testPowerAssertionAcquiredAndReleasedDuringJob() {
        val hookHistory = mutableListOf<Boolean>()
        val trackingRunner = ContainerJobRunner(
            dockerClient = mockDockerClient,
            resultReporter = mockReporter,
            powerAssertionHook = { awake -> hookHistory.add(awake) }
        )

        val spec = ContainerRunSpec(jobId = "power-test-1", image = "ubuntu:22.04")
        trackingRunner.executeJob(spec)

        // Must have acquired (true) at start and released (false) at completion
        assertEquals(listOf(true, false), hookHistory)
        assertFalse(trackingRunner.isBusy())
    }

    @Test
    fun testFailedContainerCapturesExitCodeAndReportsResult() {
        mockDockerClient.simulatedExitCode = 137 // OOM killed or error
        mockDockerClient.simulatedStderr = "OutOfMemory: Container killed"

        val spec = ContainerRunSpec(
            jobId = "job-oom",
            image = "heavy-task:latest",
            cpuLimit = 1.0,
            memoryLimitMb = 1024
        )

        val result = runner.executeJob(spec)

        assertFalse(result.isSuccess)
        assertEquals(137, result.exitCode)
        assertEquals("OutOfMemory: Container killed", result.stderr)

        // Verify reported to Control Server
        assertEquals(1, mockReporter.reportedResults.size)
        val (reportedJobId, reportedResult) = mockReporter.reportedResults.first()
        assertEquals("job-oom", reportedJobId)
        assertEquals(137, reportedResult.exitCode)
        assertEquals("OutOfMemory: Container killed", reportedResult.stderr)
    }

    @Test
    fun testCancelJobStopsRunningContainer() {
        val spec = ContainerRunSpec(
            jobId = "long-running-job",
            image = "sleep:latest",
            containerName = "custom-runner-name"
        )

        // Start job asynchronously
        val future = java.util.concurrent.CompletableFuture.supplyAsync {
            runner.executeJob(spec)
        }

        // Cancel job
        val stopped = runner.cancelJob("long-running-job")
        assertTrue(stopped)
        assertTrue(mockDockerClient.stoppedContainers.contains("custom-runner-name"))

        future.join()
    }

    @Test
    fun testDockerEngineStatusCheck() {
        val status = mockDockerClient.checkEngineStatus()
        assertTrue(status.available)
        assertEquals("24.0.7", status.version)
        assertEquals("/var/run/docker.sock", status.socketPath)
    }
}
