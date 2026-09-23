package de.heinzelmann.daemon.lifecycle

import de.heinzelmann.daemon.client.ControlServerClient
import de.heinzelmann.daemon.collector.SystemMetricsCollector
import de.heinzelmann.daemon.models.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class DaemonLifecycleManager(
    val config: DaemonConfig,
    val metricsCollector: SystemMetricsCollector,
    val serverClient: ControlServerClient
) {
    private val _state = AtomicReference(DaemonState.INITIALIZING)
    val state: DaemonState get() = _state.get()

    private val isRunning = AtomicBoolean(false)
    private var workerThread: Thread? = null

    var lastSentHeartbeat: HeartbeatRequest? = null
        private set

    var heartbeatCount: Int = 0
        private set

    fun start(): Boolean {
        if (!_state.compareAndSet(DaemonState.INITIALIZING, DaemonState.REGISTERED)) {
            if (_state.get() != DaemonState.STOPPED) {
                return false
            }
            _state.set(DaemonState.REGISTERED)
        }

        try {
            val registration = NodeRegistrationRequest(
                nodeId = config.nodeId,
                hostname = metricsCollector.getHostname(),
                osType = metricsCollector.detectOsType(),
                hardwareSpecs = metricsCollector.detectHardwareSpecs()
            )
            val response = serverClient.registerNode(registration)
            if (!response.registered) {
                _state.set(DaemonState.ERROR)
                return false
            }
        } catch (e: Exception) {
            _state.set(DaemonState.ERROR)
            return false
        }

        _state.set(DaemonState.RUNNING)
        isRunning.set(true)

        workerThread = Thread {
            while (isRunning.get()) {
                try {
                    performHeartbeatCycle()
                    Thread.sleep(config.heartbeatIntervalMs)
                } catch (_: InterruptedException) {
                    break
                } catch (_: Exception) {
                    // Log and continue on transient error
                }
            }
        }.apply {
            isDaemon = true
            name = "client-daemon-heartbeat-loop"
            start()
        }

        return true
    }

    fun performHeartbeatCycle(): HeartbeatResponse {
        val metrics = metricsCollector.sampleMetrics()
        val heartbeat = HeartbeatRequest(
            nodeId = config.nodeId,
            status = if (isRunning.get()) "ACTIVE" else "STOPPING",
            metrics = metrics,
            activeJobsCount = 0
        )
        val response = serverClient.sendHeartbeat(heartbeat)
        lastSentHeartbeat = heartbeat
        heartbeatCount++
        return response
    }

    fun verifyIdleFootprint(): Boolean {
        val metrics = metricsCollector.sampleMetrics()
        val cpuOk = metrics.cpuUsagePercent <= config.maxIdleCpuPercent
        val ramOk = metrics.ramUsedMb <= config.maxIdleRamMb
        return cpuOk && ramOk
    }

    fun stop() {
        if (_state.get() == DaemonState.RUNNING) {
            _state.set(DaemonState.STOPPING)
            isRunning.set(false)
            workerThread?.interrupt()
            workerThread?.join(2000)
            _state.set(DaemonState.STOPPED)
        }
    }
}
