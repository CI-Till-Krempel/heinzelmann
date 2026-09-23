package de.heinzelmann.daemon

import de.heinzelmann.daemon.client.ControlServerClient
import de.heinzelmann.daemon.client.MockControlServerClient
import de.heinzelmann.daemon.collector.DefaultSystemMetricsCollector
import de.heinzelmann.daemon.collector.SystemMetricsCollector
import de.heinzelmann.daemon.lifecycle.DaemonLifecycleManager
import de.heinzelmann.daemon.models.DaemonConfig
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    println("=== Heinzelmann Client Daemon Starting ===")
    val config = DaemonConfig(
        serverUrl = System.getenv("CONTROL_SERVER_URL") ?: "http://localhost:8080",
        nodeId = System.getenv("NODE_ID") ?: ("node-" + System.currentTimeMillis() % 100000)
    )

    val collector: SystemMetricsCollector = DefaultSystemMetricsCollector()
    val client: ControlServerClient = MockControlServerClient()
    val manager = DaemonLifecycleManager(config, collector, client)

    Runtime.getRuntime().addShutdownHook(Thread {
        println("Shutting down Heinzelmann Client Daemon...")
        manager.stop()
    })

    val started = manager.start()
    if (!started) {
        System.err.println("Failed to start daemon.")
        exitProcess(1)
    }

    println("Daemon successfully started on node ${config.nodeId} (OS: ${collector.detectOsType()})")
}
