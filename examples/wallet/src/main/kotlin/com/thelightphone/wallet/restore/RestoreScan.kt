package com.thelightphone.wallet.restore

sealed interface AddressStatus {
    data class Funded(val lamports: Long, val hasTokens: Boolean?) : AddressStatus

    object Empty : AddressStatus

    object Unknown : AddressStatus
}

/** Unknown wins over Empty: a failed token read on a USDC-only address must not report Empty. */
fun classifyAddress(lamports: Long?, hasTokens: Boolean?): AddressStatus = when {
    lamports == null -> AddressStatus.Unknown
    lamports > 0L -> AddressStatus.Funded(lamports, hasTokens)
    hasTokens == null -> AddressStatus.Unknown
    hasTokens -> AddressStatus.Funded(0L, true)
    else -> AddressStatus.Empty
}

data class ScanSummary(
    val addressesExpected: Int,
    val funded: Int,
    val empty: Int,
    val unreadable: Int,
) {
    val resolved: Int get() = funded + empty + unreadable

    /** "This seed holds nothing" is only sayable when every planned address answered. */
    val isConclusive: Boolean get() = unreadable == 0 && resolved == addressesExpected
}

fun summarize(statuses: Collection<AddressStatus>, addressesExpected: Int): ScanSummary = ScanSummary(
    addressesExpected = addressesExpected,
    funded = statuses.count { it is AddressStatus.Funded },
    empty = statuses.count { it is AddressStatus.Empty },
    unreadable = statuses.count { it is AddressStatus.Unknown },
)

fun needsTokenRead(lamports: Long?): Boolean = lamports == 0L
