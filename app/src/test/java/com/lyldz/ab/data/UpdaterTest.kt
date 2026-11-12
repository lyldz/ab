package com.lyldz.ab.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdaterTest {
    @Test fun versionComparison() {
        assertTrue(Updater.isNewer("v0.3.0", "0.2.0"))
        assertTrue(Updater.isNewer("0.2.1", "0.2.0"))
        assertTrue(Updater.isNewer("v1.0.0", "0.9.9"))
        assertFalse(Updater.isNewer("v0.2.0", "0.2.0"))
        assertFalse(Updater.isNewer("0.1.9", "0.2.0"))
        assertTrue(Updater.isNewer("0.10.0", "0.9.0")) // sayısal, metin sırası değil
    }

    @Test fun parsesLatestReleaseAndPicksApk() {
        val json = """{"tag_name":"v0.3.0","body":"## [0.3.0]\n### Added\n- **Yeni:** bir şey\n- ikinci","assets":[
            {"name":"notes.txt","browser_download_url":"https://x/notes.txt","size":5},
            {"name":"app-release.apk","browser_download_url":"https://x/app-release.apk","size":8048136}]}"""
        val info = Updater.parseRelease(json)!!
        assertEquals("v0.3.0", info.version)
        assertEquals("https://x/app-release.apk", info.downloadUrl)
        assertEquals(8048136L, info.sizeBytes)
        assertEquals("• Yeni: bir şey\n• ikinci", info.notes)
    }

    @Test fun noApkOrBadJsonGivesNull() {
        assertNull(Updater.parseRelease("""{"tag_name":"v1","assets":[]}"""))
        assertNull(Updater.parseRelease("not json"))
    }
}
