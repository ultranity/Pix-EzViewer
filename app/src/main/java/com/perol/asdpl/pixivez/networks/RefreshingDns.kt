package com.perol.asdpl.pixivez.networks

import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap

/** Per-host snapshots. Only the first caller refreshes; DNS failures never discard usable results. */
internal class RefreshingDns(
    private val upstream: Dns,
    private val fallback: (String) -> List<InetAddress>,
    private val accepts: (String) -> Boolean,
    private val system: Dns = Dns.SYSTEM,
    private val preferred: () -> List<InetAddress> = { emptyList() },
    private val order: (List<InetAddress>) -> List<InetAddress> = { it },
    private val clock: () -> Long = System::currentTimeMillis,
    private val load: (String) -> Saved? = { null },
    private val save: (String, Saved) -> Unit = { _, _ -> },
    private val refreshMs: Long = 5 * 60_000L,
    private val retryMs: Long = 60_000L,
    private val staleMs: Long = 24 * 60 * 60_000L,
) : Dns {
    data class Saved(val at: Long, val addresses: List<InetAddress>)
    private class State(@Volatile var saved: Saved?, var nextRefresh: Long = 0, var lastInvalidated: Long? = null)
    private val states = ConcurrentHashMap<String, State>()

    override fun lookup(hostname: String): List<InetAddress> {
        val host = hostname.lowercase().removeSuffix(".")
        if (!accepts(host)) return system.lookup(hostname)
        val state = states.getOrPut(host) { State(runCatching { load(host) }.getOrNull()) }
        synchronized(state) {
            val now = clock()
            if (now >= state.nextRefresh) {
                try {
                    val fresh = upstream.lookup(host).distinct()
                    if (fresh.isEmpty()) throw UnknownHostException("Empty DNS answer for $host")
                    state.saved = Saved(now, fresh)
                    state.nextRefresh = now + refreshMs
                    runCatching { save(host, state.saved!!) }
                } catch (_: Exception) {
                    // Malformed custom resolver settings must also preserve usable fallback routes.
                    state.nextRefresh = now + retryMs
                }
            }
            return addresses(host, state.saved).ifEmpty {
                throw UnknownHostException("No addresses for $host")
            }
        }
    }

    /** A failed connection can expire an answer, but at most once per retry interval. */
    fun invalidate(hostname: String) {
        val state = states[hostname.lowercase().removeSuffix(".")] ?: return
        synchronized(state) {
            val now = clock()
            if (state.lastInvalidated?.let { now - it < retryMs } == true) return
            state.lastInvalidated = now
            state.nextRefresh = 0
        }
    }

    /** Non-blocking route snapshot for legacy URL rewriting on the UI thread. */
    fun cached(hostname: String): List<InetAddress> {
        val host = hostname.lowercase().removeSuffix(".")
        if (!accepts(host)) return emptyList()
        val state = states.getOrPut(host) { State(runCatching { load(host) }.getOrNull()) }
        return addresses(host, state.saved)
    }

    private fun addresses(host: String, saved: Saved?): List<InetAddress> {
        val cached = saved?.takeIf { clock() - it.at in 0..staleMs }?.addresses.orEmpty()
        // Retain fallback routes so OkHttp can try another address after a connection failure.
        return (preferred() + order(cached) + order(fallback(host))).distinct()
    }

    fun snapshot(): List<InetAddress> = states.values.flatMap { it.saved?.addresses.orEmpty() }.distinct()
}

/** OkHttp 4.12's Kotlin Dns is not a fun interface. */
internal class DnsLookup(private val resolve: (String) -> List<InetAddress>) : Dns {
    override fun lookup(hostname: String): List<InetAddress> = resolve(hostname)
}
