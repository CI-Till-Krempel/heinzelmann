package de.heinzelmann.daemon.governance

import de.heinzelmann.server.policy.SchedulingWindow
import de.heinzelmann.server.policy.SchedulingWindowPolicy
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalTime

class GovernanceManagerTest {

    private lateinit var activityMonitor: SimulatedUserActivityMonitor
    private lateinit var healthProvider: SimulatedHardwareHealthProvider
    private lateinit var healthGuard: HardwareHealthGuard
    private lateinit var governanceManager: GovernanceManager

    @BeforeEach
    fun setUp() {
        activityMonitor = SimulatedUserActivityMonitor(idleSeconds = 300.0, isUserActive = false)
        healthProvider = SimulatedHardwareHealthProvider(
            isAcConnected = true,
            batteryPercent = 95.0,
            temperatureCelsius = 55.0
        )
        val policy = GovernancePolicy(
            allowedStartHour = 22,
            allowedEndHour = 6,
            minBatteryPercent = 80.0,
            requireAcPower = true,
            maxThermalCelsius = 75.0,
            maxThermalExceedanceSeconds = 60L,
            maxUserInactivityThresholdSeconds = 2.0.toLong()
        )
        healthGuard = HardwareHealthGuard(healthProvider, policy)
        governanceManager = GovernanceManager(activityMonitor, healthGuard, policy)
    }

    @Test
    fun `testInstantUserActivityYieldPausesJobAndReleasesAssertion`() {
        governanceManager.onJobStarted("job-001")
        assertTrue(governanceManager.isJobActive)
        assertTrue(governanceManager.isPowerAssertionHeld)
        assertFalse(governanceManager.isContainerPaused)

        // Baseline tick: user is idle (300s), healthy conditions -> no yield
        val initialDecision = governanceManager.evaluateTick(currentTimeMs = 1000L)
        assertFalse(initialDecision.shouldYield)
        assertFalse(governanceManager.isContainerPaused)
        assertTrue(governanceManager.isPowerAssertionHeld)

        // User moves mouse / types: idle time drops to 0.5s (< 2s threshold)
        activityMonitor.simulateIdle(0.5)

        val yieldDecision = governanceManager.evaluateTick(currentTimeMs = 2000L)
        assertTrue(yieldDecision.shouldYield)
        assertEquals(YieldReason.USER_INTERACTION, yieldDecision.reason)
        assertTrue(governanceManager.isContainerPaused, "Container must be paused on user activity")
        assertFalse(governanceManager.isPowerAssertionHeld, "Power assertion must be relinquished")
    }

    @Test
    fun `testAcDisconnectedYield`() {
        governanceManager.onJobStarted("job-002")

        // Unplug AC power
        healthProvider.isAcConnected = false

        val decision = governanceManager.evaluateTick(currentTimeMs = 1000L)
        assertTrue(decision.shouldYield)
        assertEquals(YieldReason.AC_DISCONNECTED, decision.reason)
        assertTrue(governanceManager.isContainerPaused)
        assertFalse(governanceManager.isPowerAssertionHeld)
    }

    @Test
    fun `testBatteryBelowThresholdYield`() {
        governanceManager.onJobStarted("job-003")

        // Drop battery below 80%
        healthProvider.batteryPercent = 79.0

        val decision = governanceManager.evaluateTick(currentTimeMs = 1000L)
        assertTrue(decision.shouldYield)
        assertEquals(YieldReason.BATTERY_LOW, decision.reason)
        assertTrue(governanceManager.isContainerPaused)
        assertFalse(governanceManager.isPowerAssertionHeld)
    }

    @Test
    fun `testThermalCeilingSustainedExceedance`() {
        governanceManager.onJobStarted("job-004")

        // Exceed thermal ceiling (80°C > 75°C) at t = 0s
        healthProvider.temperatureCelsius = 80.0

        val decisionT0 = governanceManager.evaluateTick(currentTimeMs = 0L)
        assertFalse(decisionT0.shouldYield, "Should tolerate transient thermal spikes")
        assertFalse(governanceManager.isContainerPaused)

        // After 30s at 80°C (< 60s limit)
        val decisionT30 = governanceManager.evaluateTick(currentTimeMs = 30_000L)
        assertFalse(decisionT30.shouldYield, "Should not yield before 60 seconds of exceedance")

        // After 61s at 80°C (>= 60s limit)
        val decisionT61 = governanceManager.evaluateTick(currentTimeMs = 61_000L)
        assertTrue(decisionT61.shouldYield, "Should yield after sustained thermal exceedance > 60s")
        assertEquals(YieldReason.THERMAL_EXCEEDED, decisionT61.reason)
        assertTrue(governanceManager.isContainerPaused)
        assertFalse(governanceManager.isPowerAssertionHeld)
    }

    @Test
    fun `testThermalSpikeResetOnCoolDown`() {
        governanceManager.onJobStarted("job-005")

        // Spike to 80°C for 30s
        healthProvider.temperatureCelsius = 80.0
        governanceManager.evaluateTick(currentTimeMs = 0L)
        governanceManager.evaluateTick(currentTimeMs = 30_000L)

        // Cool down to 65°C at t = 40s
        healthProvider.temperatureCelsius = 65.0
        val coolDecision = governanceManager.evaluateTick(currentTimeMs = 40_000L)
        assertFalse(coolDecision.shouldYield)

        // Heat up again at t = 50s -> timer should have reset
        healthProvider.temperatureCelsius = 80.0
        governanceManager.evaluateTick(currentTimeMs = 50_000L)

        // At t = 80s (30s after second heatup) -> should NOT yield yet
        val decisionAfterReset = governanceManager.evaluateTick(currentTimeMs = 80_000L)
        assertFalse(decisionAfterReset.shouldYield, "Thermal counter should have reset after cooling down")
    }

    @Test
    fun `testSchedulingWindowPolicy22to06`() {
        val policy = SchedulingWindowPolicy(SchedulingWindow(startHour = 22, endHour = 6))

        // Night-time allowed hours
        assertTrue(policy.canDispatchBatchJob(22))
        assertTrue(policy.canDispatchBatchJob(23))
        assertTrue(policy.canDispatchBatchJob(0))
        assertTrue(policy.canDispatchBatchJob(1))
        assertTrue(policy.canDispatchBatchJob(5))

        // Daytime prohibited hours
        assertFalse(policy.canDispatchBatchJob(6))
        assertFalse(policy.canDispatchBatchJob(7))
        assertFalse(policy.canDispatchBatchJob(12))
        assertFalse(policy.canDispatchBatchJob(18))
        assertFalse(policy.canDispatchBatchJob(21))

        // Test with LocalTime
        assertTrue(policy.canDispatchBatchJob(LocalTime.of(23, 30)))
        assertTrue(policy.canDispatchBatchJob(LocalTime.of(3, 15)))
        assertFalse(policy.canDispatchBatchJob(LocalTime.of(14, 0)))
    }

    @Test
    fun `testResumeExecutionRestoresState`() {
        governanceManager.onJobStarted("job-006")
        activityMonitor.simulateIdle(0.5)
        governanceManager.evaluateTick(1000L)

        assertTrue(governanceManager.isContainerPaused)
        assertFalse(governanceManager.isPowerAssertionHeld)

        // User resumes when idle returns
        activityMonitor.simulateIdle(300.0)
        governanceManager.resumeExecution()

        assertFalse(governanceManager.isContainerPaused)
        assertTrue(governanceManager.isPowerAssertionHeld)
    }
}
