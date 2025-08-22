package kotlindiagnosticsmcp.lsp

import mu.KotlinLogging
import org.eclipse.lsp4j.MessageActionItem
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.ShowMessageRequestParams
import org.eclipse.lsp4j.services.LanguageClient
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

/**
 * Language client implementation for communicating with the external kotlin-lsp server
 */
class KotlinLanguageServerClient : LanguageClient {

    override fun telemetryEvent(params: Any?) {
        logger.debug { "Received telemetry event: $params" }
    }

    override fun publishDiagnostics(params: PublishDiagnosticsParams?) {
        logger.debug { "Published diagnostics for URI: ${params?.uri} with ${params?.diagnostics?.size ?: 0} diagnostics" }

        params?.diagnostics?.forEach { diagnostic ->
            logger.trace { "Diagnostic: ${diagnostic.severity} at line ${diagnostic.range.start.line} - ${diagnostic.message}" }
        }
    }

    override fun showMessage(params: MessageParams?) {
        when (params?.type?.value) {
            1 -> logger.error { "LSP Server Error: ${params.message}" }
            2 -> logger.warn { "LSP Server Warning: ${params.message}" }
            3 -> logger.info { "LSP Server Info: ${params.message}" }
            4 -> logger.debug { "LSP Server Log: ${params.message}" }
            else -> logger.info { "LSP Server Message: ${params?.message}" }
        }
    }

    override fun showMessageRequest(params: ShowMessageRequestParams?): CompletableFuture<MessageActionItem> {
        logger.info { "LSP Server Message Request: ${params?.message}" }
        params?.actions?.forEach { action ->
            logger.debug { "Available action: ${action.title}" }
        }
        return CompletableFuture.completedFuture(null)
    }

    override fun logMessage(params: MessageParams?) {
        when (params?.type?.value) {
            1 -> logger.error { "LSP Server Log Error: ${params.message}" }
            2 -> logger.warn { "LSP Server Log Warning: ${params.message}" }
            3 -> logger.info { "LSP Server Log Info: ${params.message}" }
            4 -> logger.debug { "LSP Server Log Debug: ${params.message}" }
            else -> logger.info { "LSP Server Log: ${params?.message}" }
        }
    }
}