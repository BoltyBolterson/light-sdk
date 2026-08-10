package com.thelightphone.wallet

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

internal class ScreenLockRequiredException(cause: Throwable) : Exception(
    "Set a screen lock on this phone to use the wallet.",
    cause,
)

internal class WalletKeystore(
    private val keyAlias: String = KEY_ALIAS,
) {
    fun ensureKey() {
        if (exists()) return

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
            .setUnlockedDeviceRequired(true)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationParameters(
                AUTH_VALIDITY_SECONDS,
                KeyProperties.AUTH_DEVICE_CREDENTIAL or KeyProperties.AUTH_BIOMETRIC_STRONG,
            )
            .build()

        try {
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        } catch (e: Exception) {
            throw ScreenLockRequiredException(e)
        }
    }

    fun getSecretKey(): SecretKey {
        val entry = keyStore().getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
            ?: error("Wallet keystore key '$keyAlias' is missing")
        return entry.secretKey
    }

    fun exists(): Boolean = keyStore().containsAlias(keyAlias)

    fun delete() {
        keyStore().deleteEntry(keyAlias)
    }

    private fun keyStore(): KeyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    companion object {
        const val KEY_ALIAS = "wallet_master_key_v2"
        const val LEGACY_KEY_ALIAS = "wallet_master_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AUTH_VALIDITY_SECONDS = 900
    }
}
