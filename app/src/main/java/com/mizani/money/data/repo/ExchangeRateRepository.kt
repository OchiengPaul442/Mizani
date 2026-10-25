package com.mizani.money.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

private val Context.exchangeRateDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mizani_exchange_rates",
)

data class ExchangeRate(
    val baseCurrencyCode: String,
    val quoteCurrencyCode: String,
    /** Decimal text in quote major units per one base major unit. */
    val rate: String,
    /** Latest observation date supplied by the rate provider, not the fetch time. */
    val date: LocalDate,
)

/** The provider confirmed that no reference rate exists for the requested pair. */
class ExchangeRateUnavailableException(
    val baseCurrencyCode: String,
    val quoteCurrencyCode: String,
) : IOException("No reference rate is available for $baseCurrencyCode/$quoteCurrencyCode")

/**
 * Fetches public daily reference rates. It only sends currency codes and keeps a small,
 * app-private cache so a previously used pair remains available without a connection.
 */
class ExchangeRateRepository(
    context: Context,
    private val clock: Clock = Clock.systemUTC(),
    private val connectionFactory: (URL) -> HttpURLConnection = {
        it.openConnection() as HttpURLConnection
    },
) {
    private data class CachedRate(val rate: ExchangeRate, val fetchedAtMillis: Long)

    private val dataStore = context.applicationContext.exchangeRateDataStore
    private val cacheMutex = Mutex()
    private val cache = mutableMapOf<Pair<String, String>, CachedRate>()
    private val refreshMutex = Mutex()
    private val pairRefreshLocks = mutableMapOf<Pair<String, String>, Mutex>()
    private val requestScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Returns the latest saved observation for this pair if it is still within the safe age. */
    suspend fun cachedRate(baseCurrencyCode: String, quoteCurrencyCode: String): ExchangeRate? {
        val (base, quote) = normalizePair(baseCurrencyCode, quoteCurrencyCode)
        return readCached(base to quote)?.rate
    }

    /**
     * Keeps repeat visits fast while allowing one refresh after the daily rate may have changed.
     * Call [refreshRate] directly for a user-requested refresh.
     */
    suspend fun shouldRefresh(baseCurrencyCode: String, quoteCurrencyCode: String): Boolean {
        val (base, quote) = normalizePair(baseCurrencyCode, quoteCurrencyCode)
        val cached = readCached(base to quote) ?: return true
        val age = clock.millis() - cached.fetchedAtMillis
        return age < 0L || age >= CACHE_TTL_MILLIS
    }

    /** Fetches and saves a fresh observation. Callers may keep using [cachedRate] if this fails. */
    suspend fun refreshRate(baseCurrencyCode: String, quoteCurrencyCode: String): ExchangeRate {
        val (base, quote) = normalizePair(baseCurrencyCode, quoteCurrencyCode)
        val key = base to quote
        val requestStartedAt = clock.millis()
        val pairLock = refreshMutex.withLock { pairRefreshLocks.getOrPut(key) { Mutex() } }

        return pairLock.withLock {
            // If another screen refreshed this pair while this request was queued, reuse it.
            val cached = readCached(key)
            if (cached != null && cached.fetchedAtMillis >= requestStartedAt) return@withLock cached.rate

            val rate = fetchLatestRate(base, quote)
            validateRateDate(rate.date, clock)
            val stored = CachedRate(rate, clock.millis())
            cacheMutex.withLock { cache[key] = stored }
            persist(key, stored)
            rate
        }
    }

    private suspend fun readCached(key: Pair<String, String>): CachedRate? {
        val inMemory = cacheMutex.withLock { cache[key] }
        if (inMemory != null) return inMemory.takeIf { it.rate.isWithinAllowedAge(clock) }

        val (base, quote) = key
        val rateKey = stringPreferencesKey(rateKeyName(base, quote))
        val dateKey = stringPreferencesKey(dateKeyName(base, quote))
        val fetchedAtKey = longPreferencesKey(fetchedAtKeyName(base, quote))
        val preferences = withContext(Dispatchers.IO) {
            dataStore.data
                .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
                .first()
        }
        val rateText = preferences[rateKey] ?: return null
        val date = preferences[dateKey]?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        val decimalRate = rateText.toBigDecimalOrNull()?.takeIf { it.signum() > 0 } ?: return null
        val fetchedAt = preferences[fetchedAtKey] ?: return null
        val cached = CachedRate(
            rate = ExchangeRate(base, quote, decimalRate.stripTrailingZeros().toPlainString(), date),
            fetchedAtMillis = fetchedAt,
        )
        if (!cached.rate.isWithinAllowedAge(clock)) return null
        cacheMutex.withLock { cache[key] = cached }
        return cached
    }

    private suspend fun persist(key: Pair<String, String>, cached: CachedRate) {
        val (base, quote) = key
        try {
            dataStore.edit { preferences ->
                preferences[stringPreferencesKey(rateKeyName(base, quote))] = cached.rate.rate
                preferences[stringPreferencesKey(dateKeyName(base, quote))] = cached.rate.date.toString()
                preferences[longPreferencesKey(fetchedAtKeyName(base, quote))] = cached.fetchedAtMillis
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            // The just-fetched value remains usable in memory for this app session.
        }
    }

    private suspend fun fetchLatestRate(base: String, quote: String): ExchangeRate =
        withTimeout(REQUEST_TIMEOUT_MILLIS) {
            suspendCancellableCoroutine { continuation ->
                val connection = connectionFactory(URL("$API_BASE/rate/$base/$quote"))
                connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
                connection.readTimeout = READ_TIMEOUT_MILLIS
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                // Keep the request on the documented HTTPS host; never forward it through a redirect.
                connection.instanceFollowRedirects = false

                val request = requestScope.launch(start = CoroutineStart.LAZY) {
                    try {
                        continuation.resume(readLatestRate(connection, base, quote))
                    } catch (error: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                }
                continuation.invokeOnCancellation {
                    request.cancel()
                    // HttpURLConnection is blocking; disconnect closes the socket on timeout/cancel.
                    connection.disconnect()
                }
                request.start()
            }
        }

    private fun readLatestRate(connection: HttpURLConnection, base: String, quote: String): ExchangeRate {
        try {
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND || responseCode == 422) {
                throw ExchangeRateUnavailableException(base, quote)
            }
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Exchange-rate service returned HTTP $responseCode")
            }
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val output = StringBuilder()
                val buffer = CharArray(2_048)
                while (true) {
                    val count = reader.read(buffer)
                    if (count < 0) break
                    if (output.length + count > MAX_RESPONSE_CHARS) {
                        throw IOException("Exchange-rate response is too large")
                    }
                    output.append(buffer, 0, count)
                }
                output.toString()
            }
            val json = JSONObject(body)
            val responseBase = json.getString("base").uppercase(Locale.ROOT)
            val responseQuote = json.getString("quote").uppercase(Locale.ROOT)
            require(responseBase == base && responseQuote == quote) { "Currency pair did not match request" }
            val decimalRate = json.get("rate").toString().toBigDecimalOrNull()
                ?: throw IOException("Exchange-rate response had an invalid rate")
            require(decimalRate.signum() > 0) { "Exchange-rate response had a non-positive rate" }
            val date = LocalDate.parse(json.getString("date"))

            return ExchangeRate(
                baseCurrencyCode = base,
                quoteCurrencyCode = quote,
                rate = decimalRate.stripTrailingZeros().toPlainString(),
                date = date,
            )
        } catch (error: JSONException) {
            throw IOException("Exchange-rate response was invalid", error)
        } finally {
            connection.disconnect()
        }
    }

    private fun normalizePair(baseCurrencyCode: String, quoteCurrencyCode: String): Pair<String, String> {
        val base = baseCurrencyCode.uppercase(Locale.ROOT)
        val quote = quoteCurrencyCode.uppercase(Locale.ROOT)
        require(CURRENCY_CODE.matches(base) && CURRENCY_CODE.matches(quote)) { "Invalid currency code" }
        require(base != quote) { "Currencies must be different" }
        return base to quote
    }

    private fun rateKeyName(base: String, quote: String) = "pair_${base}_${quote}_rate"
    private fun dateKeyName(base: String, quote: String) = "pair_${base}_${quote}_date"
    private fun fetchedAtKeyName(base: String, quote: String) = "pair_${base}_${quote}_fetched_at"

    companion object {
        private const val API_BASE = "https://api.frankfurter.dev/v2"
        // A bounded wait matters on unstable networks. Saved rates remain usable during refresh.
        private const val CONNECT_TIMEOUT_MILLIS = 4_000
        private const val READ_TIMEOUT_MILLIS = 4_000
        private const val REQUEST_TIMEOUT_MILLIS = 8_000L
        private const val MAX_RESPONSE_CHARS = 16_384
        // Rates are daily observations; avoid repeated requests while checking again during the day.
        private const val CACHE_TTL_MILLIS = 12 * 60 * 60 * 1_000L
        private val CURRENCY_CODE = Regex("^[A-Z]{3}$")
    }
}

internal fun validateRateDate(date: LocalDate, clock: Clock = Clock.systemUTC()) {
    val today = LocalDate.now(clock.withZone(ZoneOffset.UTC))
    require(!date.isAfter(today)) { "Exchange rate date is in the future" }
    require(!date.isBefore(today.minusDays(MAX_RATE_AGE_DAYS))) { "Exchange rate is too old" }
}

private fun ExchangeRate.isWithinAllowedAge(clock: Clock): Boolean =
    runCatching { validateRateDate(date, clock) }.isSuccess

private const val MAX_RATE_AGE_DAYS = 7L
