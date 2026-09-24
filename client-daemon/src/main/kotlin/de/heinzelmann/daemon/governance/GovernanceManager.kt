package de.heinzelmann.daemon.governance

class GovernanceManager(
    private val activityMonitor: UserActivityMonitor,
    private val healthGuard: HardwareHealthGuard,
    val policy: GovernancePolicy = GovernancePolicy(),
    private val onYieldCallback: ((GovernanceDecision) -> Unit)? = null
) {
    var isJobActive: Boolean = false
        private set
    var activeJobId: String? = null
        private set
    var isContainerPaused: Boolean = false
        private set
    var isPowerAssertionHeld: Boolean = false
        private set
    var lastDecision: GovernanceDecision = GovernanceDecision(shouldYield = false)
        private set

    fun onJobStarted(jobId: String) {
        activeJobId = jobId
        isJobActive = true
        isContainerPaused = false
        isPowerAssertionHeld = true
        healthGuard.resetThermalTracking()
    }

    fun onJobCompleted(jobId: String) {
        if (activeJobId == jobId) {
            activeJobId = null
            isJobActive = false
            isContainerPaused = false
            isPowerAssertionHeld = false
            healthGuard.resetThermalTracking()
        }
    }

    fun evaluateTick(currentTimeMs: Long = System.currentTimeMillis()): GovernanceDecision {
        if (!isJobActive || isContainerPaused) {
            return GovernanceDecision(shouldYield = false)
        }

        // 1. Instant User Activity check (<2s threshold)
        val activity = activityMonitor.checkActivity()
        if (activity.isUserActive || activity.idleSeconds < policy.maxUserInactivityThresholdSeconds) {
            val decision = GovernanceDecision(
                shouldYield = true,
                reason = YieldReason.USER_INTERACTION,
                details = "User interaction detected (idle time: ${activity.idleSeconds}s). Yielding within 2s."
            )
            triggerYield(decision)
            return decision
        }

        // 2. Hardware Health Guard check (AC, Battery >= 80%, Thermals <= 75°C)
        val healthDecision = healthGuard.evaluateHealth(currentTimeMs)
        if (healthDecision.shouldYield) {
            triggerYield(healthDecision)
            return healthDecision
        }

        lastDecision = GovernanceDecision(shouldYield = false)
        return lastDecision
    }

    private fun triggerYield(decision: GovernanceDecision) {
        lastDecision = decision
        isContainerPaused = true
        isPowerAssertionHeld = false // Relinquish power assertion immediately
        onYieldCallback?.invoke(decision)
    }

    fun resumeExecution() {
        if (isJobActive && isContainerPaused) {
            isContainerPaused = false
            isPowerAssertionHeld = true
            healthGuard.resetThermalTracking()
            lastDecision = GovernanceDecision(shouldYield = false)
        }
    }
}
