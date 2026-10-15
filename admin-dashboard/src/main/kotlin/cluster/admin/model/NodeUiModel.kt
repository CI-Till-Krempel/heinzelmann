package cluster.admin.model

enum class NodeStatus {
    ONLINE,
    OFFLINE,
    BUSY
}

enum class OsType {
    MACOS,
    WINDOWS,
    LINUX,
    UNKNOWN
}

data class NodeHardwareMetrics(
    val cpuUsagePercent: Double = 0.0,
    val ramUsedMb: Long = 0,
    val ramTotalMb: Long = 0,
    val batteryPercent: Double? = null,
    val isAcConnected: Boolean = true,
    val temperatureCelsius: Double? = null
)

data class NodeInfo(
    val id: String,
    val hostname: String,
    val os: OsType,
    val status: NodeStatus,
    val metrics: NodeHardwareMetrics,
    val lastHeartbeatEpochMs: Long,
    val activeJobId: String? = null
)

data class NodeSummaryStats(
    val totalNodes: Int = 0,
    val onlineNodes: Int = 0,
    val busyNodes: Int = 0,
    val offlineNodes: Int = 0,
    val avgCpuUsagePercent: Double = 0.0,
    val totalRamUsedMb: Long = 0,
    val totalRamCapacityMb: Long = 0
)
