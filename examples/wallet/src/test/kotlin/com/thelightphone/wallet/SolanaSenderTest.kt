package com.thelightphone.wallet

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SolanaSenderTest {

    @Test
    fun theRequestPinsPreflightToTheCommitmentTheBalanceWasReadAt() {
        val request = sendTransactionRequest(byteArrayOf(1, 2, 3))
        assertTrue(request.contains("\"preflightCommitment\":\"confirmed\""), request)
        assertTrue(request.contains("\"encoding\":\"base64\""), request)
        assertTrue(request.contains("\"AQID\""), request)
    }

    @Test
    fun aSignatureIsReadOffASuccessfulResponse() {
        assertEquals(
            "4pq1",
            parseSendSignature("""{"jsonrpc":"2.0","result":"4pq1","id":0}"""),
        )
    }

    @Test
    fun anRpcErrorSurfacesItsOwnMessage() {
        val failure = assertFailsWith<IllegalStateException> {
            parseSendSignature(
                """{"jsonrpc":"2.0","error":{"code":-32002,"message":"AccountNotFound"},"id":0}""",
            )
        }
        assertEquals("AccountNotFound", failure.message)
    }

    @Test
    fun aResponseWithNeitherResultNorErrorIsAnError() {
        assertFailsWith<IllegalStateException> { parseSendSignature("""{"jsonrpc":"2.0","id":0}""") }
        assertFailsWith<IllegalStateException> { parseSendSignature("not json") }
        assertFailsWith<IllegalStateException> { parseSendSignature("""{"result":null}""") }
    }
}
