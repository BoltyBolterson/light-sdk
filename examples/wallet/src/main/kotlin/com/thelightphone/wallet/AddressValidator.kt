package com.thelightphone.wallet

import java.math.BigInteger
import java.security.MessageDigest
import org.bouncycastle.jcajce.provider.digest.Keccak

private const val MAX_SCAN_CHARS = 128
private const val BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
private const val BECH32_CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
private const val BITCOIN_HRP = "bc"
private val BASE58 = BigInteger.valueOf(58)
private val BECH32_GENERATOR = intArrayOf(0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3)

internal object AddressValidator {

    fun validate(scanned: String, chain: Chain): String? {
        if (scanned.length > MAX_SCAN_CHARS) return null
        val address = sanitize(scanned)
        return when (chain) {
            Chain.SOLANA -> address.takeIf { isSolana(it) }
            Chain.BITCOIN -> address.takeIf { isBase58Check(it) || isBech32(it) }
            Chain.ETHEREUM -> address.takeIf { isEthereum(it) }
        }
    }

    fun validateAny(scanned: String, chains: List<Chain>): String? =
        chains.firstNotNullOfOrNull { validate(scanned, it) }

    private fun sanitize(scanned: String): String = scanned
        .filterNot { it.isISOControl() || Character.getType(it) == Character.FORMAT.toInt() }
        .trim()

    private fun isSolana(address: String): Boolean =
        address.length in 32..44 && address.all { it in BASE58_ALPHABET }

    private fun isBase58Check(address: String): Boolean {
        if (address.length !in 26..35) return false
        val decoded = base58Decode(address) ?: return false
        if (decoded.size != 25) return false
        val payload = decoded.copyOfRange(0, 21)
        if (payload[0] != 0x00.toByte() && payload[0] != 0x05.toByte()) return false
        val sha256 = MessageDigest.getInstance("SHA-256")
        val checksum = sha256.digest(sha256.digest(payload)).copyOfRange(0, 4)
        return checksum.contentEquals(decoded.copyOfRange(21, 25))
    }

    private fun base58Decode(input: String): ByteArray? {
        var value = BigInteger.ZERO
        for (character in input) {
            val digit = BASE58_ALPHABET.indexOf(character)
            if (digit < 0) return null
            value = value * BASE58 + BigInteger.valueOf(digit.toLong())
        }
        val magnitude = value.toByteArray().let {
            if (it.size > 1 && it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it
        }
        return ByteArray(input.takeWhile { it == BASE58_ALPHABET[0] }.length) + magnitude
    }

    private fun isBech32(address: String): Boolean {
        if (address.any { it.isUpperCase() } && address.any { it.isLowerCase() }) return false
        val lowercased = address.lowercase()
        if (lowercased.length !in 14..90 || !lowercased.startsWith("${BITCOIN_HRP}1")) return false
        val data = lowercased.drop(BITCOIN_HRP.length + 1).map { BECH32_CHARSET.indexOf(it) }
        if (data.size < 6 || data.any { it < 0 }) return false
        val checksum = bech32Polymod(bech32HrpExpand(BITCOIN_HRP) + data)
        return checksum == 1 || checksum == 0x2bc830a3
    }

    private fun bech32HrpExpand(hrp: String): List<Int> =
        hrp.map { it.code ushr 5 } + 0 + hrp.map { it.code and 31 }

    private fun bech32Polymod(values: List<Int>): Int {
        var checksum = 1
        for (value in values) {
            val top = checksum ushr 25
            checksum = ((checksum and 0x1ffffff) shl 5) xor value
            for (bit in 0..4) if ((top ushr bit) and 1 == 1) checksum = checksum xor BECH32_GENERATOR[bit]
        }
        return checksum
    }

    private fun isEthereum(address: String): Boolean {
        if (address.length != 42 || !address.startsWith("0x")) return false
        val body = address.drop(2)
        if (body.any { it !in "0123456789abcdefABCDEF" }) return false
        if (body.none { it.isUpperCase() } || body.none { it.isLowerCase() }) return true
        return body == eip55(body.lowercase())
    }

    private fun eip55(lowercased: String): String {
        val hash = Keccak.Digest256().digest(lowercased.toByteArray(Charsets.US_ASCII))
        return lowercased.mapIndexed { index, character ->
            val nibble = (hash[index / 2].toInt() shr if (index % 2 == 0) 4 else 0) and 0xf
            if (nibble < 8) character else character.uppercaseChar()
        }.joinToString("")
    }
}
