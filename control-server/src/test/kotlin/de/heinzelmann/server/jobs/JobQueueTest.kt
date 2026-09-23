package de.heinzelmann.server.jobs

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JobQueueTest {

    private lateinit var jobQueue: JobQueue
    private lateinit var dispatcher: JobDispatcher

    @BeforeEach
    fun setup() {
        jobQueue = JobQueue()
        dispatcher = JobDispatcher(jobQueue, heartbeatTimeoutMs = 60_000L)
    }

    @Test
    fun testJobEnqueueAndRetrieve() {
        val request = JobSubmissionRequest(
            name = "llama3-eval-batch",
            image = "ollama/ollama:latest",
            command = listOf("ollama", "run", "llama3"),
            requiredCpuCores = 4.0,
            requiredMemoryMb = 8192
        )
        val job = jobQueue.enqueue(request)

        assertNotNull(job.id)
        assertEquals("llama3-eval-batch", job.name)
        assertEquals(JobStatus.QUEUED, job.status)
        assertNull(job.assignedNodeId)

        val retrieved = jobQueue.getJob(job.id)
        assertNotNull(retrieved)
        assertEquals(job.id, retrieved?.id)
    }

    @Test
    fun testNodeCapacitySchedulingAndDispatch() {
        // Enqueue 2 jobs
        val job1 = jobQueue.enqueue(
            JobSubmissionRequest(name = "job-1", image = "ubuntu:22.04", requiredCpuCores = 2.0, requiredMemoryMb = 2048)
        )
        val job2 = jobQueue.enqueue(
            JobSubmissionRequest(name = "job-2", image = "ubuntu:22.04", requiredCpuCores = 8.0, requiredMemoryMb = 16384)
        )

        // Register Node A (4 cores, 4096 MB) - can only run job1
        // Register Node B (16 cores, 32768 MB) - can run job2
        dispatcher.updateNodeCapacity(
            NodeCapacity(nodeId = "node-a", totalCpuCores = 4.0, totalMemoryMb = 4096)
        )
        dispatcher.updateNodeCapacity(
            NodeCapacity(nodeId = "node-b", totalCpuCores = 16.0, totalMemoryMb = 32768)
        )

        val assignments = dispatcher.dispatchNextPendingJobs()
        assertEquals(2, assignments.size)

        val assignment1 = assignments.first { it.jobId == job1.id }
        val assignment2 = assignments.first { it.jobId == job2.id }

        assertEquals(JobStatus.DISPATCHED, jobQueue.getJob(job1.id)?.status)
        assertEquals(JobStatus.DISPATCHED, jobQueue.getJob(job2.id)?.status)
        assertEquals("node-a", assignment1.nodeId)
        assertEquals("node-b", assignment2.nodeId)
    }

    @Test
    fun testInsufficientCapacityBlocksDispatch() {
        val largeJob = jobQueue.enqueue(
            JobSubmissionRequest(name = "giant-task", image = "pytorch:latest", requiredCpuCores = 32.0, requiredMemoryMb = 65536)
        )
        // Small node cannot take it
        dispatcher.updateNodeCapacity(
            NodeCapacity(nodeId = "small-node", totalCpuCores = 4.0, totalMemoryMb = 8192)
        )

        val assignments = dispatcher.dispatchNextPendingJobs()
        assertTrue(assignments.isEmpty())
        assertEquals(JobStatus.QUEUED, jobQueue.getJob(largeJob.id)?.status)
    }

    @Test
    fun testHeartbeatFailureTriggersRequeueAndAlternateNodeFailover() {
        val now = 1000000L
        val job = jobQueue.enqueue(
            JobSubmissionRequest(name = "critical-task", image = "python:3.11", requiredCpuCores = 2.0, requiredMemoryMb = 2048)
        )

        // Node 1 assigned
        dispatcher.updateNodeCapacity(
            NodeCapacity(nodeId = "node-failing", totalCpuCores = 4.0, totalMemoryMb = 4096, lastHeartbeatTimestamp = now)
        )
        val initialAssignments = dispatcher.dispatchNextPendingJobs()
        assertEquals(1, initialAssignments.size)
        assertEquals("node-failing", initialAssignments.first().nodeId)
        assertEquals(JobStatus.DISPATCHED, jobQueue.getJob(job.id)?.status)

        // Simulate node stopping heartbeats (70 seconds later > 60s timeout)
        val recovered = dispatcher.checkAndRecoverFailures(currentTimeMs = now + 70_000L)
        assertEquals(1, recovered.size)
        assertEquals(job.id, recovered.first().id)
        assertEquals(JobStatus.QUEUED, jobQueue.getJob(job.id)?.status)
        assertNull(jobQueue.getJob(job.id)?.assignedNodeId)
        assertEquals(1, jobQueue.getJob(job.id)?.retryCount)

        // Mark node-failing as offline and add alternate node-healthy
        dispatcher.updateNodeCapacity(
            NodeCapacity(nodeId = "node-failing", totalCpuCores = 4.0, totalMemoryMb = 4096, isOnline = false, lastHeartbeatTimestamp = now)
        )
        dispatcher.updateNodeCapacity(
            NodeCapacity(nodeId = "node-healthy", totalCpuCores = 4.0, totalMemoryMb = 4096, isOnline = true, lastHeartbeatTimestamp = now + 70_000L)
        )

        // Re-dispatch on alternate node
        val failoverAssignments = dispatcher.dispatchNextPendingJobs()
        assertEquals(1, failoverAssignments.size)
        assertEquals(job.id, failoverAssignments.first().jobId)
        assertEquals("node-healthy", failoverAssignments.first().nodeId)
        assertEquals(JobStatus.DISPATCHED, jobQueue.getJob(job.id)?.status)
    }

    @Test
    fun testJobStatusTransitionsAndCompletionResult() {
        val job = jobQueue.enqueue(
            JobSubmissionRequest(name = "status-test", image = "alpine:latest")
        )
        dispatcher.updateNodeCapacity(
            NodeCapacity(nodeId = "worker-1", totalCpuCores = 2.0, totalMemoryMb = 2048)
        )
        dispatcher.dispatchNextPendingJobs()

        // Worker transitions to RUNNING
        jobQueue.updateJobStatus(job.id, JobStatus.RUNNING)
        assertEquals(JobStatus.RUNNING, jobQueue.getJob(job.id)?.status)

        // Worker reports SUCCEEDED with execution result
        val result = JobResult(exitCode = 0, stdout = "Execution finished successfully", stderr = "", durationMs = 1250)
        jobQueue.updateJobStatus(job.id, JobStatus.SUCCEEDED, result)

        val completed = jobQueue.getJob(job.id)
        assertEquals(JobStatus.SUCCEEDED, completed?.status)
        assertNotNull(completed?.result)
        assertEquals(0, completed?.result?.exitCode)
        assertEquals("Execution finished successfully", completed?.result?.stdout)
    }
}
