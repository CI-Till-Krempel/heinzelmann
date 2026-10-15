package cluster.admin.viewmodel

import cluster.admin.client.ControlServerApiClient
import cluster.admin.model.NodeInfo
import cluster.admin.model.NodeStatus
import cluster.admin.model.NodeSummaryStats
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NodeDashboardUiState(
    val isLoading: Boolean = false,
    val nodes: List<NodeInfo> = emptyList(),
    val summary: NodeSummaryStats = NodeSummaryStats(),
    val errorMessage: String? = null,
    val lastUpdatedEpochMs: Long = 0L,
    val isAutoRefreshEnabled: Boolean = true
)

class NodeStatusViewModel(
    private val apiClient: ControlServerApiClient,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    private val pollIntervalMs: Long = 4000L
) {
    private val _uiState = MutableStateFlow(NodeDashboardUiState())
    val uiState: StateFlow<NodeDashboardUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        startPolling()
    }

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        _uiState.value = _uiState.value.copy(isAutoRefreshEnabled = true)
        pollingJob = coroutineScope.launch {
            while (isActive) {
                refreshNodes()
                delay(pollIntervalMs)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        _uiState.value = _uiState.value.copy(isAutoRefreshEnabled = false)
    }

    suspend fun refreshNodes() {
        _uiState.value = _uiState.value.copy(isLoading = _uiState.value.nodes.isEmpty())
        val result = apiClient.fetchNodes()
        result.fold(
            onSuccess = { nodeList ->
                val summary = computeSummary(nodeList)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    nodes = nodeList,
                    summary = summary,
                    errorMessage = null,
                    lastUpdatedEpochMs = System.currentTimeMillis()
                )
            },
            onFailure = { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Failed to fetch node telemetry"
                )
            }
        )
    }

    private fun computeSummary(nodes: List<NodeInfo>): NodeSummaryStats {
        if (nodes.isEmpty()) return NodeSummaryStats()

        val online = nodes.count { it.status == NodeStatus.ONLINE }
        val busy = nodes.count { it.status == NodeStatus.BUSY }
        val offline = nodes.count { it.status == NodeStatus.OFFLINE }
        val avgCpu = nodes.map { it.metrics.cpuUsagePercent }.average()
        val totalRamUsed = nodes.sumOf { it.metrics.ramUsedMb }
        val totalRamCap = nodes.sumOf { it.metrics.ramTotalMb }

        return NodeSummaryStats(
            totalNodes = nodes.size,
            onlineNodes = online,
            busyNodes = busy,
            offlineNodes = offline,
            avgCpuUsagePercent = if (avgCpu.isNaN()) 0.0 else avgCpu,
            totalRamUsedMb = totalRamUsed,
            totalRamCapacityMb = totalRamCap
        )
    }

    fun filterNodesByStatus(status: NodeStatus?): List<NodeInfo> {
        val current = _uiState.value.nodes
        return if (status == null) current else current.filter { it.status == status }
    }
}
