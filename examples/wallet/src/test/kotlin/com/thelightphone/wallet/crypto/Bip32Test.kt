package com.thelightphone.wallet.crypto

import kotlin.test.Test
import kotlin.test.assertEquals

/** BIP-32 test vector 1, decoded from the spec's xprv strings to raw key + chain code hex. */
class Bip32Test {
    private val seed = fromHex("000102030405060708090a0b0c0d0e0f")

    @Test
    fun masterKey() {
        val master = Bip32.masterKeyFromSeed(seed)
        assertEquals("e8f32e723decf4051aefac8e2c93c9c5b214313817cdb01a1494b917c8436b35", toHex(master.privateKey))
        assertEquals("873dff81c02f525623fd1fe5167eac3a55a049de3d314bb42ee227ffed37d508", toHex(master.chainCode))
    }

    @Test
    fun hardenedChild0() {
        val master = Bip32.masterKeyFromSeed(seed)
        val child = Bip32.deriveChild(master, Bip32.HARDENED_OFFSET)

        assertEquals("edb2e14f9ee77d26dd93b4ecede8d16ed408ce149b6cd80b0715a2d911a0afea", toHex(child.privateKey))
        assertEquals("47fdacbd0f1097043b78c63c20c34ef4ed9a111d980047ad16282c7ae6236141", toHex(child.chainCode))
    }

    @Test
    fun derivePathMatchesSpecVector() {
        val viaPath = Bip32.derivePath(seed, "m/0'")

        assertEquals("edb2e14f9ee77d26dd93b4ecede8d16ed408ce149b6cd80b0715a2d911a0afea", toHex(viaPath.privateKey))
        assertEquals("47fdacbd0f1097043b78c63c20c34ef4ed9a111d980047ad16282c7ae6236141", toHex(viaPath.chainCode))
    }

    private fun toHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }
    private fun fromHex(hex: String): ByteArray = ByteArray(hex.length / 2) { i ->
        hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
}
