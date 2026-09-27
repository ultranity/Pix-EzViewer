package com.perol.asdpl.pixivez.networks

import okhttp3.Dns
import org.junit.Assert.*
import org.junit.Test
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class RefreshingDnsTest {
    private fun ip(last: Int) = InetAddress.getByName("192.0.2.$last")
    private fun resolver(upstream: Dns, clock: () -> Long = { 100L }) = RefreshingDns(
        upstream, { listOf(ip(9)) }, { it.endsWith(".pximg.net") }, clock = clock,
        refreshMs = 10, retryMs = 5, staleMs = 30,
    )

    @Test fun refreshesRotatedAddressesWithoutManualIntervention() {
        var now = 100L
        var calls = 0
        val dns = resolver(DnsLookup { listOf(ip(++calls)) }) { now }
        assertEquals(listOf(ip(1), ip(9)), dns.lookup("i.pximg.net"))
        now += 9
        assertEquals(ip(1), dns.lookup("i.pximg.net").first())
        now++
        assertEquals(ip(2), dns.lookup("i.pximg.net").first())
        assertEquals(2, calls)
    }

    @Test fun usesLastGoodDuringOutageBacksOffThenRecovers() {
        var now = 100L
        var fails = false
        var calls = 0
        val dns = resolver(DnsLookup {
            calls++
            if (fails) throw UnknownHostException("offline")
            listOf(ip(calls))
        }) { now }
        dns.lookup("i.pximg.net")
        fails = true
        now += 10
        assertEquals(ip(1), dns.lookup("i.pximg.net").first())
        repeat(10) { dns.lookup("i.pximg.net") }
        assertEquals(2, calls)
        fails = false
        now += 5
        assertEquals(ip(3), dns.lookup("i.pximg.net").first())
    }

    @Test fun invalidCustomResolverDoesNotCrashOrDiscardFallback() {
        val dns = resolver(DnsLookup { throw IllegalArgumentException("DoH requires HTTPS") })
        assertEquals(listOf(ip(9)), dns.lookup("i.pximg.net"))
    }

    @Test fun emptyAnswerAndExpiredSavedAnswerUseFallback() {
        val dns = RefreshingDns(DnsLookup { emptyList() }, { listOf(ip(9)) }, { true },
            clock = { 200L }, load = { RefreshingDns.Saved(100, listOf(ip(1))) }, staleMs = 50)
        assertEquals(listOf(ip(9)), dns.lookup("i.pximg.net"))
    }

    @Test fun persistedAnswerSurvivesRestartAndResolverFailure() {
        var saved: RefreshingDns.Saved? = null
        val first = RefreshingDns(DnsLookup { listOf(ip(1)) }, { emptyList() }, { true },
            clock = { 100L }, save = { _, result -> saved = result })
        first.lookup("i.pximg.net")
        val restarted = RefreshingDns(DnsLookup { throw UnknownHostException() }, { listOf(ip(9)) },
            { true }, clock = { 200L }, load = { saved })
        assertEquals(listOf(ip(1), ip(9)), restarted.lookup("i.pximg.net"))
    }

    @Test fun hostPoolsAreSeparateAndUnrelatedHostsUseSystemDns() {
        val dns = RefreshingDns(DnsLookup { if (it == "i.pximg.net") listOf(ip(1)) else listOf(ip(2)) },
            { emptyList() }, { it.endsWith(".pximg.net") }, system = DnsLookup { listOf(ip(3)) })
        assertEquals(listOf(ip(1)), dns.lookup("i.pximg.net"))
        assertEquals(listOf(ip(2)), dns.lookup("s.pximg.net"))
        assertEquals(listOf(ip(3)), dns.lookup("mirror.example.org"))
    }

    @Test fun connectionFailureExpiresCacheWithRateLimit() {
        var now = 100L
        var calls = 0
        val dns = resolver(DnsLookup { listOf(ip(++calls)) }) { now }
        dns.lookup("i.pximg.net")
        dns.invalidate("i.pximg.net")
        assertEquals(ip(2), dns.lookup("i.pximg.net").first())
        repeat(10) { dns.invalidate("i.pximg.net"); dns.lookup("i.pximg.net") }
        assertEquals(2, calls)
        now += 5
        dns.invalidate("i.pximg.net")
        assertEquals(ip(3), dns.lookup("i.pximg.net").first())
    }

    @Test fun concurrentColdRequestsShareOneRefreshAndReturnIndependentLists() {
        var calls = 0
        val dns = resolver(DnsLookup { calls++; Thread.sleep(20); listOf(ip(1)) })
        val pool = Executors.newFixedThreadPool(8)
        try {
            val answers = pool.invokeAll(List(20) { Callable { dns.lookup("i.pximg.net") } })
                .map { it.get(2, TimeUnit.SECONDS) }
            assertEquals(1, calls)
            (answers.first() as MutableList).clear()
            assertEquals(listOf(ip(1), ip(9)), dns.lookup("i.pximg.net"))
        } finally { pool.shutdownNow() }
    }

    @Test fun explicitAddressesArePreferredAndRefreshedWhenChanged() {
        var custom = listOf(ip(7))
        val dns = RefreshingDns(DnsLookup { listOf(ip(1), ip(1)) }, { listOf(ip(9)) }, { true },
            preferred = { custom })
        assertEquals(listOf(ip(7), ip(1), ip(9)), dns.lookup("i.pximg.net"))
        custom = emptyList()
        assertEquals(listOf(ip(1), ip(9)), dns.lookup("i.pximg.net"))
    }
    @Test fun cachedUrlLookupNeverPerformsNetworkIoOrResolvesMirrorHosts() {
        var calls = 0
        val dns = resolver(DnsLookup { calls++; listOf(ip(1)) })
        assertEquals(listOf(ip(9)), dns.cached("i.pximg.net"))
        assertTrue(dns.cached("mirror.example.org").isEmpty())
        assertEquals(0, calls)
        dns.lookup("i.pximg.net")
        assertEquals(listOf(ip(1), ip(9)), dns.cached("i.pximg.net"))
        assertEquals(1, calls)
    }

    @Test fun cachedUrlLookupDoesNotWaitForAnInFlightRefresh() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val dns = resolver(DnsLookup {
            entered.countDown()
            check(release.await(3, TimeUnit.SECONDS))
            listOf(ip(1))
        })
        val pool = Executors.newFixedThreadPool(2)
        try {
            val refresh = pool.submit<List<InetAddress>> { dns.lookup("i.pximg.net") }
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            val cached = pool.submit<List<InetAddress>> { dns.cached("i.pximg.net") }
            assertEquals(listOf(ip(9)), cached.get(1, TimeUnit.SECONDS))
            release.countDown()
            assertEquals(ip(1), refresh.get(2, TimeUnit.SECONDS).first())
        } finally {
            release.countDown()
            pool.shutdownNow()
        }
    }

}
