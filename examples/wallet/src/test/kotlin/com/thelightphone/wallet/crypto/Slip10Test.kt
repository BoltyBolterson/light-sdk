package com.thelightphone.wallet.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters

/**
 * Fixtures from SLIP-0010's official ed25519 Test Vector 1
 * (https://github.com/satoshilabs/slips/blob/master/slip-0010.md).
 */
class Slip10Test {
    private val seed = fromHex("000102030405060708090a0b0c0d0e0f")

    @Test
    fun masterKey() {
        val master = Slip10Ed25519.masterKeyFromSeed(seed)
        assertEquals("2b4be7f19ee27bbf30c667b642d5f4aa69fd169872f8fc3059c08ebae2eb19e7", toHex(master.privateKey))
        assertEquals("90046a93de5380a72b5e45010748567d5ea02bbf6522f979e05c0d8d8ca9fffb", toHex(master.chainCode))

        // the spec's published public key is prefixed with 0x00 to disambiguate from
        // secp256k1's compressed-point encoding; strip it before comparing to BC's raw output.
        val publicKey = Ed25519PrivateKeyParameters(master.privateKey, 0).generatePublicKey().encoded
        assertEquals("a4b2856bfec510abab89753fac1ac0e1112364e7d250545963f135f2a33188ed", toHex(publicKey))
    }

    @Test
    fun hardenedChild0() {
        val master = Slip10Ed25519.masterKeyFromSeed(seed)
        val child = Slip10Ed25519.deriveChild(master, index = 0) // implicitly 0'

        assertEquals("68e0fe46dfb67e368c75379acec591dad19df3cde26e63b93a8e704f1dade7a3", toHex(child.privateKey))
        assertEquals("8b59aa11380b624e81507a27fedda59fea6d0b779a778918a2fd3590e16e9c69", toHex(child.chainCode))

        val publicKey = Ed25519PrivateKeyParameters(child.privateKey, 0).generatePublicKey().encoded
        assertEquals("8c8a13df77a28f3445213a0f432fde644acaa215fc72dcdf300d5efaa85d350c", toHex(publicKey))
    }

    @Test
    fun derivePathMatchesStepwiseDerivation() {
        val viaPath = Slip10Ed25519.derivePath(seed, "m/0'")
        val stepwise = Slip10Ed25519.deriveChild(Slip10Ed25519.masterKeyFromSeed(seed), index = 0)

        assertEquals(toHex(stepwise.privateKey), toHex(viaPath.privateKey))
        assertEquals(toHex(stepwise.chainCode), toHex(viaPath.chainCode))
    }

    private fun toHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }
    private fun fromHex(hex: String): ByteArray = ByteArray(hex.length / 2) { i ->
        hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
}
