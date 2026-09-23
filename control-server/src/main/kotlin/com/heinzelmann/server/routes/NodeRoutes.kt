package com.heinzelmann.server.routes

import com.heinzelmann.server.models.*
import com.heinzelmann.server.registry.NodeRegistry
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.nodeRoutes(registry: NodeRegistry) {
    route("/api/nodes") {
        post("/register") {
            val request = runCatching { call.receive<NodeRegistrationRequest>() }.getOrElse {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid registration payload"))
                return@post
            }

            val state = registry.registerNode(request)
            call.respond(
                HttpStatusCode.OK,
                NodeRegistrationResponse(
                    nodeId = state.nodeId,
                    status = NodeStatus.ONLINE,
                    registeredAt = state.registeredAt,
                    message = "Node registered successfully"
                )
            )
        }

        post("/{id}/heartbeat") {
            val nodeId = call.parameters["id"] ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing node id in path"))
                return@post
            }

            val telemetry = runCatching { call.receive<HeartbeatTelemetry>() }.getOrElse {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid telemetry payload"))
                return@post
            }

            val telemetryWithNodeId = if (telemetry.nodeId.isBlank()) {
                telemetry.copy(nodeId = nodeId)
            } else {
                telemetry
            }

            val updated = registry.recordHeartbeat(telemetryWithNodeId)
            if (updated == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Node '$nodeId' is not registered"))
                return@post
            }

            val status = registry.getEffectiveStatus(nodeId)
            call.respond(
                HttpStatusCode.OK,
                HeartbeatResponse(
                    nodeId = nodeId,
                    status = status,
                    acknowledgedAt = System.currentTimeMillis()
                )
            )
        }

        get {
            val now = System.currentTimeMillis()
            val allNodes = registry.getAllNodes(now).map { node ->
                mapOf(
                    "nodeId" to node.nodeId,
                    "osType" to node.osType,
                    "hardwareSpecs" to node.hardwareSpecs,
                    "status" to node.currentStatus(now, registry.heartbeatTimeoutMillis).name,
                    "registeredAt" to node.registeredAt,
                    "lastHeartbeatAt" to node.lastHeartbeatAt,
                    "latestTelemetry" to node.latestTelemetry
                )
            }
            call.respond(HttpStatusCode.OK, allNodes)
        }

        get("/{id}") {
            val nodeId = call.parameters["id"] ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing node id"))
                return@get
            }
            val now = System.currentTimeMillis()
            val node = registry.getNode(nodeId, now)
            if (node == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Node '$nodeId' not found"))
                return@get
            }
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "nodeId" to node.nodeId,
                    "osType" to node.osType,
                    "hardwareSpecs" to node.hardwareSpecs,
                    "status" to node.currentStatus(now, registry.heartbeatTimeoutMillis).name,
                    "registeredAt" to node.registeredAt,
                    "lastHeartbeatAt" to node.lastHeartbeatAt,
                    "latestTelemetry" to node.latestTelemetry
                )
            )
        }
    }
}
