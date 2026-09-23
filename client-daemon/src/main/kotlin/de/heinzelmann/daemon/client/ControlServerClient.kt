package de.heinzelmann.daemon.client

import de.heinzelmann.daemon.models.HeartbeatRequest
import de.heinzelmann.daemon.models.HeartbeatResponse
import de.heinzelmann.daemon.models.NodeRegistrationRequest
import de.heinzelmann.daemon.models.NodeRegistrationResponse

interface ControlServerClient {
    fun registerNode(request: NodeRegistrationRequest): NodeRegistrationResponse
    fun sendHeartbeat(request: HeartbeatRequest): HeartbeatResponse
}

class MockControlServerClient(
    var shouldFail: Boolean = false,
    var registeredNodeId: String? = null
) : ControlServerClient {

    val receivedHeartbeats = mutableListOf<HeartbeatRequest>()

    override fun registerNode(request: NodeRegistrationRequest): NodeRegistrationResponse {
        if (shouldFail) {
            throw RuntimeException("Control server connection refused")
        }
        registeredNodeId = request.nodeId
        return NodeRegistrationResponse(
            nodeId = request.nodeId,
            registered = true,
            heartbeatIntervalSeconds = 5,
            message = "Node successfully registered"
        )
    }

    override fun sendHeartbeat(request: HeartbeatRequest): HeartbeatResponse {
        if (shouldFail) {
            throw RuntimeException("Failed to send heartbeat to control server")
        }
        receivedHeartbeats.add(request)
        return HeartbeatResponse(
            acknowledged = true,
            command = null,
            timestamp = System.currentTimeMillis()
        )
    }
}
