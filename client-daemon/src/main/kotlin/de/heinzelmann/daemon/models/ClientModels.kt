package de.heinzelmann.daemon.models

import kotlinx.serialization.Serializable

@Serializable
enum class OsType {
    MACOS,
    WINDOWS,
    LINUX,
    UNKNOWN
}

@Serializable
enum class DaemonState {
    INITIALIZING,
    REGISTERED,
    RUNNING,
    STOPPING,
    STOPPED,
    ERROR
}

@Serializable
data class HardwareSpecs(
    val cpuCores: Int,
    val totalRamMb: Long,
    val architecture: String,
    val osVersion: String
)

@Serializable
data class SystemMetrics(
    val cpuUsagePercent: Double,
    val ramUsedMb: Long,
    val ramTotalMb: Long,
    val batteryPercent: Double?,
    val isCharging: Boolean?,
    val temperatureCelsius: Double?,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class NodeRegistrationRequest(
    val nodeId: String,
    val hostname: String,
    val osType: OsType,
    val hardwareSpecs: HardwareSpecs
)

@Serializable
data class NodeRegistrationResponse(
    val nodeId: String,
    val registered: Boolean,
    val heartbeatIntervalSeconds: Int = 10,
    val message: String = "Registration successful"
)

@Serializable
data class HeartbeatRequest(
    val nodeId: String,
    val status: String = "ACTIVE",
    val metrics: SystemMetrics,
    val activeJobsCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class HeartbeatResponse(
    val acknowledged: Boolean,
    val command: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class DaemonConfig(
    val serverUrl: String = "http://localhost:8080",
    val nodeId: String = "node-" + java.util.UUID.randomUUID().toString().substring(0, 8),
    val heartbeatIntervalMs: Long = 5000L,
    val maxIdleCpuPercent: Double = 1.0,
    val maxIdleRamMb: Long = 50L
)
