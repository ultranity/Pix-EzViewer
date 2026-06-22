package com.perol.asdpl.pixivez.networks.bypass

import com.perol.asdpl.pixivez.networks.SniMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.InetAddress

class BypassResolverTest {
    private fun ip(s: String) = InetAddress.getByName(s)

    @Test fun picks_first_probe_ok_candidate() {
        val cands = listOf(
            Endpoint(ip("1.1.1.1"), SniMode.EMPTY, null, true),
            Endpoint(ip("2.2.2.2"), SniMode.EMPTY, null, true),
        )
        val prober = Prober { addr, _, _, _ -> addr.hostAddress == "2.2.2.2" }
        assertEquals("2.2.2.2", BypassResolver.pick(cands, "h", prober)?.ip?.hostAddress)
    }

    @Test fun returns_null_when_none_ok() {
        val cands = listOf(Endpoint(ip("1.1.1.1"), SniMode.EMPTY, null, true))
        assertNull(BypassResolver.pick(cands, "h", { _, _, _, _ -> false }))
    }
}
