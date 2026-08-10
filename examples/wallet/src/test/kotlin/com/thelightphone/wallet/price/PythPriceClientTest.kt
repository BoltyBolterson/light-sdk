package com.thelightphone.wallet.price

import com.thelightphone.wallet.Chain
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PythPriceClientTest {
    private val defaultMaxConfidenceRatio = PythPriceClient.DEFAULT_MAX_CONFIDENCE_RATIO
    private val defaultMaxPriceAge = PythPriceClient.DEFAULT_MAX_PRICE_AGE

    private fun price(
        usdPrice: BigDecimal = BigDecimal("50000.00"),
        confidenceUsd: BigDecimal = BigDecimal("5.00"),
        publishTime: Long = Instant.now().epochSecond,
    ): PythPrice = PythPrice(
        chain = Chain.BITCOIN,
        usdPrice = usdPrice,
        confidenceUsd = confidenceUsd,
        publishTime = publishTime,
    )

    @Test
    fun stalePublishTimeIsRejectedEvenWithTightConfidence() {
        val now = Instant.now()
        val staleTime = now.minusSeconds(defaultMaxPriceAge.seconds + 1)

        val result = auditPrice(
            price = price(confidenceUsd = BigDecimal("0.01"), publishTime = staleTime.epochSecond),
            maxConfidenceRatio = defaultMaxConfidenceRatio,
            maxPriceAge = defaultMaxPriceAge,
            now = now,
        )

        val stale = assertIs<PythAuditedPriceResult.Stale>(result)
        assertEquals(defaultMaxPriceAge.seconds, stale.maxAgeSeconds)
        assertEquals(defaultMaxPriceAge.seconds + 1, stale.ageSeconds)
    }

    @Test
    fun priceExactlyAtMaxAgeBoundaryIsNotStale() {
        val now = Instant.now()
        val boundaryTime = now.minusSeconds(defaultMaxPriceAge.seconds)

        val result = auditPrice(
            price = price(publishTime = boundaryTime.epochSecond),
            maxConfidenceRatio = defaultMaxConfidenceRatio,
            maxPriceAge = defaultMaxPriceAge,
            now = now,
        )

        assertIs<PythAuditedPriceResult.Trusted>(result)
    }

    @Test
    fun freshPriceIsNotRejectedForStaleness() {
        val now = Instant.now()

        val result = auditPrice(
            price = price(publishTime = now.epochSecond),
            maxConfidenceRatio = defaultMaxConfidenceRatio,
            maxPriceAge = defaultMaxPriceAge,
            now = now,
        )

        assertIs<PythAuditedPriceResult.Trusted>(result)
    }

    @Test
    fun zeroPriceIsRejectedNotMaximallyConfident() {
        val now = Instant.now()

        val result = auditPrice(
            price = price(usdPrice = BigDecimal.ZERO, confidenceUsd = BigDecimal("1.00"), publishTime = now.epochSecond),
            maxConfidenceRatio = defaultMaxConfidenceRatio,
            maxPriceAge = defaultMaxPriceAge,
            now = now,
        )

        val tooUncertain = assertIs<PythAuditedPriceResult.TooUncertain>(result)
        assertNull(tooUncertain.confidenceRatio)
    }

    @Test
    fun negativePriceIsRejected() {
        val now = Instant.now()

        val result = auditPrice(
            price = price(usdPrice = BigDecimal("-100.00"), confidenceUsd = BigDecimal("1.00"), publishTime = now.epochSecond),
            maxConfidenceRatio = defaultMaxConfidenceRatio,
            maxPriceAge = defaultMaxPriceAge,
            now = now,
        )

        val tooUncertain = assertIs<PythAuditedPriceResult.TooUncertain>(result)
        assertNull(tooUncertain.confidenceRatio)
    }

    @Test
    fun confidenceRatioThrowsForZeroPrice() {
        assertFailsWith<IllegalArgumentException> {
            price(usdPrice = BigDecimal.ZERO).confidenceRatio()
        }
    }

    @Test
    fun confidenceRatioThrowsForNegativePrice() {
        assertFailsWith<IllegalArgumentException> {
            price(usdPrice = BigDecimal("-1")).confidenceRatio()
        }
    }

    @Test
    fun confidenceRatioComputesExpectedFraction() {
        val p = price(usdPrice = BigDecimal("100.00"), confidenceUsd = BigDecimal("1.00"))
        assertEquals(BigDecimal("0.010000"), p.confidenceRatio())
    }

    @Test
    fun priceUnderConfidenceThresholdIsTrusted() {
        val now = Instant.now()

        val result = auditPrice(
            price = price(usdPrice = BigDecimal("100.00"), confidenceUsd = BigDecimal("0.50"), publishTime = now.epochSecond),
            maxConfidenceRatio = defaultMaxConfidenceRatio,
            maxPriceAge = defaultMaxPriceAge,
            now = now,
        )

        assertIs<PythAuditedPriceResult.Trusted>(result)
    }

    @Test
    fun priceOverConfidenceThresholdIsTooUncertain() {
        val now = Instant.now()

        val result = auditPrice(
            price = price(usdPrice = BigDecimal("100.00"), confidenceUsd = BigDecimal("2.00"), publishTime = now.epochSecond),
            maxConfidenceRatio = defaultMaxConfidenceRatio,
            maxPriceAge = defaultMaxPriceAge,
            now = now,
        )

        val tooUncertain = assertIs<PythAuditedPriceResult.TooUncertain>(result)
        assertEquals(BigDecimal("0.020000"), tooUncertain.confidenceRatio)
        assertEquals(defaultMaxConfidenceRatio, tooUncertain.maxAllowedRatio)
    }

    @Test
    fun timeToStaleCountsFromPublishTimeNotFromFetch() {
        val now = Instant.ofEpochSecond(1_700_000_000)
        val fetched = price(publishTime = now.minusSeconds(45).epochSecond)

        assertEquals(
            15_000L,
            fetched.millisUntilStale(defaultMaxPriceAge, now),
            "a price already 45s old must expire in 15s, not in a fresh 60s",
        )
    }

    @Test
    fun anAlreadyStalePriceHasNoTimeLeft() {
        val now = Instant.ofEpochSecond(1_700_000_000)
        val fetched = price(publishTime = now.minusSeconds(defaultMaxPriceAge.seconds + 30).epochSecond)

        assertTrue(fetched.millisUntilStale(defaultMaxPriceAge, now) <= 0L)
    }
}
