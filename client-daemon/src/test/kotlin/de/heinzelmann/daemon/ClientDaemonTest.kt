package de.heinzelmann.daemon

import de.heinzelmann.daemon.client.MockControlServerClient
import de.heinzelmann.daemon.collector.DefaultSystemMetricsCollector
import de.heinzelmann.daemon.collector.SystemMetricsCollector
import de.heinzelmann.daemon.lifecycle.DaemonLifecycleManager
import de.heinzelmann.daemon.models.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ClientDaemonTest {

    @Test
    fun testMetricsCollectorSamplesRequiredTelemetry() {
        val collector = DefaultSystemMetricsCollector()
        val metrics = collector.sampleMetrics()

        assertNotNull(metrics)
        assertTrue(metrics.cpuUsagePercent >= 0.0, "CPU usage must be non-negative")
        assertTrue(metrics.ramUsedMb >= 0, "RAM used must be non-negative")
        assertTrue(metrics.ramTotalMb > 0, "RAM total must be greater than zero")
        assertNotNull(metrics.batteryPercent, "Battery percent must be sampled")
        assertNotNull(metrics.isCharging, "Charging status must be sampled")
        assertNotNull(metrics.temperatureCelsius, "Temperature must be sampled")
        assertTrue(metrics.temperatureCelsius!! > 0.0, "Temperature must be positive")
    }

    @Test
    fun testHardwareDetectionReturnsSpecs() {
        val collector = DefaultSystemMetricsCollector()
        val specs = collector.detectHardwareSpecs()

        assertNotNull(specs)
        assertTrue(specs.cpuCores > 0, "CPU cores must be > 0")
        assertTrue(specs.totalRamMb > 0, "Total RAM must be > 0")
        assertNotNull(collector.detectOsType(), "OS type must be detected")
        assertNotNull(collector.getHostname(), "Hostname must not be null")
    }

    @Test
    fun testDaemonRegistrationAndLifecycleStartStop() {
        val config = DaemonConfig(nodeId = "test-node-01", heartbeatIntervalMs = 100L)
        val collector = DefaultSystemMetricsCollector()
        val client = MockControlServerClient()
        val manager = DaemonLifecycleManager(config, collector, client)

        assertEquals(DaemonState.INITIALIZING, manager.state)

        val started = manager.start()
        assertTrue(started, "Daemon should successfully start and register")
        assertEquals(DaemonState.RUNNING, manager.state)
        assertEquals("test-node-01", client.registeredNodeId)

        // Wait for heartbeat cycle
        Thread.sleep(250L)
        assertTrue(manager.heartbeatCount > 0, "Should have performed at least one heartbeat")
        assertNotNull(manager.lastSentHeartbeat)
        assertEquals("test-node-01", manager.lastSentHeartbeat?.nodeId)
        assertNotNull(manager.lastSentHeartbeat?.metrics)

        manager.stop()
        assertEquals(DaemonState.STOPPED, manager.state)
    }

    @Test
    fun testIdleResourceFootprintVerification() {
        val config = DaemonConfig(maxIdleCpuPercent = 5.0, maxIdleRamMb = 500L)
        val collector = DefaultSystemMetricsCollector()
        val client = MockControlServerClient()
        val manager = DaemonLifecycleManager(config, collector, client)

        assertTrue(manager.verifyIdleFootprint(), "Idle footprint check should succeed under standard thresholds")
    }

    @Test
    fun testServerRegistrationFailureTransitionsToError() {
        val config = DaemonConfig(nodeId = "fail-node")
        val collector = DefaultSystemMetricsCollector()
        val failingClient = MockControlServerClient(shouldFail = true)
        val manager = DaemonLifecycleManager(config, collector, failingClient)

        val started = manager.start()
        assertFalse(started, "Daemon should fail to start when server is unreachable")
        assertEquals(DaemonState.ERROR, manager.state)
    }
}
