package cluster.admin.viewmodel

import cluster.admin.client.AdminDashboardClient
import cluster.admin.model.*
import cluster.admin.ui.NodeStatusColor
import cluster.admin.ui.NodesTabRenderer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NodeStatusViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun testInitialLoadingAndNodeListing() = testScope.runTest {
        val client = AdminDashboardClient()
        val node1 = NodeInfo(
            id = "node-macos-01",
            hostname = "macbook-pro.local",
            os = OsType.MACOS,
            status = NodeStatus.ONLINE,
            metrics = NodeHardwareMetrics(
                cpuUsagePercent = 25.4,
                ramUsedMb = 8192,
                ramTotalMb = 16384,
                batteryPercent = 95.0,
                isAcConnected = true
            ),
            lastHeartbeatEpochMs = System.currentTimeMillis()
        )
        val node2 = NodeInfo(
            id = "node-win-02",
            hostname = "desktop-win11.local",
            os = OsType.WINDOWS,
            status = NodeStatus.BUSY,
            metrics = NodeHardwareMetrics(
                cpuUsagePercent = 88.2,
                ramUsedMb = 24576,
                ramTotalMb = 32768,
                batteryPercent = null,
                isAcConnected = true
            ),
            lastHeartbeatEpochMs = System.currentTimeMillis(),
            activeJobId = "job-llm-1029"
        )
        client.registerNodeLocally(node1)
        client.registerNodeLocally(node2)

        val viewModel = NodeStatusViewModel(
            apiClient = client,
            coroutineScope = this,
            pollIntervalMs = 2000L
        )

        viewModel.refreshNodes()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.nodes.size)
        assertEquals(2, state.summary.totalNodes)
        assertEquals(1, state.summary.onlineNodes)
        assertEquals(1, state.summary.busyNodes)
        assertEquals(0, state.summary.offlineNodes)
        assertTrue(state.summary.avgCpuUsagePercent > 50.0)

        val rows = NodesTabRenderer.formatNodeRows(state.nodes)
        assertEquals(2, rows.size)
        assertEquals("ONLINE", rows[0].statusLabel)
        assertEquals(NodeStatusColor.ONLINE_HEX, rows[0].statusColor)
        assertEquals("BUSY", rows[1].statusLabel)
        assertEquals(NodeStatusColor.BUSY_HEX, rows[1].statusColor)
        assertEquals("job-llm-1029", rows[1].activeJob)

        viewModel.stopPolling()
    }

    @Test
    fun testReactiveStatusUpdatesWithinFiveSeconds() = testScope.runTest {
        val client = AdminDashboardClient()
        val node = NodeInfo(
            id = "node-01",
            hostname = "worker.cluster",
            os = OsType.LINUX,
            status = NodeStatus.ONLINE,
            metrics = NodeHardwareMetrics(cpuUsagePercent = 10.0, ramUsedMb = 2048, ramTotalMb = 8192),
            lastHeartbeatEpochMs = System.currentTimeMillis()
        )
        client.registerNodeLocally(node)

        val viewModel = NodeStatusViewModel(
            apiClient = client,
            coroutineScope = this,
            pollIntervalMs = 3000L
        )

        viewModel.refreshNodes()
        assertEquals(NodeStatus.ONLINE, viewModel.uiState.value.nodes.first().status)

        client.updateNodeMetrics(
            nodeId = "node-01",
            metrics = NodeHardwareMetrics(cpuUsagePercent = 0.0, ramUsedMb = 0, ramTotalMb = 8192),
            status = NodeStatus.OFFLINE
        )

        viewModel.refreshNodes()
        val updatedState = viewModel.uiState.value
        assertEquals(NodeStatus.OFFLINE, updatedState.nodes.first().status)
        assertEquals(1, updatedState.summary.offlineNodes)
        assertEquals(0, updatedState.summary.onlineNodes)

        val rows = NodesTabRenderer.formatNodeRows(updatedState.nodes)
        assertEquals(NodeStatusColor.OFFLINE_HEX, rows[0].statusColor)

        viewModel.stopPolling()
    }
}
