package kotlindiagnosticsmcp.lsp

import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.DidSaveTextDocumentParams
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.services.TextDocumentService

class DiagnosticsTextDocumentService : TextDocumentService {
    private val diagnostics = mutableMapOf<String, MutableList<Diagnostic>>()

    override fun didOpen(params: DidOpenTextDocumentParams) {
        analyzeDiagnostics(params.textDocument.uri, params.textDocument.text)
    }

    override fun didChange(params: DidChangeTextDocumentParams) {
        val fullText = params.contentChanges.lastOrNull()?.text ?: return
        analyzeDiagnostics(params.textDocument.uri, fullText)
    }

    override fun didClose(params: DidCloseTextDocumentParams) {
        diagnostics.remove(params.textDocument.uri)
    }

    override fun didSave(params: DidSaveTextDocumentParams) {
        // Re-analyze on save
        if (params.text != null) {
            analyzeDiagnostics(params.textDocument.uri, params.text)
        }
    }

    fun getDiagnostics(uri: String): List<Diagnostic> {
        return diagnostics[uri] ?: emptyList()
    }

    private fun analyzeDiagnostics(uri: String, text: String) {
        val fileDiagnostics = mutableListOf<Diagnostic>()

        // Basic Kotlin syntax analysis
        val lines = text.split("\n")
        lines.forEachIndexed { index, line ->
            // Check for common Kotlin issues
            if (line.contains("var ") && line.contains("= null")) {
                fileDiagnostics.add(
                    Diagnostic(
                        Range(Position(index, 0), Position(index, line.length)),
                        "Consider using nullable type instead of var with null",
                        DiagnosticSeverity.Warning,
                        "kotlin-diagnostics"
                    )
                )
            }

            if (line.contains("!!") && !line.contains("//")) {
                fileDiagnostics.add(
                    Diagnostic(
                        Range(Position(index, line.indexOf("!!")), Position(index, line.indexOf("!!") + 2)),
                        "Unsafe null assertion operator (!!) - consider safe call or explicit null check",
                        DiagnosticSeverity.Warning,
                        "kotlin-diagnostics"
                    )
                )
            }

            if (line.trim().startsWith("import ") && line.contains("*")) {
                fileDiagnostics.add(
                    Diagnostic(
                        Range(Position(index, 0), Position(index, line.length)),
                        "Wildcard imports should be avoided",
                        DiagnosticSeverity.Information,
                        "kotlin-diagnostics"
                    )
                )
            }

            // Check for syntax errors (basic)
            val openBraces = line.count { it == '{' }
            val closeBraces = line.count { it == '}' }
            if (openBraces != closeBraces && line.trim().endsWith("{") && !line.contains("//")) {
                // This is a very basic check - in real implementation you'd use a proper parser
            }
        }

        diagnostics[uri] = fileDiagnostics
    }
}