package de.heinzelmann.server.jobs

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class NodeCapacityScheduler {
    fun selectNodeForJob(job: Job, availableNodes: List<NodeCapacity>): NodeCapacity? {
        return availableNodes
            .filter { it.canAccommodate(job) }
            .sortedWith(
                compareByDescending<NodeCapacity> { it.availableCpuCores }
                    .thenByDescending { it.availableMemoryMb }
            )
            .firstOrNull()
    }
}

class JobDispatcher(
    private val jobQueue: JobQueue,
    private val scheduler: NodeCapacityScheduler = NodeCapacityScheduler(),
    private val heartbeatTimeoutMs: Long = 60_000L
) {
    private val logger = LoggerFactory.getLogger(JobDispatcher::class.java)
    private val nodeTracking = ConcurrentHashMap<String, NodeCapacity>()

    fun updateNodeCapacity(capacity: NodeCapacity) {
        nodeTracking[capacity.nodeId] = capacity
    }

    fun removeNode(nodeId: String) {
        nodeTracking.remove(nodeId)
    }

    fun getNodeCapacity(nodeId: String): NodeCapacity? = nodeTracking[nodeId]

    fun dispatchNextPendingJobs(): List<JobAssignment> {
        val queuedJobs = jobQueue.listJobs(JobStatus.QUEUED)
        val assignments = mutableListOf<JobAssignment>()
        val currentNodes = nodeTracking.values.map { it.copy() }.toMutableList()

        for (job in queuedJobs) {
            val selectedNode = scheduler.selectNodeForJob(job, currentNodes) ?: continue

            val assignedJob = jobQueue.assignJob(job.id, selectedNode.nodeId)
            if (assignedJob != null) {
                assignments.add(JobAssignment(jobId = job.id, nodeId = selectedNode.nodeId))

                // Update simulated in-memory capacity for remainder of dispatch round
                val updatedCapacity = selectedNode.copy(
                    usedCpuCores = selectedNode.usedCpuCores + job.requiredCpuCores,
                    usedMemoryMb = selectedNode.usedMemoryMb + job.requiredMemoryMb
                )
                val idx = currentNodes.indexOfFirst { it.nodeId == selectedNode.nodeId }
                if (idx >= 0) {
                    currentNodes[idx] = updatedCapacity
                }
                nodeTracking[selectedNode.nodeId] = updatedCapacity
                logger.info("Dispatched job ${job.id} to node ${selectedNode.nodeId}")
            }
        }
        return assignments
    }

    /**
     * Inspects nodes for heartbeat expiration or offline state.
     * Any DISPATCHED or RUNNING job assigned to an expired or offline node is re-queued.
     */
    fun checkAndRecoverFailures(currentTimeMs: Long = System.currentTimeMillis()): List<Job> {
        val recoveredJobs = mutableListOf<Job>()
        val activeJobs = jobQueue.listJobs().filter {
            it.status == JobStatus.DISPATCHED || it.status == JobStatus.RUNNING
        }

        for (job in activeJobs) {
            val nodeId = job.assignedNodeId ?: continue
            val node = nodeTracking[nodeId]

            val isOfflineOrTimedOut = node == null ||
                    !node.isOnline ||
                    (currentTimeMs - node.lastHeartbeatTimestamp > heartbeatTimeoutMs)

            if (isOfflineOrTimedOut) {
                logger.warn("Node $nodeId for job ${job.id} is offline or timed out. Re-queueing job.")
                val requeued = jobQueue.requeueJob(job.id, reason = "Node $nodeId heartbeat timeout")
                if (requeued != null) {
                    recoveredJobs.add(requeued)
                }
            }
        }
        return recoveredJobs
    }
}
