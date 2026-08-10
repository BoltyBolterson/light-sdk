package com.thelightphone.wallet

import org.sol4k.Constants
import org.sol4k.Keypair
import java.net.HttpURLConnection
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SplTokenSendIntegrationTest {

    private val rpcUrl = System.getenv("SOLANA_TEST_RPC") ?: "http://127.0.0.1:8899"

    private val senderKey = ByteArray(32) { it.toByte() }

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

    @Test
    fun anExecutableAccountIsRefusedAfterReadingItFromTheChain() {
        if (!validatorIsUp()) {
            println("SplTokenSendIntegrationTest skipped: no validator at $rpcUrl")
            return
        }

        val tokenProgram = Constants.TOKEN_PROGRAM_ID
        assertTrue(SplTokenTransfer.canHoldTokens(tokenProgram), "on-curve, so only the chain read can refuse it")

        assertEquals(
            TokenSendResult.Refused(TokenSendRefusal.DESTINATION_CANNOT_HOLD_TOKENS),
            SplTokenSender(rpcUrl).send(
                senderKey,
                SolanaStablecoins.USDC.mintAddress,
                tokenProgram.toBase58(),
                "1",
            ),
        )
    }

    @Test
    fun aMissingMintFailsLoudlyRatherThanBeingRefusedOrSilentlySent() {
        if (!validatorIsUp()) {
            println("SplTokenSendIntegrationTest skipped: no validator at $rpcUrl")
            return
        }

        val destination = Keypair.fromSecretKey(ByteArray(32) { (it + 7).toByte() }).publicKey.toBase58()
        val outcome = runCatching {
            SplTokenSender(rpcUrl).send(senderKey, SolanaStablecoins.USDC.mintAddress, destination, "1")
        }

        assertTrue(
            outcome.isFailure,
            "mainnet USDC does not exist on a fresh validator, so this must fail: ${outcome.getOrNull()}",
        )
    }
}
