package com.thelightphone.wallet.restore

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Every test here asks one question: can a broken, hostile or errored RPC response be mistaken for
 * "this address is empty"? That mistake tells someone their funded seed holds nothing.
 */
class SolanaScanRpcTest {

    private val address = "AaaaAaaaAaaaAaaaAaaaAaaaAaaaAaaaAaaaAaaaAaa"

    // ── parseBalance ──────────────────────────────────────────────────────

    @Test
    fun balanceIsRead() {
        val body = """{"jsonrpc":"2.0","id":0,"result":{"context":{"slot":1},"value":4200000000}}"""
        assertEquals(4_200_000_000L, SolanaScanRpc.parseBalance(body))
    }

    @Test
    fun zeroBalanceIsAnAnswerNotAFailure() {
        val body = """{"jsonrpc":"2.0","id":0,"result":{"context":{"slot":1},"value":0}}"""
        assertEquals(0L, SolanaScanRpc.parseBalance(body), "the chain said zero, and that is a real answer")
    }

    @Test
    fun rpcErrorIsUnknownNotZero() {
        val body = """{"jsonrpc":"2.0","id":0,"error":{"code":-32005,"message":"rate limited"}}"""
        assertNull(SolanaScanRpc.parseBalance(body))
    }

    @Test
    fun malformedBodyIsUnknownNotZero() {
        assertNull(SolanaScanRpc.parseBalance("<html>502 Bad Gateway</html>"))
    }

    @Test
    fun emptyBodyIsUnknownNotZero() {
        // The scanner turns any transport failure into an empty body, so this is the dead-host path.
        assertNull(SolanaScanRpc.parseBalance(""))
    }

    @Test
    fun missingValueIsUnknownNotZero() {
        assertNull(SolanaScanRpc.parseBalance("""{"jsonrpc":"2.0","id":0,"result":{"context":{"slot":1}}}"""))
    }

    @Test
    fun nonNumericValueIsUnknownNotZero() {
        assertNull(SolanaScanRpc.parseBalance("""{"jsonrpc":"2.0","id":0,"result":{"value":"lots"}}"""))
    }

    // ── parseHasTokenBalance ──────────────────────────────────────────────

    private fun tokenBody(vararg uiAmounts: Double): String {
        val entries = uiAmounts.joinToString(",") {
            """{"account":{"data":{"parsed":{"info":{"mint":"MintAAA","tokenAmount":{"uiAmount":$it}}}}}}"""
        }
        return """{"jsonrpc":"2.0","id":0,"result":{"context":{"slot":1},"value":[$entries]}}"""
    }

    @Test
    fun anyPositiveTokenBalanceCounts() {
        assertEquals(true, SolanaScanRpc.parseHasTokenBalance(tokenBody(0.0, 1.5)))
    }

    @Test
    fun onlyZeroBalanceTokenAccountsAreNotHoldings() {
        assertEquals(false, SolanaScanRpc.parseHasTokenBalance(tokenBody(0.0, 0.0)))
    }

    @Test
    fun noTokenAccountsIsAConfidentFalse() {
        assertEquals(false, SolanaScanRpc.parseHasTokenBalance(tokenBody()))
    }

    @Test
    fun tokenReadErrorIsUnknownNotFalse() {
        val body = """{"jsonrpc":"2.0","id":0,"error":{"code":-32000,"message":"boom"}}"""
        assertNull(SolanaScanRpc.parseHasTokenBalance(body), "a failed read must not look like 'holds nothing'")
    }

    @Test
    fun tokenReadMalformedBodyIsUnknown() {
        assertNull(SolanaScanRpc.parseHasTokenBalance("nonsense"))
    }

    @Test
    fun tokenReadMissingValueIsUnknown() {
        assertNull(SolanaScanRpc.parseHasTokenBalance("""{"jsonrpc":"2.0","id":0,"result":{"context":{"slot":1}}}"""))
    }

    // ── hasAnyTokenBalance: combining the two token programs ──────────────

    @Test
    fun holdingsInEitherProgramCount() {
        assertEquals(true, SolanaScanRpc.hasAnyTokenBalance(listOf(false, true)))
        assertEquals(true, SolanaScanRpc.hasAnyTokenBalance(listOf(true, false)))
    }

    @Test
    fun bothProgramsEmptyIsAConfidentFalse() {
        assertEquals(false, SolanaScanRpc.hasAnyTokenBalance(listOf(false, false)))
    }

    @Test
    fun oneProgramUnreadableMakesTheWholeAnswerUnknown() {
        // Token-2022 read failed. The tokens we could not see may be the only ones this wallet holds.
        assertNull(SolanaScanRpc.hasAnyTokenBalance(listOf(false, null)))
    }

    @Test
    fun aFoundHoldingBeatsAnUnreadableSibling() {
        // We already know there is money here; the failed read cannot make that less true.
        assertEquals(true, SolanaScanRpc.hasAnyTokenBalance(listOf(true, null)))
    }

    @Test
    fun noProgramsQueriedIsUnknownNotFalse() {
        assertNull(SolanaScanRpc.hasAnyTokenBalance(emptyList()), "asking nothing is not the same as finding nothing")
    }

    // ── requests ──────────────────────────────────────────────────────────

    @Test
    fun balanceRequestNamesTheAddress() {
        val parsed = Json.parseToJsonElement(SolanaScanRpc.balanceRequest(address)).jsonObject
        assertEquals("getBalance", parsed["method"]!!.jsonPrimitive.content)
        assertEquals(address, parsed["params"]!!.jsonArray[0].jsonPrimitive.content)
    }

    @Test
    fun tokenAccountsRequestNamesTheAddressAndProgram() {
        val programId = SolanaScanRpc.TOKEN_PROGRAM_IDS[1]
        val parsed = Json.parseToJsonElement(
            SolanaScanRpc.tokenAccountsRequest(address, programId),
        ).jsonObject
        assertEquals("getTokenAccountsByOwner", parsed["method"]!!.jsonPrimitive.content)
        val params = parsed["params"]!!.jsonArray
        assertEquals(address, params[0].jsonPrimitive.content)
        assertEquals(programId, params[1].jsonObject["programId"]!!.jsonPrimitive.content)
    }

    @Test
    fun bothTokenProgramsAreCovered() {
        // Query only the classic program and every Token-2022 holding silently disappears.
        assertEquals(2, SolanaScanRpc.TOKEN_PROGRAM_IDS.size)
        assertTrue(SolanaScanRpc.TOKEN_PROGRAM_IDS.contains("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"))
        assertTrue(SolanaScanRpc.TOKEN_PROGRAM_IDS.contains("TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb"))
    }
}
