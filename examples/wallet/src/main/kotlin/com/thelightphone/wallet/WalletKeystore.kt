package com.thelightphone.wallet

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Wraps per-chain private keys at rest with an AndroidKeyStore-backed AES-256-GCM key.
 * Mirrors examples/authenticator's TotpKeystore. StrongBox availability on LightOS
 * hardware is still unconfirmed (per discussion #139) so it is not requested here.
 */
internal class WalletKeystore(
    private val keyAlias: String = KEY_ALIAS,
) {
    fun ensureKey() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(keyAlias)) return

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        keyGenerator.generateKey()
    }

    fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
            ?: error("Wallet keystore key '$keyAlias' is missing")
        return entry.secretKey
    }

    companion object {
        const val KEY_ALIAS = "wallet_master_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    }
}
