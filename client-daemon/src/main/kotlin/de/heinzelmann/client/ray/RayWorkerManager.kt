package de.heinzelmann.client.ray

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

interface RayContainerEngine {
    fun launchContainer(image: String, name: String, command: List<String>, memoryLimit: Long?): String
    fun stopContainer(containerId: String, timeoutSeconds: Int = 10): Boolean
    fun isContainerRunning(containerId: String): Boolean
}

class DefaultRayContainerEngine : RayContainerEngine {
    private val runningContainers = mutableMapOf<String, Boolean>()

    override fun launchContainer(image: String, name: String, command: List<String>, memoryLimit: Long?): String {
        val cid = "ray-ctr-${System.currentTimeMillis()}"
        runningContainers[cid] = true
        return cid
    }

    override fun stopContainer(containerId: String, timeoutSeconds: Int): Boolean {
        return runningContainers.remove(containerId) != null
    }

    override fun isContainerRunning(containerId: String): Boolean {
        return runningContainers[containerId] == true
    }
}

enum class NodeClusterStatus {
    IDLE,
    BUSY,
    DISCONNECTED,
    DRAINING
}

class RayWorkerManager(
    private val containerEngine: RayContainerEngine = DefaultRayContainerEngine()
) {
    private val mutex = Mutex()
    private var currentConfig: RayWorkerConfig? = null
    private var currentStatus: RayWorkerStatus = RayWorkerStatus(state = RayWorkerState.STOPPED)

    fun getStatus(): RayWorkerStatus = currentStatus

    suspend fun startWorker(config: RayWorkerConfig): Result<RayWorkerStatus> = mutex.withLock {
        if (currentStatus.state == RayWorkerState.RUNNING) {
            return Result.failure(IllegalStateException("Ray worker is already running (container ${currentStatus.containerId})"))
        }

        currentStatus = currentStatus.copy(state = RayWorkerState.STARTING, errorMessage = null)
        currentConfig = config

        try {
            val command = config.buildRayStartCommand()
            val containerId = containerEngine.launchContainer(
                image = config.workerImage,
                name = config.containerName,
                command = command,
                memoryLimit = config.memoryBytes
            )

            currentStatus = RayWorkerStatus(
                state = RayWorkerState.RUNNING,
                containerId = containerId,
                headAddress = "${config.headAddress}:${config.headPort}",
                startedAt = Instant.now().epochSecond,
                errorMessage = null
            )
            Result.success(currentStatus)
        } catch (e: Exception) {
            currentStatus = RayWorkerStatus(
                state = RayWorkerState.FAILED,
                containerId = null,
                headAddress = "${config.headAddress}:${config.headPort}",
                startedAt = null,
                errorMessage = "Failed to launch Ray worker: ${e.message}"
            )
            Result.failure(e)
        }
    }

    suspend fun stopWorker(): Result<RayWorkerStatus> = mutex.withLock {
        val containerId = currentStatus.containerId
        if (containerId == null || currentStatus.state == RayWorkerState.STOPPED) {
            currentStatus = RayWorkerStatus(state = RayWorkerState.STOPPED)
            return Result.success(currentStatus)
        }

        currentStatus = currentStatus.copy(state = RayWorkerState.STOPPING)
        val stopped = containerEngine.stopContainer(containerId)

        currentStatus = if (stopped) {
            RayWorkerStatus(
                state = RayWorkerState.STOPPED,
                containerId = null,
                headAddress = null,
                startedAt = null,
                errorMessage = null
            )
        } else {
            RayWorkerStatus(
                state = RayWorkerState.FAILED,
                containerId = containerId,
                errorMessage = "Could not cleanly terminate container $containerId"
            )
        }
        Result.success(currentStatus)
    }

    suspend fun onNodeStatusChanged(newNodeStatus: NodeClusterStatus): Boolean {
        // As per AC: When node status changes to busy or disconnects, then the worker cleanly leaves the Ray cluster.
        return when (newNodeStatus) {
            NodeClusterStatus.BUSY, NodeClusterStatus.DISCONNECTED -> {
                if (currentStatus.state == RayWorkerState.RUNNING || currentStatus.state == RayWorkerState.STARTING) {
                    stopWorker()
                    true
                } else {
                    false
                }
            }
            NodeClusterStatus.IDLE, NodeClusterStatus.DRAINING -> false
        }
    }

    suspend fun checkHealth(): Boolean = mutex.withLock {
        val cid = currentStatus.containerId ?: return false
        if (!containerEngine.isContainerRunning(cid)) {
            currentStatus = currentStatus.copy(
                state = RayWorkerState.FAILED,
                errorMessage = "Container $cid exited unexpectedly"
            )
            return false
        }
        return true
    }
}
