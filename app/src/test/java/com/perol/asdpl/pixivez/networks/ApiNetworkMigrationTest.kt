package com.perol.asdpl.pixivez.networks

import org.junit.Assert.*
import org.junit.Test

class ApiNetworkMigrationTest {
    @Test fun replacesOnlyTheOldBuiltInOriginProfile() {
        assertTrue(shouldMigrateLegacyApiNetwork(null, null, null, null))
        assertTrue(shouldMigrateLegacyApiNetwork("direct", "replace", "", "pixiv.me"))
        assertTrue(shouldMigrateLegacyApiNetwork("direct", "empty", null, null))
        assertFalse(shouldMigrateLegacyApiNetwork("system", "plain", null, null))
        assertFalse(shouldMigrateLegacyApiNetwork("doh", "plain", null, null))
        assertFalse(shouldMigrateLegacyApiNetwork("direct", "replace", "1.2.3.4", null))
        assertFalse(shouldMigrateLegacyApiNetwork("direct", "replace", null, "custom.example"))
        assertFalse(shouldMigrateLegacyApiNetwork("direct", "ech", null, null))
    }
}
