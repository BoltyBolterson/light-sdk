package com.thelightphone.wallet

const val LAMPORTS_PER_SIGNATURE = 5_000L

sealed interface SendAffordability {
    object Affordable : SendAffordability

    data class ShortBy(val lamports: Long) : SendAffordability

    data class StrandsRent(val maxKeepingAccount: Long, val maxDraining: Long) : SendAffordability
}

/** A remainder of exactly zero is legal; anything between zero and the rent-exempt minimum is not. */
fun affordSend(
    balance: Long,
    amount: Long,
    fee: Long,
    rentExemptMinimum: Long,
): SendAffordability {
    require(balance >= 0L && amount > 0L && fee >= 0L && rentExemptMinimum >= 0L)
    val spendable = balance - fee
    if (amount > spendable) return SendAffordability.ShortBy(saturatingMinus(amount, spendable))
    val remainder = spendable - amount
    if (remainder == 0L || remainder >= rentExemptMinimum) return SendAffordability.Affordable
    return SendAffordability.StrandsRent(
        maxKeepingAccount = (spendable - rentExemptMinimum).coerceAtLeast(0L),
        maxDraining = spendable,
    )
}

private fun saturatingMinus(a: Long, b: Long): Long =
    runCatching { Math.subtractExact(a, b) }.getOrDefault(Long.MAX_VALUE)
