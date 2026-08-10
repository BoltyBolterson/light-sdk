package com.thelightphone.wallet

import com.thelightphone.wallet.restore.SolanaScanRpc
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException

internal class SolanaBalanceReader(
    private val rpcUrl: String,
    private val client: HttpClient = defaultClient(),
) {

    /** Zero is an answer, null is not. */
    suspend fun lamports(address: String): Long? = try {
        SolanaScanRpc.parseBalance(
            client.post(rpcUrl) {
                contentType(ContentType.Application.Json)
                setBody(SolanaScanRpc.balanceRequest(address))
            }.bodyAsText(),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
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
