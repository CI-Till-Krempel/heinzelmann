package cluster.admin.client

import cluster.admin.model.NodeInfo
import cluster.admin.model.NodeStatus
import cluster.admin.model.OsType
import cluster.admin.model.NodeHardwareMetrics

interface ControlServerApiClient {
    suspend fun fetchNodes(): Result<List<NodeInfo>>
    suspend fun getNodeById(nodeId: String): Result<NodeInfo?>
}

class AdminDashboardClient(
    val serverBaseUrl: String = "http://localhost:8080",
    private val fetcher: (suspend (String) -> String)? = null
) : ControlServerApiClient {

    private val inMemoryNodes = mutableMapOf<String, NodeInfo>()

    fun registerNodeLocally(node: NodeInfo) {
        inMemoryNodes[node.id] = node
    }

    fun updateNodeMetrics(nodeId: String, metrics: NodeHardwareMetrics, status: NodeStatus) {
        val existing = inMemoryNodes[nodeId]
        if (existing != null) {
            inMemoryNodes[nodeId] = existing.copy(
                metrics = metrics,
                status = status,
                lastHeartbeatEpochMs = System.currentTimeMillis()
            )
        }
    }

    override suspend fun fetchNodes(): Result<List<NodeInfo>> {
        return try {
            if (fetcher != null) {
                fetcher.invoke("$serverBaseUrl/api/nodes")
                Result.success(inMemoryNodes.values.toList())
            } else {
                Result.success(inMemoryNodes.values.toList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getNodeById(nodeId: String): Result<NodeInfo?> {
        return Result.success(inMemoryNodes[nodeId])
    }
}
