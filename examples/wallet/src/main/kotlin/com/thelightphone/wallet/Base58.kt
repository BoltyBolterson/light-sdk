package com.thelightphone.wallet

import java.math.BigInteger
import java.security.MessageDigest

internal object Base58 {
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private val BASE = BigInteger.valueOf(58)

    fun encode(input: ByteArray): String {
        if (input.isEmpty()) return ""

        var value = BigInteger(1, input)
        val output = StringBuilder()
        while (value > BigInteger.ZERO) {
            val (quotient, remainder) = value.divideAndRemainder(BASE)
            output.append(ALPHABET[remainder.toInt()])
            value = quotient
        }
        input.takeWhile { it == 0.toByte() }.forEach { _ -> output.append(ALPHABET[0]) }
        return output.reverse().toString()
    }

    /** Bitcoin-style Base58Check: payload + first 4 bytes of double-SHA256(payload). */
    fun encodeCheck(payload: ByteArray): String {
        val sha256 = MessageDigest.getInstance("SHA-256")
        val checksum = sha256.digest(sha256.digest(payload)).copyOfRange(0, 4)
        return encode(payload + checksum)
    }
}
