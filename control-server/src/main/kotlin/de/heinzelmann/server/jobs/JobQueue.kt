package de.heinzelmann.server.jobs

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class JobQueue {
    private val jobs = ConcurrentHashMap<String, Job>()
    private val lock = ReentrantReadWriteLock()

    fun enqueue(request: JobSubmissionRequest): Job {
        val job = Job(
            name = request.name,
            image = request.image,
            command = request.command,
            environment = request.environment,
            requiredCpuCores = request.requiredCpuCores,
            requiredMemoryMb = request.requiredMemoryMb,
            status = JobStatus.QUEUED
        )
        jobs[job.id] = job
        return job
    }

    fun getJob(jobId: String): Job? = jobs[jobId]

    fun listJobs(status: JobStatus? = null): List<Job> {
        return jobs.values
            .filter { status == null || it.status == status }
            .sortedBy { it.submittedAt }
    }

    fun assignJob(jobId: String, nodeId: String): Job? = lock.write {
        val job = jobs[jobId] ?: return@write null
        if (job.status != JobStatus.QUEUED) {
            return@write null
        }
        val updated = job.copy(
            status = JobStatus.DISPATCHED,
            assignedNodeId = nodeId,
            updatedAt = System.currentTimeMillis()
        )
        jobs[jobId] = updated
        updated
    }

    fun updateJobStatus(jobId: String, status: JobStatus, result: JobResult? = null): Job? = lock.write {
        val job = jobs[jobId] ?: return@write null
        val updated = job.copy(
            status = status,
            result = result ?: job.result,
            updatedAt = System.currentTimeMillis()
        )
        jobs[jobId] = updated
        updated
    }

    fun requeueJob(jobId: String, reason: String? = null): Job? = lock.write {
        val job = jobs[jobId] ?: return@write null
        val updated = job.copy(
            status = JobStatus.QUEUED,
            assignedNodeId = null,
            retryCount = job.retryCount + 1,
            updatedAt = System.currentTimeMillis()
        )
        jobs[jobId] = updated
        updated
    }

    fun cancelJob(jobId: String): Job? = lock.write {
        val job = jobs[jobId] ?: return@write null
        val updated = job.copy(
            status = JobStatus.CANCELLED,
            updatedAt = System.currentTimeMillis()
        )
        jobs[jobId] = updated
        updated
    }

    fun clear() {
        jobs.clear()
    }
}
