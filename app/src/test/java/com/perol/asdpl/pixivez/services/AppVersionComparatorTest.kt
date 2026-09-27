package com.perol.asdpl.pixivez.services

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionComparatorTest {
    @Test
    fun equalReleaseTagWithVPrefixDoesNotPrompt() {
        assertFalse(AppVersionComparator.isRemoteNewer("2.2.5", "v2.2.5"))
    }

    @Test
    fun numericComponentsUseNumericOrdering() {
        assertTrue(AppVersionComparator.isRemoteNewer("2.9.0", "v2.10.0"))
        assertFalse(AppVersionComparator.isRemoteNewer("2.10.0", "v2.9.0"))
        assertFalse(AppVersionComparator.isRemoteNewer("2.2.6", "v2.2.5"))
    }

    @Test
    fun stableReleaseIsNewerThanItsPreRelease() {
        assertTrue(AppVersionComparator.isRemoteNewer("2.2.5-rc.1", "v2.2.5"))
        assertFalse(AppVersionComparator.isRemoteNewer("2.2.5", "v2.2.5-rc.1"))
    }

    @Test
    fun preReleaseIdentifiersFollowSemanticOrdering() {
        assertTrue(AppVersionComparator.isRemoteNewer("2.2.5-alpha.9", "v2.2.5-alpha.10"))
        assertTrue(AppVersionComparator.isRemoteNewer("2.2.5-alpha", "v2.2.5-beta"))
        assertFalse(AppVersionComparator.isRemoteNewer("2.2.5-beta", "v2.2.5-alpha"))
    }

    @Test
    fun buildMetadataDoesNotChangePrecedence() {
        assertFalse(AppVersionComparator.isRemoteNewer("2.2.5+local.1", "v2.2.5+release.2"))
    }

    @Test
    fun localDebugSuffixMatchesTheUnderlyingRelease() {
        assertFalse(AppVersionComparator.isRemoteNewer("2.2.5-debug", "v2.2.5"))
        assertTrue(AppVersionComparator.isRemoteNewer("2.2.5-debug", "v2.2.6"))
    }

    @Test
    fun malformedVersionsDoNotPrompt() {
        assertFalse(AppVersionComparator.isRemoteNewer("2.2.5", "latest"))
        assertFalse(AppVersionComparator.isRemoteNewer("2.2", "v2..3"))
        assertFalse(AppVersionComparator.isRemoteNewer("2.2.5", "v2.2.5+"))
        assertFalse(AppVersionComparator.isRemoteNewer("2.2.5", "v2.2.5-"))
        assertFalse(AppVersionComparator.isRemoteNewer("2.2.5", "v2.2.5-01"))
        assertFalse(AppVersionComparator.isRemoteNewer("2.2.5+build", "v02.2.6"))
        assertFalse(AppVersionComparator.isRemoteNewer("2.2.5", "V2.2.6"))
        assertFalse(AppVersionComparator.isRemoteNewer("2.2.5", " v2.2.6"))
    }
}
