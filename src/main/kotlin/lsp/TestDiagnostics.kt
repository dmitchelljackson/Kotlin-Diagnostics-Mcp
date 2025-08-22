package kotlindiagnosticsmcp.lsp

import mu.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.eclipse.lsp4j.services.LanguageServer
import kotlindiagnosticsmcp.lsp.FileDiagnostics
import org.eclipse.lsp4j.TextDocumentItem
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.DocumentDiagnosticParams
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Test runner for the Kotlin diagnostics tool
 */
object TestDiagnostics {
    private val logger = KotlinLogging.logger {}

    @JvmStatic
    fun main(args: Array<String>) {
        val testFilePath = "/Users/mitchell/Workspace/Kotlin-Diagnostics-Mcp/src/main/kotlin/DiagnosticsMcpServer.kt"

        logger.info { "Starting Kotlin LSP diagnostics test" }
        println("Starting Kotlin LSP diagnostics test")

        logger.info { "Testing file: $testFilePath" }
        println("Testing file: $testFilePath")

        runBlocking {
            try {
                // Create and initialize the external LSP server
                logger.info { "Creating and initializing external Kotlin LSP server..." }
                println("Creating and initializing external Kotlin LSP server...")

                val (externalLanguageServer, process) = KotlinLanguageServer.createAndInitialize()

                logger.info { "External LSP server initialized successfully" }
                println("External LSP server initialized successfully")

                logger.info { "Process ID: ${process.pid()}" }
                println("Process ID: ${process.pid()}")

                logger.info { "Process alive: ${process.isAlive}" }
                println("Process alive: ${process.isAlive}")

                // Test the diagnostics using the external LSP server
                logger.info { "Running diagnostics analysis using external Kotlin LSP server..." }
                println("Running diagnostics analysis using external Kotlin LSP server...")

                val results = analyzeFilesWithExternalLsp(externalLanguageServer, listOf(testFilePath))

                logger.info { "Analysis completed. Found ${results.size} results" }
                println("Analysis completed. Found ${results.size} results")

                // Print results
                results.forEach { result ->
                    when (result) {
                        is FileDiagnostics.Success -> {
                            val message = "✅ Success: ${result.filePath} - ${result.diagnostics.size} diagnostics"
                            logger.info { message }
                            println(message)

                            result.diagnostics.forEach { diagnostic ->
                                val diagMessage =
                                    "   - ${diagnostic.severity}: ${diagnostic.message} at line ${diagnostic.range.start.line + 1}"
                                logger.info { diagMessage }
                                println(diagMessage)
                            }
                        }

                        is FileDiagnostics.Error -> {
                            val message = "❌ Error: ${result.filePath} - ${result.message}"
                            logger.error { message }
                            println(message)
                        }
                    }
                }

                logger.info { "Test completed successfully" }
                println("Test completed successfully")

                // Clean shutdown
                logger.info { "Shutting down external LSP server..." }
                println("Shutting down external LSP server...")

                if (process.isAlive) {
                    externalLanguageServer.shutdown().get()
                    externalLanguageServer.exit()
                    process.destroyForcibly()
                }

                logger.info { "External LSP server shut down" }
                println("External LSP server shut down")

            } catch (e: Exception) {
                val errorMessage = "Test failed with error: ${e.message}"
                logger.error { errorMessage }
                println(errorMessage)
                e.printStackTrace()
                System.exit(1)
            }
        }

        logger.info { "Test runner completed" }
        println("Test runner completed")
    }

    /**
     * Analyze files using the external LSP server
     */
    private suspend fun analyzeFilesWithExternalLsp(
        languageServer: LanguageServer,
        filePaths: List<String>
    ): List<FileDiagnostics> {
        val results = mutableListOf<FileDiagnostics>()

        for (filePath in filePaths) {
            try {
                val path = Paths.get(filePath)
                if (!Files.exists(path)) {
                    results.add(FileDiagnostics.Error(filePath, "File not found"))
                    continue
                }

                if (!filePath.endsWith(".kt") && !filePath.endsWith(".kts")) {
                    results.add(FileDiagnostics.Error(filePath, "Not a Kotlin file"))
                    continue
                }

                logger.info { "Analyzing file with external LSP: $filePath" }
                println("Analyzing file with external LSP: $filePath")

                // Read file content
                val content = Files.readString(path)
                val uri = path.toUri().toString()

                // Open the document in the external LSP server
                val textDocumentItem = TextDocumentItem(uri, "kotlin", 1, content)
                val didOpenParams = DidOpenTextDocumentParams(textDocumentItem)
                languageServer.textDocumentService.didOpen(didOpenParams)

                // Request diagnostics from the external LSP server
                val diagnosticParams = DocumentDiagnosticParams(TextDocumentIdentifier(uri))
                val diagnosticResult = languageServer.textDocumentService.diagnostic(diagnosticParams).get()

                val diagnostics = when {
                    diagnosticResult.left != null -> diagnosticResult.left.items ?: emptyList()
                    else -> emptyList()
                }

                results.add(FileDiagnostics.Success(filePath, diagnostics))

                val completionMessage = "Analysis complete for: $filePath - found ${diagnostics.size} diagnostics"
                logger.info { completionMessage }
                println(completionMessage)

            } catch (e: Exception) {
                val errorMessage = "Error analyzing file $filePath: ${e.message}"
                logger.error { errorMessage }
                println(errorMessage)
                e.printStackTrace()
                results.add(FileDiagnostics.Error(filePath, "Error analyzing: ${e.message}"))
            }
        }

        return results
    }
}