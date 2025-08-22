package kotlindiagnosticsmcp

import io.ktor.utils.io.streams.asInput
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import kotlinx.coroutines.*
import kotlinx.io.asSink
import kotlinx.io.buffered
import org.eclipse.lsp4j.services.LanguageServer
import kotlindiagnosticsmcp.lsp.KotlinLanguageServer
import kotlin.system.exitProcess
import kotlindiagnosticsmcp.tools.addAnalyzeFilesTool

class DiagnosticsMcpServer {
    private lateinit var server: Server
    private var lspServerDeferred: Deferred<LanguageServer>? = null
    private var lspProcess: Process? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun start() {
        // Initialize LSP server in background
        val lspDeferred = coroutineScope.async {
            KotlinLanguageServer.createAndInitialize()
        }

        lspServerDeferred = coroutineScope.async {
            val (languageServer, process) = lspDeferred.await()
            lspProcess = process
            languageServer
        }

        // Set up shutdown hook
        Runtime.getRuntime().addShutdownHook(Thread {
            runBlocking {
                shutdown()
            }
        })

        // Create MCP server
        server = Server(
            serverInfo = Implementation(
                name = "kotlin-diagnostics-mcp",
                version = "1.0.0"
            ),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = true)
                )
            )
        )

        // Add the analyze-files tool
        lspServerDeferred?.let { deferred ->
            server.addAnalyzeFilesTool(deferred)
        }

        // Connect with stdio transport
        val transport = StdioServerTransport(
            inputStream = System.`in`.asInput(),
            outputStream = System.out.asSink().buffered()
        )
        server.connect(transport)

        println("Kotlin Diagnostics MCP Server started successfully")
    }

    private suspend fun shutdown() {
        try {
            lspServerDeferred?.let { deferred ->
                if (deferred.isCompleted) {
                    deferred.await().shutdown().get()
                    deferred.await().exit()
                }
            }
            lspProcess?.let { process ->
                if (process.isAlive) {
                    process.destroyForcibly()
                }
            }
            coroutineScope.cancel()
            println("Kotlin Diagnostics MCP Server shutdown complete")
        } catch (e: Exception) {
            println("Error during shutdown: ${e.message}")
        }
    }
}

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