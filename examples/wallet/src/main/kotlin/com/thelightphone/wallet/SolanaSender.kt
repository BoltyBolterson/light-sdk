package com.thelightphone.wallet

import java.net.HttpURLConnection
import java.net.URI
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.sol4k.Connection
import org.sol4k.Keypair
import org.sol4k.PublicKey
import org.sol4k.Transaction
import org.sol4k.api.Commitment
import org.sol4k.instruction.TransferInstruction

private val SEND_COMMITMENT = Commitment.CONFIRMED
private const val SYSTEM_ACCOUNT_BYTES = 0
private const val CONNECT_TIMEOUT_MS = 10_000
private const val READ_TIMEOUT_MS = 15_000
private val json = Json { ignoreUnknownKeys = true }

internal class SolanaSender(private val rpcUrl: String) {

    fun send(privateKey: ByteArray, destination: String, lamports: Long): String {
        val keypair = Keypair.fromSecretKey(privateKey)
        val connection = Connection(rpcUrl, SEND_COMMITMENT)
        val rentExemptMinimum = connection.getMinimumBalanceForRentExemption(SYSTEM_ACCOUNT_BYTES)
        val balance = connection.getBalance(keypair.publicKey).longValueExact()

        when (val affordability = affordSend(balance, lamports, LAMPORTS_PER_SIGNATURE, rentExemptMinimum)) {
            SendAffordability.Affordable -> Unit
            is SendAffordability.ShortBy ->
                refuse(keypair.publicKey, lamports, rentExemptMinimum, affordability)
            is SendAffordability.StrandsRent -> error(
                "Sending that much would leave too little behind to keep the account open. " +
                    "Send at most ${SolanaAmount.format(affordability.maxKeepingAccount)} SOL, " +
                    "or ${SolanaAmount.format(affordability.maxDraining)} SOL to empty the wallet.",
            )
        }

        val transaction = Transaction(
            connection.getLatestBlockhash(),
            TransferInstruction(keypair.publicKey, PublicKey(destination), lamports),
            keypair.publicKey,
        )
        transaction.sign(keypair)
        return parseSendSignature(post(sendTransactionRequest(transaction.serialize())))
    }

    private fun refuse(
        owner: PublicKey,
        lamports: Long,
        rentExemptMinimum: Long,
        short: SendAffordability.ShortBy,
    ): Nothing {
        val processed = Connection(rpcUrl, Commitment.PROCESSED).getBalance(owner).longValueExact()
        if (affordSend(processed, lamports, LAMPORTS_PER_SIGNATURE, rentExemptMinimum) ==
            SendAffordability.Affordable
        ) {
            error("That SOL has landed but is still confirming. Try again in a few seconds.")
        }
        error(
            "Not enough SOL. You are ${SolanaAmount.format(short.lamports)} SOL short, " +
                "including the ${SolanaAmount.format(LAMPORTS_PER_SIGNATURE)} SOL network fee.",
        )
    }

    private fun post(body: String): String {
        val http = URI(rpcUrl).toURL().openConnection() as HttpURLConnection
        return try {
            http.requestMethod = "POST"
            http.connectTimeout = CONNECT_TIMEOUT_MS
            http.readTimeout = READ_TIMEOUT_MS
            http.doOutput = true
            http.setRequestProperty("Content-Type", "application/json")
            http.outputStream.use { it.write(body.toByteArray()) }
            val stream = if (http.responseCode in 200..299) http.inputStream else http.errorStream
            stream?.use { it.readBytes().decodeToString() } ?: error("The network gave no response.")
        } finally {
            http.disconnect()
        }
    }
}

/** sol4k omits preflightCommitment, so its own send would preflight against finalized state. */
internal fun sendTransactionRequest(signed: ByteArray): String = buildJsonObject {
    put("jsonrpc", "2.0")
    put("id", 0)
    put("method", "sendTransaction")
    put(
        "params",
        buildJsonArray {
            add(Base64.getEncoder().encodeToString(signed))
            add(
                buildJsonObject {
                    put("encoding", "base64")
                    put("preflightCommitment", SEND_COMMITMENT.toString())
                },
            )
        },
    )
}.toString()

internal fun parseSendSignature(body: String): String {
    val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
        ?: error("The network gave an unreadable response.")
    (root["error"] as? JsonObject)?.let { failure ->
        error(failure["message"]?.jsonPrimitive?.contentOrNull ?: "The network refused the transaction.")
    }
    return root["result"]?.jsonPrimitive?.contentOrNull ?: error("The network returned no signature.")
}
