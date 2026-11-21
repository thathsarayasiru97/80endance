package com.example

import com.example.system.AppUpdateChecker
import com.example.system.UpdateCheckResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppUpdateCheckerTest {

    @Test
    fun isNewerVersion_handlesStandardAndEdgeCaseTags() {
        assertTrue(AppUpdateChecker.isNewerVersion("v1.1", "1.0"))
        assertTrue(AppUpdateChecker.isNewerVersion("1.10", "1.2"))
        assertTrue(AppUpdateChecker.isNewerVersion("v2.0.0", "1.9.9"))
        assertTrue(AppUpdateChecker.isNewerVersion("1.0.1", "1.0"))
        assertTrue(AppUpdateChecker.isNewerVersion("V1.2.0", "v1.1.9"))

        assertFalse(AppUpdateChecker.isNewerVersion("v1.0.0", "1.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("1.0", "1.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("0.9.9", "1.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("v1.0-build.5", "1.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("", "1.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("not-a-version", "1.0"))
    }

    @Test
    fun formatVersionLabel_normalizesLeadingV() {
        assertEquals("v1.0", AppUpdateChecker.formatVersionLabel("1.0"))
        assertEquals("v1.2.3", AppUpdateChecker.formatVersionLabel("v1.2.3"))
        assertEquals("v2.0", AppUpdateChecker.formatVersionLabel("V2.0"))
        assertEquals("v1.0", AppUpdateChecker.formatVersionLabel(""))
    }

    @Test
    fun parseReleaseJson_returnsUpdateAvailableForNewerStableRelease() {
        val json = """
            {
              "tag_name": "v1.2.0",
              "name": "80endance 1.2.0",
              "body": "- Liquid Glass refresh\n- Faster sync",
              "html_url": "https://github.com/thathsarayasiru97/80endence/releases/tag/v1.2.0",
              "draft": false,
              "prerelease": false
            }
        """.trimIndent()

        val result = AppUpdateChecker.parseReleaseJson(json, currentVersion = "1.0")
        assertTrue(result is UpdateCheckResult.UpdateAvailable)
        val info = (result as UpdateCheckResult.UpdateAvailable).info
        assertEquals("1.0", info.currentVersion)
        assertEquals("v1.2.0", info.latestVersion)
        assertEquals("80endance 1.2.0", info.releaseTitle)
        assertEquals(
            "https://github.com/thathsarayasiru97/80endence/releases/tag/v1.2.0",
            info.releaseUrl
        )
    }

    @Test
    fun parseReleaseJson_returnsUpToDateWhenSameOrOlderOrPrerelease() {
        val sameVersionJson = """
            {
              "tag_name": "v1.0",
              "name": "80endance 1.0",
              "html_url": "https://github.com/thathsarayasiru97/80endence/releases/tag/v1.0",
              "draft": false,
              "prerelease": false
            }
        """.trimIndent()
        assertTrue(
            AppUpdateChecker.parseReleaseJson(sameVersionJson, "1.0") is UpdateCheckResult.UpToDate
        )

        val prereleaseJson = """
            {
              "tag_name": "v2.0",
              "name": "Beta 2.0",
              "html_url": "https://github.com/thathsarayasiru97/80endence/releases/tag/v2.0",
              "draft": false,
              "prerelease": true
            }
        """.trimIndent()
        assertTrue(
            AppUpdateChecker.parseReleaseJson(prereleaseJson, "1.0") is UpdateCheckResult.UpToDate
        )
    }

    @Test
    fun parseReleaseJson_fallsBackToRepoLatestUrlIfHtmlUrlUnexpected() {
        val json = """
            {
              "tag_name": "v1.1",
              "name": "Release 1.1",
              "html_url": "https://malicious.example.com/fake",
              "draft": false,
              "prerelease": false
            }
        """.trimIndent()

        val result = AppUpdateChecker.parseReleaseJson(json, "1.0") as UpdateCheckResult.UpdateAvailable
        assertEquals(AppUpdateChecker.GITHUB_LATEST_RELEASE_WEB, result.info.releaseUrl)
    }

    @Test
    fun isNewerVersion_handlesPrefixedTagsAndPrereleaseOrdering() {
        assertTrue(AppUpdateChecker.isNewerVersion("Release 1.2", "1.0"))
        assertTrue(AppUpdateChecker.isNewerVersion("80endance-v1.3.0", "1.2.9"))
        assertTrue(AppUpdateChecker.isNewerVersion("v1.2.0", "1.2.0-beta"))
        assertFalse(AppUpdateChecker.isNewerVersion("v1.2.0-beta", "1.2.0"))
    }

    @Test
    fun parseReleasesArrayJson_handlesEmptyPrereleaseAndUpToDate() {
        val noReleases = AppUpdateChecker.parseReleasesArrayJson("[]", currentVersion = "1.0")
        assertTrue(noReleases is UpdateCheckResult.NoReleases)

        val prereleaseOnlyJson = """
            [
              {
                "tag_name": "v1.3.0-beta1",
                "name": "Beta 1.3",
                "body": "Testing new update",
                "html_url": "https://github.com/thathsarayasiru97/80endence/releases/tag/v1.3.0-beta1",
                "draft": false,
                "prerelease": true
              }
            ]
        """.trimIndent()
        val preResult = AppUpdateChecker.parseReleasesArrayJson(prereleaseOnlyJson, currentVersion = "1.0")
        assertTrue(preResult is UpdateCheckResult.UpdateAvailable)
        val info = (preResult as UpdateCheckResult.UpdateAvailable).info
        assertEquals("v1.3.0-beta1", info.latestVersion)
        assertTrue(info.isPrerelease)

        val upToDateResult = AppUpdateChecker.parseReleasesArrayJson(prereleaseOnlyJson, currentVersion = "1.3.0")
        assertTrue(upToDateResult is UpdateCheckResult.UpToDate)
        assertEquals("v1.3.0-beta1", (upToDateResult as UpdateCheckResult.UpToDate).latestFoundTag)
    }
}
