package com.thelightphone.wallet.nfc

interface NfcTapTarget {
    val isAvailable: Boolean

    suspend fun awaitTap(): Result<String>
}

object UnavailableNfcTapTarget : NfcTapTarget {
    override val isAvailable: Boolean = false

    override suspend fun awaitTap(): Result<String> =
        Result.failure(UnsupportedOperationException(UNAVAILABLE_MESSAGE))

    const val UNAVAILABLE_MESSAGE =
        "Tap to pay isn't available yet."
}
