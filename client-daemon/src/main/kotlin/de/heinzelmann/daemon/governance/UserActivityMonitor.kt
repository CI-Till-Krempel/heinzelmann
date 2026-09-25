package de.heinzelmann.daemon.governance

interface UserActivityMonitor {
    fun checkActivity(): UserActivityStatus
}

class SimulatedUserActivityMonitor(
    var idleSeconds: Double = 300.0,
    var isUserActive: Boolean = false
) : UserActivityMonitor {
    override fun checkActivity(): UserActivityStatus {
        return UserActivityStatus(
            isUserActive = isUserActive || idleSeconds < 2.0,
            idleSeconds = idleSeconds
        )
    }

    fun triggerUserInput() {
        isUserActive = true
        idleSeconds = 0.0
    }

    fun simulateIdle(seconds: Double) {
        idleSeconds = seconds
        isUserActive = seconds < 2.0
    }
}

class PlatformUserActivityMonitor : UserActivityMonitor {
    private val osName = System.getProperty("os.name", "").lowercase()

    override fun checkActivity(): UserActivityStatus {
        return try {
            if (osName.contains("mac")) {
                checkMacOsIdleTime()
            } else if (osName.contains("win")) {
                checkWindowsIdleTime()
            } else {
                // Linux / generic fallback
                UserActivityStatus(isUserActive = false, idleSeconds = 300.0)
            }
        } catch (e: Exception) {
            UserActivityStatus(isUserActive = false, idleSeconds = 300.0)
        }
    }

    private fun checkMacOsIdleTime(): UserActivityStatus {
        // Query IOHIDSystem idle time via ioreg
        val process = ProcessBuilder("ioreg", "-c", "IOHIDSystem").start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()

        val hidIdleRegex = "\"HIDIdleTime\"\\s*=\\s*(\\d+)".toRegex()
        val match = hidIdleRegex.find(output)
        val idleNanos = match?.groupValues?.get(1)?.toLongOrNull() ?: (300L * 1_000_000_000L)
        val idleSec = idleNanos.toDouble() / 1_000_000_000.0
        return UserActivityStatus(isUserActive = idleSec < 2.0, idleSeconds = idleSec)
    }

    private fun checkWindowsIdleTime(): UserActivityStatus {
        // Fallback for Windows environment without native DLL
        return UserActivityStatus(isUserActive = false, idleSeconds = 300.0)
    }
}
