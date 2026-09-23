package de.heinzelmann.daemon

import de.heinzelmann.daemon.models.OsType
import de.heinzelmann.daemon.power.MacOSPowerAssertionProvider
import de.heinzelmann.daemon.power.PowerAssertionManager
import de.heinzelmann.daemon.power.SimulatedPowerAssertionProvider
import de.heinzelmann.daemon.power.WindowsPowerAssertionProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PowerAssertionTest {

    @Test
    fun testPowerAssertionAcquiredOnFirstJobAndReleasedOnCompletion() {
        val provider = SimulatedPowerAssertionProvider()
        val manager = PowerAssertionManager(provider)

        assertFalse(manager.isSleepPrevented, "Initial state should not prevent sleep")
        assertEquals(0, manager.activeWorkloadCount)

        // Start job 1 -> assertion acquired
        manager.onJobStarted("job-001")
        assertTrue(manager.isSleepPrevented, "Power assertion should be active while job is running")
        assertEquals(1, manager.activeWorkloadCount)

        // Complete job 1 -> assertion released
        manager.onJobFinished("job-001")
        assertFalse(manager.isSleepPrevented, "Power assertion should be released when all jobs finish")
        assertEquals(0, manager.activeWorkloadCount)
    }

    @Test
    fun testMultipleConcurrentJobsKeepAssertionActiveUntilLastCompletes() {
        val provider = SimulatedPowerAssertionProvider()
        val manager = PowerAssertionManager(provider)

        manager.onJobStarted("job-A")
        manager.onJobStarted("job-B")
        assertEquals(2, manager.activeWorkloadCount)
        assertTrue(manager.isSleepPrevented)

        // Finish job-A, job-B is still active
        manager.onJobFinished("job-A")
        assertTrue(manager.isSleepPrevented, "Power assertion should remain active while job-B is still running")
        assertEquals(1, manager.activeWorkloadCount)

        // Finish job-B
        manager.onJobFinished("job-B")
        assertFalse(manager.isSleepPrevented, "Power assertion must be released once all jobs complete")
        assertEquals(0, manager.activeWorkloadCount)
    }

    @Test
    fun testPlatformProvidersAcquireAndRelease() {
        val macProvider = MacOSPowerAssertionProvider()
        assertEquals(OsType.MACOS, macProvider.platformType)
        val macId = macProvider.acquire("Batch task")
        assertTrue(macProvider.hasActiveAssertion())
        assertTrue(macProvider.release(macId))
        assertFalse(macProvider.hasActiveAssertion())

        val winProvider = WindowsPowerAssertionProvider()
        assertEquals(OsType.WINDOWS, winProvider.platformType)
        val winId = winProvider.acquire("Batch task")
        assertTrue(winProvider.hasActiveAssertion())
        assertTrue(winProvider.release(winId))
        assertFalse(winProvider.hasActiveAssertion())
    }

    @Test
    fun testReleaseAllCleansUpActiveAssertions() {
        val provider = SimulatedPowerAssertionProvider()
        val manager = PowerAssertionManager(provider)

        manager.onJobStarted("job-101")
        manager.onJobStarted("job-102")
        assertTrue(manager.isSleepPrevented)

        manager.releaseAll()
        assertFalse(manager.isSleepPrevented)
        assertEquals(0, manager.activeWorkloadCount)
    }
}
