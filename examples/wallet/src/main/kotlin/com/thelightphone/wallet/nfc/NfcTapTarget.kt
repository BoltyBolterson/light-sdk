package com.thelightphone.wallet.nfc

/**
 * Abstraction over "tap to receive a payment request" - reading an NFC tag/device that's
 * presenting a Solana Pay transfer-request URI (https://docs.solanapay.com/spec), the same
 * way LightQrCodeScanner reads one from a QR code.
 *
 * There's no NFC wrapper in the SDK yet (confirmed 2026-08 - `android.permission.NFC` and its
 * `android.hardware.nfc` <uses-feature> are already allow-listed at the plugin level per
 * LightToolPolicy.kt/ManifestGeneratorTest.kt, but sdk/ has no Kotlin API to actually read a
 * tag). This interface is what the wallet UI codes against in the meantime, so that once Light
 * ships a real wrapper - presumably something like `LightNfcReader`, mirroring
 * LightQrCodeScanner's title/onScanned/onBack shape - swapping [UnavailableNfcTapTarget] for a
 * real implementation is a one-file change with no UI rework.
 *
 * Scope note: this only covers *reading* an incoming request (phone-as-reader). The reverse
 * direction - presenting the Light Phone itself as the thing a merchant terminal taps, i.e.
 * Host Card Emulation - is a much bigger ask (an `HostApduService`, AID registration, likely
 * OS-level entitlement) and isn't something to assume or design around without Light's input.
 */
interface NfcTapTarget {
    /** True if tap-to-pay can plausibly work on this build/device right now. */
    val isAvailable: Boolean

    /**
     * Suspends until a tag is read or the caller cancels. Returns the raw NDEF payload text
     * (expected to be a `solana:` Solana Pay URI) on success.
     */
    suspend fun awaitTap(): Result<String>
}

/** Current reality: no LightOS NFC API exists yet, so every call fails cleanly and says why. */
object UnavailableNfcTapTarget : NfcTapTarget {
    override val isAvailable: Boolean = false

    override suspend fun awaitTap(): Result<String> =
        Result.failure(UnsupportedOperationException(UNAVAILABLE_MESSAGE))

    const val UNAVAILABLE_MESSAGE =
        "Tap to pay isn't available yet - LightOS doesn't have an NFC API for tools yet."
}
