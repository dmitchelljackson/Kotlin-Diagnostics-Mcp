package kotlindiagnosticsmcp

import kotlin.system.exitProcess

suspend fun main() {
    try {
        val mcpServer = DiagnosticsMcpServer()
        mcpServer.start()
    } catch (e: Exception) {
        println("Failed to start MCP server: ${e.message}")
        e.printStackTrace()
        exitProcess(1)
    }
}