package com.thelightphone.wallet.price

import com.thelightphone.wallet.Chain
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant

/**
 * Client for Pyth Network's Hermes price service.
 *
 * Hermes is a plain HTTPS/REST API — no on-chain RPC call and no Pyth SDK are needed to
 * *read* a price. Verified live against the running service on 2026-08-07:
 * `GET https://hermes.pyth.network/v2/updates/price/latest?ids[]=<feed id>&encoding=hex`
 * returned, for the BTC/USD feed:
 * ```json
 * {
 *   "binary": { "encoding": "hex", "data": ["<hex update bytes>"] },
 *   "parsed": [{
 *     "id": "e62df6c8b4a85fe1a67db44dc12de5db330f7ac66b72dc658afedf0f4a415b43",
 *     "price": { "price": "6488617685106", "conf": "2302419392", "expo": -8, "publish_time": 1786140552 },
 *     "ema_price": { "price": "...", "conf": "...", "expo": -8, "publish_time": 1786140552 },
 *     "metadata": { "slot": 307134755, "proof_available_time": 1786140557, "prev_publish_time": 1786140551 }
 *   }]
 * }
 * ```
 * `price` and `conf` share `expo`; the real value is `price * 10^expo` and the real
 * confidence half-width is `conf * 10^expo` (see
 * https://docs.pyth.network/price-feeds/core/how-pyth-works/hermes).
 *
 * `binary.data` is the raw Wormhole-signed update payload — the same bytes an on-chain
 * `updatePriceFeeds`-style call would consume to post this exact attestation on-chain for
 * verification. This client does not post anything on-chain; it only carries that payload
 * through on [PythPrice.rawUpdateData] so a later step (Tier 2 spend confirmation) can.
 *
 * Feed IDs below were read directly from the live Hermes metadata endpoint
 * (`GET https://hermes.pyth.network/v2/price_feeds?query=<BASE>/USD&asset_type=crypto`)
 * on 2026-08-07, matching `attributes.symbol == "Crypto.<BASE>/USD"`. Do not hand-derive
 * or guess these — re-verify against Hermes if they ever need to change.
 *
 * Note: Pyth's docs (as fetched 2026-08-07) mention an "upgraded" Hermes host
 * (`https://pyth.dourolabs.app/hermes/api/`) moving to require an API key for the legacy
 * `/api/latest_price_feeds` (v1) path. This client uses the current `hermes.pyth.network`
 * host and the v2 `/v2/updates/price/latest` path, which serves unauthenticated as of this
 * writing (confirmed by the live call above). No API key handling is implemented — if
 * Hermes starts requiring one for this path, calls will fail with a non-2xx status.
 *
 * Every failure path in [getDisplayPrice] / [getAuditedPrice] — a non-2xx HTTP status, a
 * network/connect/timeout failure, malformed or unexpected-shape JSON, or invalid hex in
 * the binary update payload — surfaces uniformly as an [IllegalStateException], with the
 * original lower-level exception (if any) attached as [Throwable.cause]. That is a
 * deliberate single documented failure type for callers to handle, not an accident of
 * which code path happened to throw first; see [fetchLatest]'s `catch` block.
 */
