package com.thelightphone.wallet.restore

/** What the chain was able to tell us about one derived address. */
sealed interface AddressStatus {
    data class Funded(val lamports: Long, val hasTokens: Boolean) : AddressStatus

    /** The chain answered, and the answer was nothing. */
    object Empty : AddressStatus

    /** Some read failed. We do not know, and must not guess. */
    object Unknown : AddressStatus
}

/**
 * Classify one address from its three reads, any of which may be `null` for "couldn't tell".
 *
 * Unknown wins over Empty everywhere. A zero SOL balance is only Empty once the token read has also
 * come back clean — otherwise a failed token read on a wallet holding nothing but USDC would present
 * as an empty address, and the user would restore past their own money.
 */
fun classifyAddress(lamports: Long?, hasTokens: Boolean?): AddressStatus = when {
    lamports == null -> AddressStatus.Unknown
    lamports > 0L -> AddressStatus.Funded(lamports, hasTokens == true)
    hasTokens == null -> AddressStatus.Unknown
    hasTokens -> AddressStatus.Funded(0L, true)
    else -> AddressStatus.Empty
}

/**
 * The outcome of a whole scan. [unreadable] is the number that matters: it is what turns
 * "this seed holds nothing" from a claim into a guess.
 */
data class ScanSummary(
    val probesTotal: Int,
    val funded: Int,
    val empty: Int,
    val unreadable: Int,
) {
    /** True only when every probe returned a real answer, so "nothing found" can be stated outright. */
    val isConclusive: Boolean get() = unreadable == 0

    /** Progress for the bar: probes resolved, however they resolved. */
    val resolved: Int get() = funded + empty + unreadable
}

fun summarize(statuses: Collection<AddressStatus>, probesTotal: Int): ScanSummary = ScanSummary(
    probesTotal = probesTotal,
    funded = statuses.count { it is AddressStatus.Funded },
    empty = statuses.count { it is AddressStatus.Empty },
    unreadable = statuses.count { it is AddressStatus.Unknown },
)

/**
 * Whether an address still needs the follow-up token reads. An address holding SOL is already Funded,
 * and one whose balance read failed is already Unknown — neither verdict can be changed by asking about
 * tokens, so only a confirmed zero balance is worth the extra requests.
 */
fun needsTokenRead(lamports: Long?): Boolean = lamports == 0L
