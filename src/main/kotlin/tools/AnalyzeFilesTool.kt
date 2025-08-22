package kotlindiagnosticsmcp.tools

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.coroutines.Deferred
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.DiagnosticSeverity
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlindiagnosticsmcp.lsp.KotlinLanguageServer
import kotlindiagnosticsmcp.lsp.FileDiagnostics

/**
 * Extension function to add the analyze-files tool to the MCP server
 */
fun Server.addAnalyzeFilesTool(lspServerDeferred: Deferred<LanguageServer>) {
    addTool(
        Tool(
            name = "analyze-files",
            description = "Analyze Kotlin files for errors and warnings using LSP diagnostics",
            inputSchema = Tool.Input(buildJsonObject {
                put("type", JsonPrimitive("object"))
                put("properties", buildJsonObject {
                    put("files", buildJsonObject {
                        put("type", JsonPrimitive("array"))
                        put("items", buildJsonObject {
                            put("type", JsonPrimitive("string"))
                        })
                        put("description", JsonPrimitive("Array of Kotlin file or directory paths to analyze"))
                    })
                })
                put("required", JsonArray(listOf(JsonPrimitive("files"))))
            }),
            outputSchema = null,
            annotations = null
        )
    ) { request ->
        handleAnalyzeFiles(request, lspServerDeferred)
    }
}

/**
 * Handler for the analyze-files tool
 */
private suspend fun handleAnalyzeFiles(
    request: CallToolRequest,
    lspServerDeferred: Deferred<LanguageServer>
): CallToolResult {
    try {
        val arguments = request.arguments as? Map<*, *>
            ?: return CallToolResult(
                content = listOf(
                    TextContent(text = "Error: Invalid arguments format")
                ),
                isError = true
            )

        val files = arguments["files"] as? List<*>
            ?: return CallToolResult(
                content = listOf(
                    TextContent(text = "Error: 'files' parameter is required and must be an array")
                ),
                isError = true
            )

        val filePaths = files.mapNotNull { it as? String }

        if (filePaths.isEmpty()) {
            return CallToolResult(
                content = listOf(
                    TextContent(text = "Error: No valid file paths provided")
                ),
                isError = true
            )
        }

        // Wait for LSP server to be ready
        val lspServer = lspServerDeferred.await()
        val kotlinLanguageServer = lspServer as KotlinLanguageServer

        // Get structured diagnostic results
        val fileDiagnostics = kotlinLanguageServer.analyzeFiles(filePaths)

        // Format results for display
        val formattedResults = formatDiagnostics(fileDiagnostics)

        return CallToolResult(
            content = listOf(
                TextContent(text = formattedResults.joinToString("\n\n"))
            )
        )

    } catch (e: Exception) {
        return CallToolResult(
            content = listOf(
                TextContent(text = "Error: ${e.message}")
            ),
            isError = true
        )
    }
}

/**
 * Format FileDiagnostics into user-friendly text messages
 */
private fun formatDiagnostics(fileDiagnostics: List<FileDiagnostics>): List<String> {
    return fileDiagnostics.map { fileResult ->
        when (fileResult) {
            is FileDiagnostics.Error -> {
                "❌ ${fileResult.filePath}: ${fileResult.message}"
            }
            is FileDiagnostics.Success -> {
                if (fileResult.diagnostics.isEmpty()) {
                    "✅ ${fileResult.filePath}: No issues found"
                } else {
                    val diagnosticLines = mutableListOf<String>()
                    diagnosticLines.add("📄 ${fileResult.filePath}:")
                    fileResult.diagnostics.forEach { diagnostic ->
                        val severity = when (diagnostic.severity) {
                            DiagnosticSeverity.Error -> "❌ ERROR"
                            DiagnosticSeverity.Warning -> "⚠️ WARNING"
                            DiagnosticSeverity.Information -> "ℹ️ INFO"
                            DiagnosticSeverity.Hint -> "💡 HINT"
                            else -> "❓ UNKNOWN"
                        }

                        val line = diagnostic.range.start.line + 1
                        val char = diagnostic.range.start.character + 1
                        diagnosticLines.add("  $severity (Line $line:$char): ${diagnostic.message}")
                    }
                    diagnosticLines.joinToString("\n")
                }
            }
        }
    }
}