package de.heinzelmann.daemon.governance

interface HardwareHealthProvider {
    fun getHealth(): HardwareHealth
}

class SimulatedHardwareHealthProvider(
    var isAcConnected: Boolean = true,
    var batteryPercent: Double? = 100.0,
    var temperatureCelsius: Double = 50.0
) : HardwareHealthProvider {
    override fun getHealth(): HardwareHealth {
        return HardwareHealth(
            isAcConnected = isAcConnected,
            batteryPercent = batteryPercent,
            temperatureCelsius = temperatureCelsius
        )
    }
}

class HardwareHealthGuard(
    private val provider: HardwareHealthProvider,
    private val policy: GovernancePolicy = GovernancePolicy()
) {
    private var thermalExceedanceStartTimeMs: Long? = null

    fun evaluateHealth(currentTimeMs: Long = System.currentTimeMillis()): GovernanceDecision {
        val health = provider.getHealth()

        // 1. Check AC power connection requirement
        if (policy.requireAcPower && !health.isAcConnected) {
            return GovernanceDecision(
                shouldYield = true,
                reason = YieldReason.AC_DISCONNECTED,
                details = "AC power is disconnected. Jobs require constant mains power."
            )
        }

        // 2. Check minimum battery percentage
        if (health.batteryPercent != null && health.batteryPercent < policy.minBatteryPercent) {
            return GovernanceDecision(
                shouldYield = true,
                reason = YieldReason.BATTERY_LOW,
                details = "Battery level (${health.batteryPercent}%) is below minimum required (${policy.minBatteryPercent}%)."
            )
        }

        // 3. Check thermal ceiling (sustained > 75°C for > 60s)
        if (health.temperatureCelsius > policy.maxThermalCelsius) {
            if (thermalExceedanceStartTimeMs == null) {
                thermalExceedanceStartTimeMs = currentTimeMs
            }
            val exceedanceDurationSec = (currentTimeMs - thermalExceedanceStartTimeMs!!) / 1000L
            if (exceedanceDurationSec >= policy.maxThermalExceedanceSeconds) {
                return GovernanceDecision(
                    shouldYield = true,
                    reason = YieldReason.THERMAL_EXCEEDED,
                    details = "CPU/GPU temperature (${health.temperatureCelsius}°C) exceeded ${policy.maxThermalCelsius}°C for ${exceedanceDurationSec}s."
                )
            }
        } else {
            // Temperature back within safe limits, reset tracker
            thermalExceedanceStartTimeMs = null
        }

        return GovernanceDecision(shouldYield = false)
    }

    fun resetThermalTracking() {
        thermalExceedanceStartTimeMs = null
    }
}
