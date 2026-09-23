package de.heinzelmann.client.ray

import kotlinx.coroutines.runBlocking
import kotlin.test.*

class RayWorkerManagerTest {

    private class MockContainerEngine : RayContainerEngine {
        val launchedImages = mutableListOf<String>()
        val launchedCommands = mutableListOf<List<String>>()
        val stoppedContainers = mutableListOf<String>()
        var shouldFailLaunch = false
        var containerRunningState = true

        override fun launchContainer(image: String, name: String, command: List<String>, memoryLimit: Long?): String {
            if (shouldFailLaunch) throw RuntimeException("Docker daemon unavailable")
            launchedImages.add(image)
            launchedCommands.add(command)
            return "mock-cid-12345"
        }

        override fun stopContainer(containerId: String, timeoutSeconds: Int): Boolean {
            stoppedContainers.add(containerId)
            return true
        }

        override fun isContainerRunning(containerId: String): Boolean = containerRunningState
    }

    @Test
    fun testRayWorkerConfigBuildCommand() {
        val config = RayWorkerConfig(
            headAddress = "192.168.1.100",
            headPort = 6379,
            redisPassword = "secret-redis-pass",
            numCpus = 4,
            numGpus = 1,
            objectStoreMemoryBytes = 2147483648L
        )

        val cmd = config.buildRayStartCommand()
        assertTrue(cmd.contains("ray"))
        assertTrue(cmd.contains("start"))
        assertTrue(cmd.contains("--address=192.168.1.100:6379"))
        assertTrue(cmd.contains("--block"))
        assertTrue(cmd.contains("--redis-password=secret-redis-pass"))
        assertTrue(cmd.contains("--num-cpus=4"))
        assertTrue(cmd.contains("--num-gpus=1"))
        assertTrue(cmd.contains("--object-store-memory=2147483648"))
    }

    @Test
    fun testStartWorkerSuccess() = runBlocking {
        val mockEngine = MockContainerEngine()
        val manager = RayWorkerManager(mockEngine)

        val config = RayWorkerConfig(headAddress = "10.0.0.1", headPort = 6379)
        val result = manager.startWorker(config)

        assertTrue(result.isSuccess)
        val status = manager.getStatus()
        assertEquals(RayWorkerState.RUNNING, status.state)
        assertEquals("mock-cid-12345", status.containerId)
        assertEquals("10.0.0.1:6379", status.headAddress)
        assertEquals(1, mockEngine.launchedImages.size)
    }

    @Test
    fun testStartWorkerFailure() = runBlocking {
        val mockEngine = MockContainerEngine().apply { shouldFailLaunch = true }
        val manager = RayWorkerManager(mockEngine)

        val config = RayWorkerConfig(headAddress = "10.0.0.1", headPort = 6379)
        val result = manager.startWorker(config)

        assertTrue(result.isFailure)
        val status = manager.getStatus()
        assertEquals(RayWorkerState.FAILED, status.state)
        assertNotNull(status.errorMessage)
    }

    @Test
    fun testStopWorker() = runBlocking {
        val mockEngine = MockContainerEngine()
        val manager = RayWorkerManager(mockEngine)

        manager.startWorker(RayWorkerConfig(headAddress = "10.0.0.1"))
        assertEquals(RayWorkerState.RUNNING, manager.getStatus().state)

        val stopResult = manager.stopWorker()
        assertTrue(stopResult.isSuccess)
        assertEquals(RayWorkerState.STOPPED, manager.getStatus().state)
        assertTrue(mockEngine.stoppedContainers.contains("mock-cid-12345"))
    }

    @Test
    fun testNodeStatusChangeToBusyOrDisconnectedStopsWorker() = runBlocking {
        val mockEngine = MockContainerEngine()
        val manager = RayWorkerManager(mockEngine)

        manager.startWorker(RayWorkerConfig(headAddress = "10.0.0.1"))
        assertEquals(RayWorkerState.RUNNING, manager.getStatus().state)

        // When node status changes to BUSY, worker leaves the Ray cluster cleanly
        val stoppedForBusy = manager.onNodeStatusChanged(NodeClusterStatus.BUSY)
        assertTrue(stoppedForBusy)
        assertEquals(RayWorkerState.STOPPED, manager.getStatus().state)

        // Restart worker
        manager.startWorker(RayWorkerConfig(headAddress = "10.0.0.1"))
        assertEquals(RayWorkerState.RUNNING, manager.getStatus().state)

        // When node status changes to DISCONNECTED, worker leaves the Ray cluster cleanly
        val stoppedForDisconnect = manager.onNodeStatusChanged(NodeClusterStatus.DISCONNECTED)
        assertTrue(stoppedForDisconnect)
        assertEquals(RayWorkerState.STOPPED, manager.getStatus().state)
    }

    @Test
    fun testHealthCheckDetectsExitedContainer() = runBlocking {
        val mockEngine = MockContainerEngine()
        val manager = RayWorkerManager(mockEngine)

        manager.startWorker(RayWorkerConfig(headAddress = "10.0.0.1"))
        assertTrue(manager.checkHealth())

        mockEngine.containerRunningState = false
        assertFalse(manager.checkHealth())
        assertEquals(RayWorkerState.FAILED, manager.getStatus().state)
    }
}
