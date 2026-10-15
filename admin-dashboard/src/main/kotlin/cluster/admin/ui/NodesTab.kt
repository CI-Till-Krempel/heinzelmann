package cluster.admin.ui

import cluster.admin.model.NodeInfo
import cluster.admin.model.NodeStatus
import cluster.admin.model.NodeSummaryStats
import cluster.admin.viewmodel.NodeDashboardUiState

object NodeStatusColor {
    const val ONLINE_HEX = "#4CAF50"
    const val BUSY_HEX = "#FF9800"
    const val OFFLINE_HEX = "#F44336"
    const val UNKNOWN_HEX = "#9E9E9E"

    fun getBadgeColor(status: NodeStatus): String = when (status) {
        NodeStatus.ONLINE -> ONLINE_HEX
        NodeStatus.BUSY -> BUSY_HEX
        NodeStatus.OFFLINE -> OFFLINE_HEX
    }
}

data class NodeRowDisplayItem(
    val nodeId: String,
    val hostname: String,
    val osBadge: String,
    val statusLabel: String,
    val statusColor: String,
    val cpuFormatted: String,
    val ramFormatted: String,
    val activeJob: String
)

object NodesTabRenderer {
    fun formatNodeRows(nodes: List<NodeInfo>): List<NodeRowDisplayItem> {
        return nodes.map { node ->
            val ramPercent = if (node.metrics.ramTotalMb > 0) {
                (node.metrics.ramUsedMb.toDouble() / node.metrics.ramTotalMb * 100).toInt()
            } else 0
            NodeRowDisplayItem(
                nodeId = node.id,
                hostname = node.hostname,
                osBadge = node.os.name,
                statusLabel = node.status.name,
                statusColor = NodeStatusColor.getBadgeColor(node.status),
                cpuFormatted = "%.1f%%".format(node.metrics.cpuUsagePercent),
                ramFormatted = "${node.metrics.ramUsedMb} / ${node.metrics.ramTotalMb} MB ($ramPercent%)",
                activeJob = node.activeJobId ?: "None"
            )
        }
    }

    fun renderDashboardSummary(summary: NodeSummaryStats): String {
        return buildString {
            append("Nodes: ${summary.totalNodes} total | ")
            append("Online: ${summary.onlineNodes} | ")
            append("Busy: ${summary.busyNodes} | ")
            append("Offline: ${summary.offlineNodes} | ")
            append("Avg CPU: %.1f%% | ".format(summary.avgCpuUsagePercent))
            append("RAM: ${summary.totalRamUsedMb}MB / ${summary.totalRamCapacityMb}MB")
        }
    }
}