class PythPriceClient {
    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            // This feeds spend decisions - fail fast rather than hang indefinitely on a
            // slow/dead network. Values are generous enough for a normal mobile connection
            // but not so long that a caller gating a spend on this is left hanging.
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 15_000
        }
    }

    /**
     * Tier 1 (display): fetch the current Hermes price for [chain] on demand. No polling —
     * callers decide when to refresh, matching this app's manual-refresh design.
     */
    suspend fun getDisplayPrice(chain: Chain): PythPrice = fetchLatest(chain)

    /**
     * Tier 2 (audited): fetch the current Hermes price for [chain] and apply Pyth's own
     * confidence-interval guidance before it's trusted for an actual spend of a
     * non-stable asset.
     *
     * Pyth does not publish one universal pass/fail number for confidence-to-price ratio
     * (`conf / price`); its best-practices guidance
     * (https://docs.pyth.network/price-feeds/core/best-practices) is that a consuming
     * protocol should pick its own threshold based on its risk tolerance and, once the
     * ratio exceeds it, "widen spreads or cap the maximum trade size" rather than act on
     * the price as-is. [maxConfidenceRatio] is this app's chosen threshold (not a Pyth
     * constant) for gating a spend; [DEFAULT_MAX_CONFIDENCE_RATIO] is used if the caller
     * doesn't override it.
     *
     * This also gates on staleness, independent of confidence: a price whose
     * `publish_time` is more than [maxPriceAge] in the past is rejected even if its
     * confidence interval is tight, because a delayed/cached Hermes response can carry a
     * perfectly confident-looking price that is simply out of date for a fast-moving
     * market. [DEFAULT_MAX_PRICE_AGE] is used if the caller doesn't override it.
     *
     * Returns [PythAuditedPriceResult.TooUncertain] for a too-wide (or non-positive/
     * degenerate) price and [PythAuditedPriceResult.Stale] for a too-old one, instead of
     * silently returning an untrustworthy price — see those variants' docs for how to tell
     * the two rejection reasons apart.
     */
    suspend fun getAuditedPrice(
        chain: Chain,
        maxConfidenceRatio: BigDecimal = DEFAULT_MAX_CONFIDENCE_RATIO,
        maxPriceAge: Duration = DEFAULT_MAX_PRICE_AGE,
    ): PythAuditedPriceResult {
        val price = fetchLatest(chain)
        return auditPrice(price, maxConfidenceRatio, maxPriceAge, Instant.now())
    }

    private suspend fun fetchLatest(chain: Chain): PythPrice {
        val feedId = chain.pythFeedId()
        try {
            val response = client.get(
                "$HERMES_BASE/v2/updates/price/latest?ids%5B%5D=$feedId&encoding=hex",
            )

            if (!response.status.isSuccess()) {
                val body = response.bodyAsText().take(500)
                throw IllegalStateException("Hermes HTTP ${response.status.value}: $body")
            }

            val parsedResponse: HermesLatestPriceResponse = response.body()
            val entry = parsedResponse.parsed.firstOrNull { it.id.equals(feedId, ignoreCase = true) }
                ?: throw IllegalStateException("Hermes response had no price entry for feed $feedId")
            val rawHex = parsedResponse.binary.data.firstOrNull()
                ?: throw IllegalStateException("Hermes response had no binary update data for feed $feedId")

            return PythPrice(
                chain = chain,
                usdPrice = entry.price.priceAsUsd(),
                confidenceUsd = entry.price.confAsUsd(),
                publishTime = entry.price.publishTime,
                rawUpdateData = rawHex.decodeHex(),
            )
        } catch (e: CancellationException) {
            // Coroutine cancellation must always propagate untouched, never get wrapped.
            throw e
        } catch (e: IllegalStateException) {
            // Already our documented failure type (e.g. the non-2xx branch above) - don't
            // double-wrap it.
            throw e
        } catch (e: Exception) {
            // Anything else - network/connect/timeout failure (IOException and friends from
            // OkHttp/Ktor), malformed-JSON deserialization (SerializationException), bad hex
            // in the binary payload (IllegalArgumentException from decodeHex), etc. Wrap
            // uniformly so every failure path documented on the class surfaces the same way.
            throw IllegalStateException("Hermes request failed for feed $feedId: ${e.message}", e)
        }
    }

    fun close() {
        client.close()
    }

    companion object {
        /**
         * This app's chosen "too uncertain to spend against" threshold for
         * [getAuditedPrice]: 1% confidence-to-price ratio. Pyth does not prescribe this
         * number (see [getAuditedPrice] doc) — it is a conservative default for gating a
         * non-stable-asset spend, not a value sourced from Pyth's docs.
         */
        val DEFAULT_MAX_CONFIDENCE_RATIO: BigDecimal = BigDecimal("0.01")

        /**
         * This app's chosen "too old to spend against" threshold for [getAuditedPrice]: 60
         * seconds. Not a Pyth constant - Hermes typically republishes each feed roughly
         * every ~400ms-a few seconds under normal operation, so anything measured in
         * minutes indicates a stalled/cached/delayed publisher rather than routine network
         * jitter. 60s is a conservative "spend-time audited price" cutoff: generous enough
         * to absorb a brief Hermes hiccup or clock skew without spuriously rejecting good
         * data, tight enough that a fast-moving market can't drift meaningfully past it
         * before the stale price is refused.
         */
        val DEFAULT_MAX_PRICE_AGE: Duration = Duration.ofSeconds(60)
    }
}

