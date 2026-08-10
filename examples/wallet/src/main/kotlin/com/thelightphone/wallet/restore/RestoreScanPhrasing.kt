package com.thelightphone.wallet.restore

import com.thelightphone.wallet.SolanaAmount

fun statusLabel(status: AddressStatus): String = when (status) {
    is AddressStatus.Funded -> when {
        status.lamports > 0L && status.hasTokens == true ->
            "${SolanaAmount.format(status.lamports)} SOL + tokens"
        status.lamports > 0L -> "${SolanaAmount.format(status.lamports)} SOL"
        else -> "tokens"
    }
    AddressStatus.Empty -> "empty"
    AddressStatus.Unknown -> "couldn't check"
}

fun scanHeadline(summary: ScanSummary): String = when {
    summary.funded > 0 -> "Funds on ${summary.funded} of ${summary.addressesExpected}"
    summary.isConclusive -> "Nothing found"
    else -> "No funds found so far"
}

/** An address that never answered must never read as "nothing here". */
fun scanCaveat(summary: ScanSummary): String? {
    if (summary.isConclusive) return null
    val unanswered = summary.addressesExpected - summary.resolved + summary.unreadable
    return "$unanswered of ${summary.addressesExpected} addresses did not answer. " +
        "This is not proof the seed is empty."
}
