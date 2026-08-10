package com.thelightphone.wallet

import com.thelightphone.wallet.crypto.Bip39
import java.net.HttpURLConnection
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SplTokenRealTransferTest {

    private val rpcUrl = System.getenv("SOLANA_TEST_RPC") ?: "http://127.0.0.1:8899"
    private val mintAddress = System.getenv("SOLANA_TEST_MINT")

    private val senderPhrase =
        "legal winner thank year wave sausage worth useful legal winner thank yellow"
    private val recipientPhrase =
        "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"

    private fun rpc(body: String): String? = runCatching {
        val connection = URI(rpcUrl).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 3000
        connection.readTimeout = 10000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.outputStream.use { it.write(body.toByteArray()) }
        connection.inputStream.use { it.readBytes().decodeToString() }
    }.getOrNull()

    private fun privateKeyFor(phrase: String): ByteArray {
        val seed = Bip39.mnemonicToSeed(phrase.split(" "))
        return ChainKeyPair.deriveFrom(seed, Chain.SOLANA).privateKey
    }

    private fun tokenBalance(owner: String): String? {
        val body = """{"jsonrpc":"2.0","id":1,"method":"getTokenAccountsByOwner","params":["$owner",{"mint":"$mintAddress"},{"encoding":"jsonParsed","commitment":"confirmed"}]}"""
        val response = rpc(body) ?: return null
        return Regex("\"uiAmountString\":\"([0-9.]+)\"").find(response)?.groupValues?.get(1)
    }

    @Test
    fun anAllowListedTokenActuallyTransfersOnChain() {
        val mint = mintAddress
        if (mint == null) {
            println("SplTokenRealTransferTest skipped: set SOLANA_TEST_MINT to a funded local mint")
            return
        }
        if (rpc("""{"jsonrpc":"2.0","id":1,"method":"getHealth"}""")?.contains("\"ok\"") != true) {
            println("SplTokenRealTransferTest skipped: no validator at $rpcUrl")
            return
        }

        val localMint = AllowedStablecoin(mintAddress = mint, symbol = "TEST", decimals = 6)
        val senderKey = privateKeyFor(senderPhrase)
        val recipient = ChainKeyPair.addressFor(Chain.SOLANA, privateKeyFor(recipientPhrase))
        val before = tokenBalance(recipient)?.toBigDecimal() ?: java.math.BigDecimal.ZERO

        val result = SplTokenSender(rpcUrl) { if (it == mint) localMint else null }
            .send(senderKey, mint, recipient, "12.5")

        assertTrue(result is TokenSendResult.Sent, "token transfer refused: $result")
        println("token sig=${(result as TokenSendResult.Sent).signature} createdAta=${result.createdRecipientAccount}")

        var after = before
        repeat(30) {
            after = tokenBalance(recipient)?.toBigDecimal() ?: java.math.BigDecimal.ZERO
            if (after > before) return@repeat
            Thread.sleep(1000)
        }
        assertEquals(0, before.add("12.5".toBigDecimal()).compareTo(after), "recipient balance wrong")
    }
}
