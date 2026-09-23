package com.heinzelmann.server

import com.heinzelmann.server.models.HardwareSpecs
import com.heinzelmann.server.models.HeartbeatTelemetry
import com.heinzelmann.server.models.NodeRegistrationRequest
import com.heinzelmann.server.models.NodeStatus
import com.heinzelmann.server.registry.NodeRegistry
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class NodeApiTest {

    @Test
    fun `test node registration returns 200 and confirms registration`() = testApplication {
        val registry = NodeRegistry(heartbeatTimeoutMillis = 60_000L)
        application {
            module(registry)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val request = NodeRegistrationRequest(
            nodeId = "macbook-pro-m2-01",
            osType = "macOS",
            hardwareSpecs = HardwareSpecs(
                cpuCores = 12,
                totalMemoryMb = 32768,
                osType = "macOS 14.5",
                arch = "aarch64"
            )
        )

        val response = client.post("/api/nodes/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<JsonObject>()
        assertEquals("macbook-pro-m2-01", body["nodeId"]?.jsonPrimitive?.content)
        assertEquals("ONLINE", body["status"]?.jsonPrimitive?.content)
        assertEquals("Node registered successfully", body["message"]?.jsonPrimitive?.content)

        val nodeInRegistry = registry.getNode("macbook-pro-m2-01")
        assertNotNull(nodeInRegistry)
        assertEquals("macOS", nodeInRegistry.osType)
    }

    @Test
    fun `test node heartbeat updates last seen timestamp and metric cache`() = testApplication {
        val registry = NodeRegistry(heartbeatTimeoutMillis = 60_000L)
        application {
            module(registry)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val registerReq = NodeRegistrationRequest(
            nodeId = "win-laptop-01",
            osType = "Windows",
            hardwareSpecs = HardwareSpecs(
                cpuCores = 8,
                totalMemoryMb = 16384,
                osType = "Windows 11",
                arch = "x86_64"
            )
        )
        client.post("/api/nodes/register") {
            contentType(ContentType.Application.Json)
            setBody(registerReq)
        }

        val telemetry = HeartbeatTelemetry(
            nodeId = "win-laptop-01",
            cpuUsagePercent = 24.5,
            ramUsedMb = 8192,
            ramTotalMb = 16384,
            batteryPercent = 95.0,
            temperatureCelsius = 48.0,
            timestamp = System.currentTimeMillis()
        )

        val response = client.post("/api/nodes/win-laptop-01/heartbeat") {
            contentType(ContentType.Application.Json)
            setBody(telemetry)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<JsonObject>()
        assertEquals("win-laptop-01", body["nodeId"]?.jsonPrimitive?.content)
        assertEquals("ONLINE", body["status"]?.jsonPrimitive?.content)

        val node = registry.getNode("win-laptop-01")
        assertNotNull(node)
        assertEquals(24.5, node.latestTelemetry?.cpuUsagePercent)
        assertEquals(8192, node.latestTelemetry?.ramUsedMb)
    }

    @Test
    fun `test node marked OFFLINE when heartbeat threshold of 60 seconds expires`() = testApplication {
        val shortTimeoutRegistry = NodeRegistry(heartbeatTimeoutMillis = 60_000L)
        application {
            module(shortTimeoutRegistry)
        }

        val baseTime = 1_000_000_000L
        val request = NodeRegistrationRequest(
            nodeId = "offline-test-node",
            osType = "macOS",
            hardwareSpecs = HardwareSpecs(cpuCores = 4, totalMemoryMb = 8192, osType = "macOS", arch = "aarch64")
        )
        shortTimeoutRegistry.registerNode(request, now = baseTime)

        // At baseTime + 30s: still ONLINE
        assertEquals(NodeStatus.ONLINE, shortTimeoutRegistry.getEffectiveStatus("offline-test-node", now = baseTime + 30_000L))

        // At baseTime + 61s: beyond 60s timeout -> marked OFFLINE
        assertEquals(NodeStatus.OFFLINE, shortTimeoutRegistry.getEffectiveStatus("offline-test-node", now = baseTime + 61_000L))
    }

    @Test
    fun `test heartbeat on non-registered node returns 404`() = testApplication {
        val registry = NodeRegistry()
        application {
            module(registry)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val telemetry = HeartbeatTelemetry(
            nodeId = "unknown-node",
            cpuUsagePercent = 10.0,
            ramUsedMb = 1024,
            ramTotalMb = 8192
        )

        val response = client.post("/api/nodes/unknown-node/heartbeat") {
            contentType(ContentType.Application.Json)
            setBody(telemetry)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
