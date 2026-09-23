package de.heinzelmann.client.ray

import kotlinx.serialization.Serializable

@Serializable
enum class RayWorkerState {
    STOPPED,
    STARTING,
    RUNNING,
    STOPPING,
    FAILED
}

@Serializable
data class RayWorkerConfig(
    val headAddress: String,
    val headPort: Int = 6379,
    val redisPassword: String? = null,
    val numCpus: Int? = null,
    val numGpus: Int? = null,
    val memoryBytes: Long? = null,
    val workerImage: String = "rayproject/ray:latest",
    val containerName: String = "heinzelmann-ray-worker",
    val objectStoreMemoryBytes: Long? = null,
    val extraArgs: List<String> = emptyList()
) {
    fun buildRayStartCommand(): List<String> {
        val cmd = mutableListOf(
            "ray", "start",
            "--address=${headAddress}:${headPort}",
            "--block"
        )
        if (!redisPassword.isNullOrBlank()) {
            cmd.add("--redis-password=$redisPassword")
        }
        if (numCpus != null && numCpus > 0) {
            cmd.add("--num-cpus=$numCpus")
        }
        if (numGpus != null && numGpus > 0) {
            cmd.add("--num-gpus=$numGpus")
        }
        if (objectStoreMemoryBytes != null && objectStoreMemoryBytes > 0) {
            cmd.add("--object-store-memory=$objectStoreMemoryBytes")
        }
        cmd.addAll(extraArgs)
        return cmd
    }
}

@Serializable
data class RayWorkerStatus(
    val state: RayWorkerState,
    val containerId: String? = null,
    val headAddress: String? = null,
    val startedAt: Long? = null,
    val errorMessage: String? = null
)
