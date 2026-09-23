package com.heinzelmann.server.registry

import com.heinzelmann.server.models.HeartbeatTelemetry
import com.heinzelmann.server.models.NodeRegistrationRequest
import com.heinzelmann.server.models.NodeState
import com.heinzelmann.server.models.NodeStatus
import java.util.concurrent.ConcurrentHashMap

class NodeRegistry(
    val heartbeatTimeoutMillis: Long = 60_000L
) {
    private val nodes = ConcurrentHashMap<String, NodeState>()

    fun registerNode(request: NodeRegistrationRequest, now: Long = System.currentTimeMillis()): NodeState {
        val state = NodeState(
            nodeId = request.nodeId,
            osType = request.osType,
            hardwareSpecs = request.hardwareSpecs,
            registeredAt = now,
            lastHeartbeatAt = now,
            latestTelemetry = null,
            manualStatus = null
        )
        nodes[request.nodeId] = state
        return state
    }

    fun recordHeartbeat(telemetry: HeartbeatTelemetry, now: Long = System.currentTimeMillis()): NodeState? {
        val existing = nodes[telemetry.nodeId] ?: return null
        val updated = existing.copy(
            lastHeartbeatAt = now,
            latestTelemetry = telemetry,
            manualStatus = null
        )
        nodes[telemetry.nodeId] = updated
        return updated
    }

    fun getNode(nodeId: String, now: Long = System.currentTimeMillis()): NodeState? {
        val node = nodes[nodeId] ?: return null
        return node
    }

    fun getAllNodes(now: Long = System.currentTimeMillis()): List<NodeState> {
        return nodes.values.toList()
    }

    fun getEffectiveStatus(nodeId: String, now: Long = System.currentTimeMillis()): NodeStatus {
        val node = nodes[nodeId] ?: return NodeStatus.OFFLINE
        return node.currentStatus(now, heartbeatTimeoutMillis)
    }

    fun markNodeStatus(nodeId: String, status: NodeStatus) {
        val existing = nodes[nodeId] ?: return
        nodes[nodeId] = existing.copy(manualStatus = status)
    }

    fun clear() {
        nodes.clear()
    }
}
