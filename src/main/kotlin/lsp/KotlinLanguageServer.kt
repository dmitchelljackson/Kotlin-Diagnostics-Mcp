package kotlindiagnosticsmcp.lsp

import org.eclipse.lsp4j.DiagnosticRegistrationOptions
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.InitializedParams
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.CompletionOptions
import org.eclipse.lsp4j.TextDocumentSyncKind
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.TextDocumentService
import org.eclipse.lsp4j.services.WorkspaceService
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.TextDocumentItem
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.ClientCapabilities
import org.eclipse.lsp4j.TextDocumentClientCapabilities
import org.eclipse.lsp4j.DiagnosticCapabilities
import java.util.concurrent.CompletableFuture
import java.nio.file.Files
import java.nio.file.Paths
import kotlinx.coroutines.*
import java.util.*

class KotlinLanguageServer : LanguageServer {
    private val textDocumentService = DiagnosticsTextDocumentService()
    private val workspaceService = DiagnosticsWorkspaceService()

    companion object {
        /**
         * Creates and initializes a new KotlinLanguageServer instance by launching external kotlin-lsp
         */
        suspend fun createAndInitialize(rootPath: String = System.getProperty("user.dir")): Pair<LanguageServer, Process> {
            // Launch external kotlin-lsp process in stdio mode
            val processBuilder = ProcessBuilder("kotlin-lsp", "--stdio")
            processBuilder.directory(java.io.File(rootPath))
            val process = processBuilder.start()

            // Create a client to connect to the external LSP server
            val launcher = org.eclipse.lsp4j.launch.LSPLauncher.createClientLauncher(
                KotlinLanguageServerClient(),
                process.inputStream,
                process.outputStream
            )

            val languageServer = launcher.remoteProxy
            launcher.startListening()

            // Initialize the external LSP server
            val initParams = InitializeParams().apply {
                rootUri = "file://$rootPath"
                capabilities = ClientCapabilities().apply {
                    textDocument = TextDocumentClientCapabilities().apply {
                        diagnostic = DiagnosticCapabilities()
                    }
                }
            }

            languageServer.initialize(initParams).get()
            languageServer.initialized(InitializedParams())

            return Pair(languageServer, process)
        }
    }

    override fun initialize(params: InitializeParams): CompletableFuture<InitializeResult> {
        val capabilities = ServerCapabilities().apply {
            setTextDocumentSync(TextDocumentSyncKind.Full)
            diagnosticProvider = DiagnosticRegistrationOptions()
        }

        return CompletableFuture.completedFuture(
            InitializeResult(capabilities)
        )
    }

    override fun initialized(params: InitializedParams) {
        // Server is ready
    }

    override fun shutdown(): CompletableFuture<Any> {
        return CompletableFuture.completedFuture(null)
    }

    override fun exit() {
        // Clean exit
    }

    override fun getTextDocumentService(): TextDocumentService = textDocumentService
    override fun getWorkspaceService(): WorkspaceService = workspaceService

    /**
     * Analyze a list of Kotlin files or directories and return diagnostic results
     */
    suspend fun analyzeFiles(filePaths: List<String>): List<FileDiagnostics> {
        val results = mutableListOf<FileDiagnostics>()

        for (filePath in filePaths) {
            try {
                val path = Paths.get(filePath)
                if (!Files.exists(path)) {
                    results.add(FileDiagnostics.Error(filePath, "File/directory not found"))
                    continue
                }

                val kotlinFiles = if (Files.isDirectory(path)) {
                    // Find all .kt and .kts files iteratively
                    val foundFiles = mutableListOf<java.nio.file.Path>()
                    val dirsToProcess = ArrayDeque<java.nio.file.Path>()
                    dirsToProcess.add(path)

                    while (dirsToProcess.isNotEmpty()) {
                        val currentDir = dirsToProcess.removeFirst()
                        try {
                            Files.list(currentDir).use { stream ->
                                stream.forEach { file ->
                                    when {
                                        Files.isDirectory(file) -> dirsToProcess.add(file)
                                        Files.isRegularFile(file) &&
                                                (file.toString().endsWith(".kt") || file.toString().endsWith(".kts")) ->
                                            foundFiles.add(file)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Skip directories we can't read
                        }
                    }
                    foundFiles
                } else {
                    if (!filePath.endsWith(".kt") && !filePath.endsWith(".kts")) {
                        results.add(FileDiagnostics.Error(filePath, "Skipping non-Kotlin file"))
                        continue
                    }
                    listOf(path)
                }

                if (kotlinFiles.isEmpty()) {
                    results.add(FileDiagnostics.Error(filePath, "No Kotlin files found"))
                    continue
                }

                // Process files in batches of 10 using coroutines
                kotlinFiles.chunked(10).forEach { batch ->
                    val batchResults = coroutineScope {
                        batch.map { kotlinFile ->
                            async {
                                analyzeKotlinFile(kotlinFile)
                            }
                        }.awaitAll()
                    }

                    results.addAll(batchResults)
                }
            } catch (e: Exception) {
                results.add(FileDiagnostics.Error(filePath, "Error analyzing: ${e.message}"))
            }
        }

        return results
    }

    /**
     * Analyze a single Kotlin file and return diagnostic result
     */
    private suspend fun analyzeKotlinFile(kotlinFile: java.nio.file.Path): FileDiagnostics {
        return try {
            val content = Files.readString(kotlinFile)
            val uri = kotlinFile.toUri().toString()
            val relativePath = kotlinFile.toString()

            // Open the document in the text document service
            val didOpenParams = DidOpenTextDocumentParams(
                TextDocumentItem(uri, "kotlin", 1, content)
            )
            textDocumentService.didOpen(didOpenParams)

            // Get diagnostics
            val diagnostics = textDocumentService.getDiagnostics(uri)

            FileDiagnostics.Success(relativePath, diagnostics)
        } catch (e: Exception) {
            FileDiagnostics.Error(kotlinFile.toString(), "Error analyzing: ${e.message}")
        }
    }
}

/**
 * Sealed interface representing diagnostics results for a single file
 */
sealed interface FileDiagnostics {
    val filePath: String

    data class Success(
        override val filePath: String,
        val diagnostics: List<Diagnostic>
    ) : FileDiagnostics

    data class Error(
        override val filePath: String,
        val message: String
    ) : FileDiagnostics
}