package com.heinzelmann.server.models

import kotlinx.serialization.Serializable

@Serializable
enum class NodeStatus {
    ONLINE,
    BUSY,
    OFFLINE
}

@Serializable
data class HardwareSpecs(
    val cpuCores: Int,
    val totalMemoryMb: Long,
    val osType: String,
    val arch: String
)

@Serializable
data class NodeRegistrationRequest(
    val nodeId: String,
    val osType: String,
    val hardwareSpecs: HardwareSpecs
)

@Serializable
data class NodeRegistrationResponse(
    val nodeId: String,
    val status: NodeStatus,
    val registeredAt: Long,
    val message: String
)

@Serializable
data class HeartbeatTelemetry(
    val nodeId: String,
    val cpuUsagePercent: Double,
    val ramUsedMb: Long,
    val ramTotalMb: Long,
    val batteryPercent: Double? = null,
    val temperatureCelsius: Double? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class HeartbeatResponse(
    val nodeId: String,
    val status: NodeStatus,
    val acknowledgedAt: Long
)

@Serializable
data class NodeState(
    val nodeId: String,
    val osType: String,
    val hardwareSpecs: HardwareSpecs,
    val registeredAt: Long,
    val lastHeartbeatAt: Long,
    val latestTelemetry: HeartbeatTelemetry? = null,
    val manualStatus: NodeStatus? = null
) {
    fun currentStatus(now: Long = System.currentTimeMillis(), timeoutMillis: Long = 60_000L): NodeStatus {
        if (manualStatus != null) return manualStatus
        return if (now - lastHeartbeatAt > timeoutMillis) {
            NodeStatus.OFFLINE
        } else {
            NodeStatus.ONLINE
        }
    }
}
