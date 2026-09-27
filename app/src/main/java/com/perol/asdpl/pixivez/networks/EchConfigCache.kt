package com.perol.asdpl.pixivez.networks

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import okio.ByteString.Companion.decodeBase64
import java.io.IOException

/** The ECH config and the address hints published in one HTTPS DNS answer. */
data class EchConfig(
    val bytes: ByteArray,
    val ipv4Hints: List<String>,
    val ttlSeconds: Long,
)

/** Parses the JSON representation returned by a DNS-over-HTTPS HTTPS query. */
object EchConfigParser {
    private val BASE64 = Regex("[A-Za-z0-9+/]+={0,2}")
    private const val HTTPS_RECORD_TYPE = 65

    fun parse(json: String): EchConfig {
        val root = try {
            Json.parseToJsonElement(json) as? JsonObject
                ?: throw IOException("ECH DNS response is not an object")
        } catch (error: IOException) {
            throw error
        } catch (error: Exception) {
            throw IOException("Invalid ECH DNS response JSON", error)
        }

        root["Status"]?.let { statusElement ->
            val status = (statusElement as? JsonPrimitive)?.longOrNull
                ?: throw IOException("ECH DNS response has an invalid status")
            if (status != 0L) throw IOException("ECH DNS response status=$status")
        }

        val answers = root["Answer"] as? JsonArray
            ?: throw IOException("ECH DNS response has no Answer array")

        for (answer in answers) {
            val record = answer as? JsonObject ?: throw IOException("Invalid DNS answer record")
            val type = (record["type"] as? JsonPrimitive)?.intOrNull ?: continue
            if (type != HTTPS_RECORD_TYPE) continue

            val data = (record["data"] as? JsonPrimitive)?.content
                ?: throw IOException("HTTPS answer has no data")
            val params = parseSvcParams(data)
            val encoded = params["ech"] ?: continue
            val ttl = (record["TTL"] as? JsonPrimitive)?.longOrNull
                ?: throw IOException("HTTPS answer has no valid TTL")
            if (ttl < 0L) throw IOException("HTTPS answer has a negative TTL")

            return EchConfig(
                bytes = decodeEchConfig(encoded),
                ipv4Hints = parseIpv4Hints(params["ipv4hint"]),
                ttlSeconds = ttl,
            )
        }

        throw IOException("ECH DNS response has no ECH config")
    }

    private fun parseSvcParams(data: String): Map<String, String> {
        val params = linkedMapOf<String, String>()
        var index = 0
        while (true) {
            while (index < data.length && data[index].isWhitespace()) index++
            if (index == data.length) break

            val keyStart = index
            while (index < data.length && !data[index].isWhitespace() && data[index] != '=') index++
            if (index == keyStart) throw IOException("Invalid HTTPS SVCB parameter")
            val key = data.substring(keyStart, index).lowercase()

            // SVCB flags such as "no-default-alpn" have no value. They are irrelevant here.
            if (index == data.length || data[index] != '=') {
                while (index < data.length && !data[index].isWhitespace()) index++
                continue
            }
            index++

            val value = if (index < data.length && data[index] == '"') {
                index++
                val valueBuilder = StringBuilder()
                var closed = false
                while (index < data.length) {
                    val character = data[index++]
                    when {
                        character == '"' -> {
                            closed = true
                            break
                        }
                        character == '\\' && index < data.length -> valueBuilder.append(data[index++])
                        else -> valueBuilder.append(character)
                    }
                }
                if (!closed || (index < data.length && !data[index].isWhitespace())) {
                    throw IOException("Unterminated HTTPS SVCB value")
                }
                valueBuilder.toString()
            } else {
                val valueStart = index
                while (index < data.length && !data[index].isWhitespace()) index++
                data.substring(valueStart, index)
            }

            if ((key == "ech" || key == "ipv4hint") && params.containsKey(key)) {
                throw IOException("Duplicate HTTPS SVCB parameter: $key")
            }
            params[key] = value
        }
        return params
    }

    private fun decodeEchConfig(encoded: String): ByteArray {
        if (encoded.isEmpty() || encoded.length % 4 != 0 || !BASE64.matches(encoded)) {
            throw IOException("Invalid ECH base64")
        }
        val decoded = encoded.decodeBase64()
            ?: throw IOException("Invalid ECH base64")
        // Reject non-canonical padding/trailing bits accepted by permissive decoders.
        if (decoded.base64() != encoded) throw IOException("Non-canonical ECH base64")

        val bytes = decoded.toByteArray()
        if (bytes.size < 6) throw IOException("ECH config list is truncated")
        val listLength = readUnsignedShort(bytes, 0)
        if (listLength != bytes.size - 2 || listLength == 0) {
            throw IOException("ECH config list length does not match payload")
        }

        var offset = 2
        var entries = 0
        while (offset < bytes.size) {
            if (bytes.size - offset < 4) throw IOException("ECH config entry is truncated")
            val configLength = readUnsignedShort(bytes, offset + 2)
            if (configLength == 0 || configLength > bytes.size - offset - 4) {
                throw IOException("ECH config entry length is invalid")
            }
            offset += 4 + configLength
            entries++
        }
        if (offset != bytes.size || entries == 0) {
            throw IOException("ECH config list framing is invalid")
        }
        return bytes
    }

