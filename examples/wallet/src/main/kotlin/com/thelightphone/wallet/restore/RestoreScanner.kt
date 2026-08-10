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

data class ScanTarget(val address: String, val pathLabel: String)

data class ScanResult(val target: ScanTarget, val status: AddressStatus)

data class ScanProgressUpdate(val done: Int, val total: Int, val results: List<ScanResult>)

class RestoreScanner(
    private val rpcUrl: String,
    private val client: HttpClient = defaultClient(),
) {

    suspend fun scan(
        targets: List<ScanTarget>,
        onProgress: (ScanProgressUpdate) -> Unit = {},
    ): List<ScanResult> {
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
                done += SolanaScanRpc.TOKEN_PROGRAM_IDS.size
                null
            }

            results += ScanResult(target, classifyAddress(lamports, hasTokens))
            onProgress(ScanProgressUpdate(done, probesTotal, results.toList()))
        }
        return results
    }

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
