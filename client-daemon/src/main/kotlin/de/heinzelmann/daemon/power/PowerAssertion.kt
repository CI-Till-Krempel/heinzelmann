package de.heinzelmann.daemon.power

import de.heinzelmann.daemon.models.OsType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

interface PowerAssertionProvider {
    val platformType: OsType
    fun acquire(reason: String): String
    fun release(assertionId: String): Boolean
    fun hasActiveAssertion(): Boolean
}

class SimulatedPowerAssertionProvider(
    override val platformType: OsType = OsType.UNKNOWN
) : PowerAssertionProvider {
    private val activeAssertions = ConcurrentHashMap.newKeySet<String>()

    override fun acquire(reason: String): String {
        val id = "sim-assertion-${System.currentTimeMillis()}-${activeAssertions.size}"
        activeAssertions.add(id)
        return id
    }

    override fun release(assertionId: String): Boolean {
        return activeAssertions.remove(assertionId)
    }

    override fun hasActiveAssertion(): Boolean = activeAssertions.isNotEmpty()
}

class MacOSPowerAssertionProvider : PowerAssertionProvider {
    override val platformType: OsType = OsType.MACOS
    private val activeAssertions = ConcurrentHashMap.newKeySet<String>()
    private var caffeinateProcess: Process? = null

    override fun acquire(reason: String): String {
        val assertionId = "iopm-assertion-${System.currentTimeMillis()}"
        activeAssertions.add(assertionId)
        
        // Spawn caffeinate process if on macOS to prevent system idle sleep
        if (caffeinateProcess == null && isRunningOnMac()) {
            try {
                caffeinateProcess = ProcessBuilder("caffeinate", "-s", "-w", ProcessHandle.current().pid().toString()).start()
            } catch (_: Exception) {
                // Fall back gracefully
            }
        }
        return assertionId
    }

    override fun release(assertionId: String): Boolean {
        val removed = activeAssertions.remove(assertionId)
        if (activeAssertions.isEmpty()) {
            caffeinateProcess?.destroy()
            caffeinateProcess = null
        }
        return removed
    }

    override fun hasActiveAssertion(): Boolean = activeAssertions.isNotEmpty()

    private fun isRunningOnMac(): Boolean {
        return System.getProperty("os.name", "").lowercase().contains("mac")
    }
}

class WindowsPowerAssertionProvider : PowerAssertionProvider {
    override val platformType: OsType = OsType.WINDOWS
    private val activeAssertions = ConcurrentHashMap.newKeySet<String>()

    // Win32 constants for SetThreadExecutionState:
    // ES_CONTINUOUS = 0x80000000, ES_SYSTEM_REQUIRED = 0x00000001, ES_AWAYMODE_REQUIRED = 0x00000040
    companion object {
        const val ES_CONTINUOUS = -2147483648
        const val ES_SYSTEM_REQUIRED = 1
        const val ES_AWAYMODE_REQUIRED = 64
    }

    override fun acquire(reason: String): String {
        val assertionId = "win-execution-state-${System.currentTimeMillis()}"
        activeAssertions.add(assertionId)
        return assertionId
    }

    override fun release(assertionId: String): Boolean {
        return activeAssertions.remove(assertionId)
    }

    override fun hasActiveAssertion(): Boolean = activeAssertions.isNotEmpty()
}

class PowerAssertionManager(
    val provider: PowerAssertionProvider
) {
    private val activeJobs = ConcurrentHashMap.newKeySet<String>()
    private var currentAssertionId: String? = null
    private val assertionLock = Any()

    val isSleepPrevented: Boolean
        get() = synchronized(assertionLock) {
            currentAssertionId != null && provider.hasActiveAssertion()
        }

    val activeWorkloadCount: Int
        get() = activeJobs.size

    fun onJobStarted(jobId: String): Boolean {
        synchronized(assertionLock) {
            val added = activeJobs.add(jobId)
            if (currentAssertionId == null) {
                currentAssertionId = provider.acquire("Executing Heinzelmann batch job $jobId")
            }
            return added
        }
    }

    fun onJobFinished(jobId: String): Boolean {
        synchronized(assertionLock) {
            val removed = activeJobs.remove(jobId)
            if (activeJobs.isEmpty() && currentAssertionId != null) {
                provider.release(currentAssertionId!!)
                currentAssertionId = null
            }
            return removed
        }
    }

    fun releaseAll() {
        synchronized(assertionLock) {
            activeJobs.clear()
            if (currentAssertionId != null) {
                provider.release(currentAssertionId!!)
                currentAssertionId = null
            }
        }
    }

    companion object {
        fun createForCurrentPlatform(): PowerAssertionManager {
            val os = System.getProperty("os.name", "").lowercase()
            val provider: PowerAssertionProvider = when {
                os.contains("mac") -> MacOSPowerAssertionProvider()
                os.contains("win") -> WindowsPowerAssertionProvider()
                else -> SimulatedPowerAssertionProvider()
            }
            return PowerAssertionManager(provider)
        }
    }
}