    private fun parseIpv4Hints(value: String?): List<String> {
        if (value == null) return emptyList()
        if (value.isEmpty()) throw IOException("Empty IPv4 hint")

        return value.split(',').map { hint ->
            val address = hint.trim()
            val octets = address.split('.')
            if (octets.size != 4 || octets.any { it.isEmpty() || it.length > 3 || !it.all(Char::isDigit) }) {
                throw IOException("Invalid IPv4 hint: $address")
            }
            if (octets.any { it.toIntOrNull() !in 0..255 }) {
                throw IOException("Invalid IPv4 hint: $address")
            }
            address
        }.distinct()
    }

    private fun readUnsignedShort(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xff) shl 8) or (bytes[offset + 1].toInt() and 0xff)
}

/**
 * Synchronized ECH config cache. A refresh is performed while holding the monitor, so concurrent
 * cold callers share one loader invocation. Failed refreshes back off while retaining the last
 * good config for at most one day.
 */
class EchConfigCache(
    private val loader: () -> EchConfig,
    private val now: () -> Long = { System.nanoTime() / 1_000_000L },
) {
    private companion object {
        const val MAX_AGE_MS = 24L * 60 * 60 * 1_000
        const val FAILURE_BACKOFF_MS = 30L * 1_000
        const val INVALIDATE_INTERVAL_MS = 60L * 1_000
    }

    private val monitor = Any()
    private var fresh: EchConfig? = null
    private var freshUntilMs = 0L
    private var lastGood: EchConfig? = null
    private var lastGoodAtMs: Long? = null
    private var retryAfterMs: Long? = null
    private var lastFailure: IOException? = null
    private var lastInvalidatedAtMs: Long? = null

    fun get(): EchConfig = synchronized(monitor) {
        val timestamp = now()
        fresh?.let { value ->
            if (timestamp < freshUntilMs) return value
        }

        if (retryAfterMs?.let { timestamp < it } == true) {
            return usableLastGood(timestamp) ?: throw lastFailureOrDefault()
        }

        fresh = null
        freshUntilMs = 0L
        try {
            val loaded = loader()
            lastGood = loaded
            lastGoodAtMs = timestamp
            fresh = loaded
            freshUntilMs = expiry(timestamp, loaded.ttlSeconds)
            retryAfterMs = null
            lastFailure = null
            loaded
        } catch (error: Exception) {
            val failure = if (error is IOException) error else IOException("ECH config refresh failed", error)
            lastFailure = failure
            retryAfterMs = plus(timestamp, FAILURE_BACKOFF_MS)
            usableLastGood(timestamp) ?: throw failure
        }
    }

    /** Clears only the fresh snapshot. Returns false when the 60-second invalidation limit applies. */
    fun invalidate(): Boolean = synchronized(monitor) {
        val timestamp = now()
        val previous = lastInvalidatedAtMs
        if (previous != null && timestamp >= previous && timestamp - previous < INVALIDATE_INTERVAL_MS) {
            return false
        }
        lastInvalidatedAtMs = timestamp
        fresh = null
        freshUntilMs = 0L
        // An explicit invalidation is the caller's request to try once now; failed attempts still
        // establish the normal 30-second refresh backoff.
        retryAfterMs = null
        true
    }

    private fun usableLastGood(timestamp: Long): EchConfig? {
        val savedAt = lastGoodAtMs ?: return null
        if (timestamp < savedAt || timestamp - savedAt > MAX_AGE_MS) return null
        return lastGood
    }

    private fun expiry(timestamp: Long, ttlSeconds: Long): Long {
        if (ttlSeconds <= 0L) return timestamp
        val boundedSeconds = minOf(ttlSeconds, MAX_AGE_MS / 1_000)
        return plus(timestamp, boundedSeconds * 1_000)
    }

    private fun plus(base: Long, delta: Long): Long =
        if (base > Long.MAX_VALUE - delta) Long.MAX_VALUE else base + delta

    private fun lastFailureOrDefault(): IOException =
        lastFailure ?: IOException("ECH config refresh is backing off")
}
