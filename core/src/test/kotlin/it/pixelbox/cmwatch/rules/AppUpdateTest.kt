package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** L'aggiornamento dentro l'app (P6, approvata da Franz il 21/10 alle 23:58): le release di GitHub e quando proporle. */
class AppUpdateTest {
    private val sha = "32dbc89859d7e5ea3fc7e4bb88aefc4232353c06d9b2c55ea13c9cc36fb2958d"
    private val older = "0000000000000000000000000000000000000000000000000000000000000001"

    // La forma della risposta di GET /repos/{owner}/{repo}/releases (campi in più ignorati), con dati inventati.
    private fun release(tag: String, at: String, digest: String? = "sha256:$sha", asset: String = "mobile-release.apk", draft: Boolean = false, pre: Boolean = true, name: String? = null) = """
        {"tag_name": "$tag", "name": ${name?.let { "\"$it\"" } ?: "null"}, "draft": $draft, "prerelease": $pre, "published_at": "$at", "body": "Note.\n",
         "html_url": "https://example.com/r/$tag",
         "assets": [{"name": "$asset", "size": 68813846, "digest": ${digest?.let { "\"$it\"" } ?: "null"}, "content_type": "application/vnd.android.package-archive",
                     "browser_download_url": "https://example.com/d/$tag/$asset"}]}
    """.trimIndent()
    private val body = "[${release("b-2026-10-22", "2026-10-22T01:10:00Z", name = "Notte 22/10")}, ${release("a-2026-10-21", "2026-10-21T19:51:20Z", digest = "sha256:$older")}]"
    private val t21 = 1792612280L   // 2026-10-21T19:51:20Z
    private val t22 = t21 + 5 * 3600 + 18 * 60 + 40   // 2026-10-22T01:10:00Z

    @Test fun parsesTheReleasesNewestFirst() {
        val r = AppUpdate.parse(body)
        assertEquals(listOf("b-2026-10-22", "a-2026-10-21"), r.map { it.tag })
        assertEquals(listOf("Notte 22/10", "a-2026-10-21"), r.map { it.name })
        assertEquals(t22, r[0].publishedAt)
        assertEquals("https://example.com/d/b-2026-10-22/mobile-release.apk", r[0].url)
        assertEquals(68813846L, r[0].size)
        assertEquals(sha, r[0].sha256)
        assertEquals(older, r[1].sha256)
        assertEquals("Note.", r[0].notes)
        assertTrue(r[0].prerelease)
    }

    @Test fun draftsAndReleasesWithoutThePhoneApkAreSkipped() {
        val b = "[${release("d", "2026-10-22T01:10:00Z", draft = true)}, ${release("w", "2026-10-22T01:00:00Z", asset = "wear-release.apk")}, ${release("ok", "2026-10-21T19:51:20Z")}]"
        assertEquals(listOf("ok"), AppUpdate.parse(b).map { it.tag })
        assertEquals(emptyList<AppUpdate.Release>(), AppUpdate.parse("not json"))
        assertEquals(emptyList<AppUpdate.Release>(), AppUpdate.parse("[]"))
        assertEquals(emptyList<AppUpdate.Release>(), AppUpdate.parse("{\"message\": \"API rate limit exceeded\"}"))
    }

    @Test fun theDigestIsSha256OrNothing() {
        assertEquals(sha, AppUpdate.sha256Of("sha256:$sha"))
        assertEquals(sha, AppUpdate.sha256Of(" SHA256:${sha.uppercase()} "))
        assertNull(AppUpdate.sha256Of("md5:abc"))
        assertNull(AppUpdate.sha256Of("sha256:abc"))
        assertNull(AppUpdate.sha256Of(null))
        assertNull(AppUpdate.parse("[${release("x", "2026-10-22T01:10:00Z", digest = null)}]")[0].sha256)
    }

    @Test fun offersTheNewestPublishedAfterTheInstall() {
        val r = AppUpdate.parse(body)
        // Installato ieri sera l'APK vecchio: la release di stanotte è un aggiornamento.
        assertEquals("b-2026-10-22", AppUpdate.offer(r, AppUpdate.Installed(older, t21 + 600))?.tag)
        // Senza ancora il digest dell'installato conta la data.
        assertEquals("b-2026-10-22", AppUpdate.offer(r, AppUpdate.Installed(null, t21 + 600))?.tag)
        assertNull(AppUpdate.offer(emptyList(), AppUpdate.Installed(null, 0)))
    }

    @Test fun theSameApkOrANewerInstallIsNotAnUpdate() {
        val r = AppUpdate.parse(body)
        // La release di stanotte installata via ADB dopo la pubblicazione: stesso digest, niente da proporre.
        assertNull(AppUpdate.offer(r, AppUpdate.Installed(sha, t22 + 1800)))
        // Lo stesso APK ma installato prima che GitHub lo pubblicasse (la CI lo aveva già dato): stesso digest, niente.
        assertNull(AppUpdate.offer(r, AppUpdate.Installed(sha, t22 - 600)))
        // Una build di sviluppo messa sul telefono dopo l'ultima release non torna indietro.
        assertNull(AppUpdate.offer(r, AppUpdate.Installed("ffff", t22 + 60)))
        // Installazione nello stesso istante della pubblicazione: non è dopo.
        assertNull(AppUpdate.offer(r, AppUpdate.Installed(null, t22)))
    }

    @Test fun notNowSkipsThatTagUntilANewOne() {
        val r = AppUpdate.parse(body)
        val inst = AppUpdate.Installed(older, t21 + 600)
        assertNull(AppUpdate.offer(r, inst, dismissed = "b-2026-10-22"))
        assertEquals("b-2026-10-22", AppUpdate.offer(r, inst, dismissed = "a-2026-10-21")?.tag)
    }

    @Test fun theDownloadIsGoodOnlyWithTheRightDigestAndSize() {
        val r = AppUpdate.parse(body)[0]
        assertTrue(AppUpdate.verified(r, sha.uppercase(), 68813846))
        assertFalse(AppUpdate.verified(r, older, 68813846))
        assertFalse(AppUpdate.verified(r, sha, 68813845))
        assertFalse(AppUpdate.verified(r.copy(sha256 = null), sha, 68813846))
        assertTrue(AppUpdate.verified(r.copy(size = 0), sha, 1))
    }

    @Test fun checksEveryHalfHourAndAlwaysTheFirstTime() {
        assertTrue(AppUpdate.due(0, 100))
        assertFalse(AppUpdate.due(1000, 1000 + 4 * 60))
        assertTrue(AppUpdate.due(1000, 1000 + 5 * 60))
    }

    @Test fun labelsAndFileNames() {
        assertEquals("68 MB", AppUpdate.sizeLabel(68813846))
        assertEquals("1,5 MB", AppUpdate.sizeLabel(1_550_000))
        assertEquals("950 kB", AppUpdate.sizeLabel(950_000))
        assertEquals("impegni-2026-10-22.apk", AppUpdate.fileName("impegni-2026-10-22"))
        assertEquals("v0.2_rc_1.apk", AppUpdate.fileName("v0.2/rc 1"))
    }
}
