package com.thelightphone.wallet.restore

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

/**
 * Request building and response parsing for the restore scan, with no I/O of any kind. Everything that
 * decides what an address IS lives here so it can be tested directly, the same split
 * `PythPriceClient`/`auditPrice` already uses: the network shell stays dumb, the judgement is pure.
 *
 * One address per request, one request per call. Slower than folding the whole sweep into
 * `getMultipleAccounts` and JSON-RPC batches, and deliberately so — every response stands alone, so a
 * failure can only ever be one address wide.
 *
 * One rule governs every parser below. An address is only ever reported empty when the chain actually
 * said so. An error reply, an unparseable body or a dead host all yield `null` — unknown. Reporting
 * unknown as empty would tell someone restoring a funded seed that their money is gone, and they would
 * believe it.
 */
object SolanaScanRpc {

    /** Both SPL token programs. A wallet's holdings are only fully known once both are read. */
    val TOKEN_PROGRAM_IDS = listOf(
        "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
        "TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb",
    )

    private val json = Json { ignoreUnknownKeys = true }

    fun balanceRequest(address: String): String = request("getBalance") {
        add(address)
    }

    fun tokenAccountsRequest(address: String, programId: String): String =
        request("getTokenAccountsByOwner") {
            add(address)
            add(buildJsonObject { put("programId", programId) })
            add(buildJsonObject { put("encoding", "jsonParsed") })
        }

    private fun request(
        method: String,
        params: kotlinx.serialization.json.JsonArrayBuilder.() -> Unit,
    ): String = buildJsonObject {
        put("jsonrpc", "2.0")
        put("id", 0)
        put("method", method)
        put("params", buildJsonArray(params))
    }.toString()

    /** Lamports held by the address, or null if the read failed. Zero is an answer; null is not. */
    fun parseBalance(body: String): Long? {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        if (root["error"] != null) return null
        return runCatching { root["result"]!!.jsonObject["value"]!!.jsonPrimitive.long }.getOrNull()
    }

    /**
     * Whether this response shows any non-dust token balance, or null if the read failed.
     *
     * Note this answers for ONE token program. A false here is only "holds nothing" for that program —
     * see [hasAnyTokenBalance] for the combined answer across both.
     */
    fun parseHasTokenBalance(body: String): Boolean? {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        if (root["error"] != null) return null
        val value = runCatching { root["result"]!!.jsonObject["value"]!!.jsonArray }.getOrNull() ?: return null
        return value.any { it.uiAmountOrZero() > 0.0 }
    }

    /**
     * Combine the per-program answers into one. Unknown is contagious: if either program's read failed,
     * the address holds an unknown amount, because the tokens we couldn't see may be the only ones there.
     * Only when both programs answered, and both were empty, is "no tokens" a real statement.
     */
    fun hasAnyTokenBalance(perProgram: Collection<Boolean?>): Boolean? = when {
        perProgram.any { it == true } -> true
        perProgram.any { it == null } -> null
        perProgram.isEmpty() -> null
        else -> false
    }

    private fun JsonElement.uiAmountOrZero(): Double = runCatching {
        jsonObject["account"]!!.jsonObject["data"]!!.jsonObject["parsed"]!!
            .jsonObject["info"]!!.jsonObject["tokenAmount"]!!.jsonObject["uiAmount"]!!
            .jsonPrimitive.content.toDouble()
    }.getOrNull() ?: 0.0
}
