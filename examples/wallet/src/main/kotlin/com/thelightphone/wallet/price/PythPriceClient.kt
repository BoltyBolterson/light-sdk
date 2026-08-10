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

class PythPriceClient {
    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 15_000
        }
    }

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
            val response = client.get("$HERMES_BASE/v2/updates/price/latest?ids%5B%5D=$feedId")

            if (!response.status.isSuccess()) {
                val body = response.bodyAsText().take(500)
                throw IllegalStateException("Hermes HTTP ${response.status.value}: $body")
            }

            val parsedResponse: HermesLatestPriceResponse = response.body()
            val entry = parsedResponse.parsed.firstOrNull { it.id.equals(feedId, ignoreCase = true) }
                ?: throw IllegalStateException("Hermes response had no price entry for feed $feedId")

            return PythPrice(
                chain = chain,
                usdPrice = entry.price.priceAsUsd(),
                confidenceUsd = entry.price.confAsUsd(),
                publishTime = entry.price.publishTime,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException("Hermes request failed for feed $feedId: ${e.message}", e)
        }
    }

    fun close() {
        client.close()
    }

    companion object {
        val DEFAULT_MAX_CONFIDENCE_RATIO: BigDecimal = BigDecimal("0.01")

        val DEFAULT_MAX_PRICE_AGE: Duration = Duration.ofSeconds(60)
    }
}

internal fun auditPrice(
    price: PythPrice,
    maxConfidenceRatio: BigDecimal,
    maxPriceAge: Duration,
    now: Instant,
): PythAuditedPriceResult {
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

private fun Chain.pythFeedId(): String = when (this) {
    Chain.BITCOIN -> "e62df6c8b4a85fe1a67db44dc12de5db330f7ac66b72dc658afedf0f4a415b43"
    Chain.ETHEREUM -> "ff61491a931112ddf1bd8147cd1b641375f79f5825126d665480874634fd0ace"
    Chain.SOLANA -> "ef0d8b6fda2ceba41da15d4095d1da392a0d2f8ed0c6c7bc0f4cfac8c280b56d"
}

@Serializable
internal data class HermesLatestPriceResponse(
    val parsed: List<HermesParsedPrice> = emptyList(),
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

data class PythPrice(
    val chain: Chain,
    val usdPrice: BigDecimal,
    val confidenceUsd: BigDecimal,
    val publishTime: Long,
)

fun PythPrice.confidenceRatio(): BigDecimal {
    require(usdPrice.signum() > 0) { "Cannot compute a confidence ratio for non-positive usdPrice=$usdPrice" }
    return confidenceUsd.divide(usdPrice, 6, RoundingMode.HALF_UP)
}

/** Ages from the publish time, not from when we happened to fetch it. */
fun PythPrice.millisUntilStale(maxPriceAge: Duration, now: Instant): Long =
    Instant.ofEpochSecond(publishTime).plus(maxPriceAge).toEpochMilli() - now.toEpochMilli()

sealed class PythAuditedPriceResult {
    data class Trusted(val price: PythPrice) : PythAuditedPriceResult()

    data class TooUncertain(
        val price: PythPrice,
        val confidenceRatio: BigDecimal?,
        val maxAllowedRatio: BigDecimal,
    ) : PythAuditedPriceResult()

    data class Stale(
        val price: PythPrice,
        val ageSeconds: Long,
        val maxAgeSeconds: Long,
    ) : PythAuditedPriceResult()
}