/**
 * Pure gating logic behind [PythPriceClient.getAuditedPrice], split out from the network
 * fetch so it's independently testable against constructed [PythPrice] fixtures. [now] is
 * passed in (rather than read internally) for the same reason - deterministic staleness
 * tests without a real clock.
 */
internal fun auditPrice(
    price: PythPrice,
    maxConfidenceRatio: BigDecimal,
    maxPriceAge: Duration,
    now: Instant,
): PythAuditedPriceResult {
    // A non-positive price is degenerate data, not just "low confidence" - conf/price is
    // undefined (or actively misleading) in that case, so reject before ever computing a
    // ratio. See confidenceRatio()'s doc for why it now throws instead of returning ZERO.
    if (price.usdPrice.signum() <= 0) {
        return PythAuditedPriceResult.TooUncertain(
            price = price,
            confidenceRatio = null,
            maxAllowedRatio = maxConfidenceRatio,
        )
    }

    val ageSeconds = Duration.between(Instant.ofEpochSecond(price.publishTime), now).seconds
    if (ageSeconds > maxPriceAge.seconds) {
        return PythAuditedPriceResult.Stale(
            price = price,
            ageSeconds = ageSeconds,
            maxAgeSeconds = maxPriceAge.seconds,
        )
    }

    val ratio = price.confidenceRatio()
    return if (ratio > maxConfidenceRatio) {
        PythAuditedPriceResult.TooUncertain(
            price = price,
            confidenceRatio = ratio,
            maxAllowedRatio = maxConfidenceRatio,
        )
    } else {
        PythAuditedPriceResult.Trusted(price)
    }
}

private const val HERMES_BASE = "https://hermes.pyth.network"

/** Pyth mainnet Hermes feed ID for `Crypto.<BASE>/USD`. See class doc for how these were verified. */
private fun Chain.pythFeedId(): String = when (this) {
    Chain.BITCOIN -> "e62df6c8b4a85fe1a67db44dc12de5db330f7ac66b72dc658afedf0f4a415b43"
    Chain.ETHEREUM -> "ff61491a931112ddf1bd8147cd1b641375f79f5825126d665480874634fd0ace"
    Chain.SOLANA -> "ef0d8b6fda2ceba41da15d4095d1da392a0d2f8ed0c6c7bc0f4cfac8c280b56d"
}

@Serializable
internal data class HermesLatestPriceResponse(
    val binary: HermesBinaryUpdate,
    val parsed: List<HermesParsedPrice> = emptyList(),
)

@Serializable
internal data class HermesBinaryUpdate(
    val encoding: String,
    val data: List<String>,
)

@Serializable
internal data class HermesParsedPrice(
    val id: String,
    val price: HermesPricePoint,
    @SerialName("ema_price") val emaPrice: HermesPricePoint? = null,
)

@Serializable
internal data class HermesPricePoint(
    val price: String,
    val conf: String,
    val expo: Int,
    @SerialName("publish_time") val publishTime: Long,
)

private fun HermesPricePoint.priceAsUsd(): BigDecimal =
    BigDecimal(BigInteger(price)).scaleByPowerOfTen(expo)

private fun HermesPricePoint.confAsUsd(): BigDecimal =
    BigDecimal(BigInteger(conf)).scaleByPowerOfTen(expo)

private fun String.decodeHex(): ByteArray {
    require(length % 2 == 0) { "Hex string $this has odd length" }
    return ByteArray(length / 2) { i ->
        val hi = Character.digit(this[i * 2], 16)
        val lo = Character.digit(this[i * 2 + 1], 16)
        require(hi >= 0 && lo >= 0) { "Invalid hex string $this" }
        ((hi shl 4) + lo).toByte()
    }
}

