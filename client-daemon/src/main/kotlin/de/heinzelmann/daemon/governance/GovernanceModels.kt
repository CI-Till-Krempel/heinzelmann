package de.heinzelmann.daemon.governance

import kotlinx.serialization.Serializable

@Serializable
enum class YieldReason {
    USER_INTERACTION,
    AC_DISCONNECTED,
    BATTERY_LOW,
    THERMAL_EXCEEDED,
    OUTSIDE_WINDOW
}

@Serializable
data class GovernancePolicy(
    val allowedStartHour: Int = 22,
    val allowedEndHour: Int = 6,
    val minBatteryPercent: Double = 80.0,
    val requireAcPower: Boolean = true,
    val maxThermalCelsius: Double = 75.0,
    val maxThermalExceedanceSeconds: Long = 60L,
    val maxUserInactivityThresholdSeconds: Long = 2L
)

@Serializable
data class HardwareHealth(
    val isAcConnected: Boolean,
    val batteryPercent: Double?,
    val temperatureCelsius: Double,
    val timestampMs: Long = System.currentTimeMillis()
)

@Serializable
data class UserActivityStatus(
    val isUserActive: Boolean,
    val idleSeconds: Double,
    val timestampMs: Long = System.currentTimeMillis()
)

@Serializable
data class GovernanceDecision(
    val shouldYield: Boolean,
    val reason: YieldReason? = null,
    val details: String? = null
)
