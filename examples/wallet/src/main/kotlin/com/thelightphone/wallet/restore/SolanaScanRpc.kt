package com.thelightphone.wallet.restore

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

object SolanaScanRpc {

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

    /** Zero is an answer, null is not. */
    fun parseBalance(body: String): Long? {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        if (root["error"].isRealError()) return null
        return runCatching { root["result"]!!.jsonObject["value"]!!.jsonPrimitive.long }.getOrNull()
    }

    /** Answers for one token program. An entry we cannot read makes the whole response unknown. */
    fun parseHasTokenBalance(body: String): Boolean? {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        if (root["error"].isRealError()) return null
        val value = runCatching { root["result"]!!.jsonObject["value"]!!.jsonArray }.getOrNull() ?: return null
        var held = false
        for (entry in value) {
            val amount = entry.rawTokenAmount() ?: return null
            if (amount > 0L) held = true
        }
        return held
    }

    fun hasAnyTokenBalance(perProgram: Collection<Boolean?>): Boolean? = when {
        perProgram.any { it == true } -> true
        perProgram.any { it == null } -> null
        perProgram.isEmpty() -> null
        else -> false
    }

    private fun JsonElement?.isRealError(): Boolean = this != null && this !is JsonNull

    private fun JsonElement.rawTokenAmount(): Long? = runCatching {
        jsonObject["account"]!!.jsonObject["data"]!!.jsonObject["parsed"]!!
            .jsonObject["info"]!!.jsonObject["tokenAmount"]!!.jsonObject["amount"]!!
            .jsonPrimitive.content.toLong()
    }.getOrNull()
}