/**
 * A price quote for [chain] read from Pyth's Hermes API.
 *
 * [usdPrice] and [confidenceUsd] are already converted out of Pyth's fixed-point
 * `price` / `conf` / `expo` representation into USD (`value = raw * 10^expo`).
 * [confidenceUsd] is Pyth's stated ± half-width in USD, i.e. Pyth's attestation is that
 * the true price lies within `usdPrice ± confidenceUsd` — it is a dollar amount, not a
 * fraction (see [confidenceRatio] for the fraction).
 *
 * [publishTime] (Unix seconds) and [rawUpdateData] (the raw Hermes binary update payload)
 * are carried through unmodified so a later step can post this exact attestation on-chain
 * for verification (Tier 2 spend flow) — this class does not do that posting itself.
 */
data class PythPrice(
    val chain: Chain,
    val usdPrice: BigDecimal,
    val confidenceUsd: BigDecimal,
    val publishTime: Long,
    val rawUpdateData: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PythPrice) return false
        return chain == other.chain &&
            usdPrice == other.usdPrice &&
            confidenceUsd == other.confidenceUsd &&
            publishTime == other.publishTime &&
            rawUpdateData.contentEquals(other.rawUpdateData)
    }

    override fun hashCode(): Int {
        var result = chain.hashCode()
        result = 31 * result + usdPrice.hashCode()
        result = 31 * result + confidenceUsd.hashCode()
        result = 31 * result + publishTime.hashCode()
        result = 31 * result + rawUpdateData.contentHashCode()
        return result
    }
}

/**
 * Pyth's own "how uncertain is this price" ratio: `confidenceUsd / usdPrice`.
 *
 * Throws [IllegalArgumentException] for a non-positive [PythPrice.usdPrice] (zero or
 * negative) instead of returning a value. A zero/negative price is a degenerate response,
 * not a "perfectly confident" one — the previous behavior of returning [BigDecimal.ZERO]
 * for a zero price meant a broken response could never fail
 * [PythPriceClient.getAuditedPrice]'s confidence check, since nothing exceeds a ratio of
 * zero. [PythPriceClient.getAuditedPrice] (via `auditPrice`) checks for this case
 * explicitly before ever calling this function, so it fails closed with a
 * [PythAuditedPriceResult.TooUncertain] rather than hitting this throw.
 */
fun PythPrice.confidenceRatio(): BigDecimal {
    require(usdPrice.signum() > 0) { "Cannot compute a confidence ratio for non-positive usdPrice=$usdPrice" }
    return confidenceUsd.divide(usdPrice, 6, RoundingMode.HALF_UP)
}

/** Result of a Tier 2 (audited/spend) price fetch — see [PythPriceClient.getAuditedPrice]. */
sealed class PythAuditedPriceResult {
    /** [price]'s confidence-to-price ratio was within the caller's tolerance, and it was fresh enough. */
    data class Trusted(val price: PythPrice) : PythAuditedPriceResult()

    /**
     * [price] failed the confidence gate — either its confidence-to-price ratio
     * ([confidenceRatio]) exceeded [maxAllowedRatio], or [confidenceRatio] is `null`
     * because [price]'s `usdPrice` was zero/negative and no ratio could even be computed
     * (see [PythPrice.confidenceRatio]'s doc). Either way, per Pyth's guidance, callers
     * should not act on [price] for a spend as-is — widen spreads, cap trade size, or
     * refuse until a tighter/valid update is available.
     */
    data class TooUncertain(
        val price: PythPrice,
        val confidenceRatio: BigDecimal?,
        val maxAllowedRatio: BigDecimal,
    ) : PythAuditedPriceResult()

    /**
     * [price] was within the confidence tolerance but its `publish_time` is more than
     * [maxAgeSeconds] old ([ageSeconds] actual) as of the audit call. This is a distinct
     * failure mode from [TooUncertain]: the price *looks* confident but is out of date, e.g.
     * from a delayed/cached Hermes response during a fast market move — exactly the case
     * [PythPriceClient.getAuditedPrice] exists to catch. Callers should treat this the same
     * as [TooUncertain] for spend purposes (refuse / refetch) but may want to log or
     * message it differently since it's a freshness problem, not a data-quality one.
     */
    data class Stale(
        val price: PythPrice,
        val ageSeconds: Long,
        val maxAgeSeconds: Long,
    ) : PythAuditedPriceResult()
}
