package com.thelightphone.wallet

import com.thelightphone.wallet.crypto.Bip39
import java.net.HttpURLConnection
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SolanaSendIntegrationTest {

    private val rpcUrl = System.getenv("SOLANA_TEST_RPC") ?: "http://127.0.0.1:8899"

    private val senderPhrase =
        "legal winner thank year wave sausage worth useful legal winner thank yellow"
    private val destinationPhrase =
        "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"

    private fun rpc(body: String): String? = runCatching {
        val connection = URI(rpcUrl).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 3000
        connection.readTimeout = 8000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.outputStream.use { it.write(body.toByteArray()) }
        connection.inputStream.use { it.readBytes().decodeToString() }
    }.getOrNull()

    private fun validatorIsUp(): Boolean =
        rpc("""{"jsonrpc":"2.0","id":1,"method":"getHealth"}""")?.contains("\"ok\"") == true

    private fun balance(address: String, commitment: String = "finalized"): Long {
        val body = """{"jsonrpc":"2.0","id":1,"method":"getBalance","params":["$address",{"commitment":"$commitment"}]}"""
        val response = rpc(body) ?: return -1
        return Regex("\"value\":(\\d+)").find(response)?.groupValues?.get(1)?.toLong() ?: -1
    }

    private fun privateKeyFor(phrase: String): ByteArray {
        val seed = Bip39.mnemonicToSeed(phrase.split(" "))
        return ChainKeyPair.deriveFrom(seed, Chain.SOLANA).privateKey
    }

    private fun awaitBalance(address: String, atLeast: Long): Long {
        repeat(30) {
            val current = balance(address)
            if (current >= atLeast) return current
            Thread.sleep(1000)
        }
        return balance(address)
    }

    @Test
    fun aRealTransferLandsOnChain() {
        if (!validatorIsUp()) {
            println("SolanaSendIntegrationTest skipped: no validator at $rpcUrl")
            return
        }

        val senderKey = privateKeyFor(senderPhrase)
        val sender = ChainKeyPair.addressFor(Chain.SOLANA, senderKey)
        val destination = ChainKeyPair.addressFor(Chain.SOLANA, privateKeyFor(destinationPhrase))

        rpc("""{"jsonrpc":"2.0","id":1,"method":"requestAirdrop","params":["$sender",2000000000]}""")
        val funded = awaitBalance(sender, 1)
        assertTrue(funded > 0, "airdrop never landed for $sender")

        val before = balance(destination)
        val lamports = SolanaAmount.toLamports("0.25")!!
        val signature = SolanaSender(rpcUrl).send(senderKey, destination, lamports)

        assertTrue(signature.isNotBlank(), "no signature returned")
        val after = awaitBalance(destination, before + lamports)
        assertEquals(before + lamports, after, "destination did not receive exactly the sent amount")
        println("sent $lamports lamports $sender -> $destination sig=$signature")
    }
}
