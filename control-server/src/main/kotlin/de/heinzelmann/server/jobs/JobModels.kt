package de.heinzelmann.server.jobs

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class JobStatus {
    QUEUED,
    DISPATCHED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED
}

@Serializable
data class Job(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val image: String,
    val command: List<String> = emptyList(),
    val environment: Map<String, String> = emptyMap(),
    val requiredCpuCores: Double = 1.0,
    val requiredMemoryMb: Long = 512,
    var status: JobStatus = JobStatus.QUEUED,
    var assignedNodeId: String? = null,
    val submittedAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var result: JobResult? = null,
    var retryCount: Int = 0
)

@Serializable
data class JobSubmissionRequest(
    val name: String,
    val image: String,
    val command: List<String> = emptyList(),
    val environment: Map<String, String> = emptyMap(),
    val requiredCpuCores: Double = 1.0,
    val requiredMemoryMb: Long = 512
)

@Serializable
data class JobResult(
    val exitCode: Int,
    val stdout: String = "",
    val stderr: String = "",
    val durationMs: Long = 0L
)

@Serializable
data class JobAssignment(
    val jobId: String,
    val nodeId: String,
    val assignedAt: Long = System.currentTimeMillis()
)

@Serializable
data class JobStatusUpdateRequest(
    val status: JobStatus,
    val result: JobResult? = null
)

@Serializable
data class NodeCapacity(
    val nodeId: String,
    val totalCpuCores: Double,
    val totalMemoryMb: Long,
    val usedCpuCores: Double = 0.0,
    val usedMemoryMb: Long = 0,
    val isOnline: Boolean = true,
    val lastHeartbeatTimestamp: Long = System.currentTimeMillis()
) {
    val availableCpuCores: Double get() = (totalCpuCores - usedCpuCores).coerceAtLeast(0.0)
    val availableMemoryMb: Long get() = (totalMemoryMb - usedMemoryMb).coerceAtLeast(0L)

    fun canAccommodate(job: Job): Boolean {
        return isOnline && availableCpuCores >= job.requiredCpuCores && availableMemoryMb >= job.requiredMemoryMb
    }
}
