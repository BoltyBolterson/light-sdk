package com.thelightphone.wallet.restore

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException

/** One candidate address the scan will check, and the derivation path it came from. */
data class ScanTarget(val address: String, val pathLabel: String)

data class ScanResult(val target: ScanTarget, val status: AddressStatus)

/** Probes finished out of probes planned, for the progress bar. */
data class ScanProgressUpdate(val done: Int, val total: Int, val results: List<ScanResult>)

/**
 * Walks the candidate addresses one request at a time and reports what each one holds.
 *
 * Deliberately unbatched: a request per address per token program. It is the slower of the two designs
 * we built, and the one we kept, because a response that covers exactly one address cannot mix an
 * outage on someone else's read into this address's answer.
 *
 * The scanner never decides anything itself. It gathers reads, hands them to [classifyAddress], and
 * passes the verdict through — so the rule that unknown never becomes empty lives in one tested place.
 */
class RestoreScanner(
    private val rpcUrl: String,
    private val client: HttpClient = defaultClient(),
) {

    /**
     * Check every target, emitting [onProgress] after each probe so the caller can stream results
     * instead of waiting for the sweep. Results arrive in target order.
     */
    suspend fun scan(
        targets: List<ScanTarget>,
        onProgress: (ScanProgressUpdate) -> Unit = {},
    ): List<ScanResult> {
        // Planned up front so the bar has a fixed denominator: one balance read per address, plus a
        // token read per program. Skipped probes are credited as they are skipped, never left hanging.
        val probesTotal = targets.size * (1 + SolanaScanRpc.TOKEN_PROGRAM_IDS.size)
        var done = 0
        val results = mutableListOf<ScanResult>()

        for (target in targets) {
            val lamports = SolanaScanRpc.parseBalance(rpc(SolanaScanRpc.balanceRequest(target.address)))
            done++

            val hasTokens = if (needsTokenRead(lamports)) {
                val perProgram = SolanaScanRpc.TOKEN_PROGRAM_IDS.map { programId ->
                    SolanaScanRpc.parseHasTokenBalance(
                        rpc(SolanaScanRpc.tokenAccountsRequest(target.address, programId)),
                    ).also { done++ }
                }
                SolanaScanRpc.hasAnyTokenBalance(perProgram)
            } else {
                // Already Unknown, or already Funded. The token reads cannot change either verdict, so
                // they are skipped and their share of the bar is credited immediately.
                done += SolanaScanRpc.TOKEN_PROGRAM_IDS.size
                null
            }

            results += ScanResult(target, classifyAddress(lamports, hasTokens))
            onProgress(ScanProgressUpdate(done, probesTotal, results.toList()))
        }
        return results
    }

    /** Post one JSON-RPC call. Any failure returns an empty body, which every parser reads as unknown. */
    private suspend fun rpc(body: String): String = try {
        client.post(rpcUrl) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.bodyAsText()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        ""
    }

    fun close() {
        client.close()
    }

    companion object {
        fun defaultClient(): HttpClient = HttpClient(OkHttp) {
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000
                requestTimeoutMillis = 15_000
            }
        }
    }
}
