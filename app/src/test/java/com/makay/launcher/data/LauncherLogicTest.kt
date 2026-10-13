package com.makay.launcher.data

import com.makay.launcher.domain.WorkspaceItem
import com.makay.launcher.domain.WorkspaceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LauncherLogicTest {
    @Test
    fun bumpPatch_followsSemanticPatch() {
        assertEquals("1.0.7" to 8, Versioning.bumpPatch("1.0.6", 7))
        assertEquals("1.0.8" to 9, Versioning.bumpPatch("1.0.7", 8))
        assertEquals("1.0.1" to 2, Versioning.bumpPatch("1.0", 1))
    }

    @Test
    fun updateChecker_parsesValidManifest() {
        val offer = UpdateChecker.parse(
            """{"versionName":"1.0.9","versionCode":10,"apkUrl":"https://example.com/a.apk","notes":"fix","sha256":"AB"}""",
        )
        assertEquals("1.0.9", offer?.versionName)
        assertEquals(10, offer?.versionCode)
        assertEquals("https://example.com/a.apk", offer?.apkUrl)
        assertEquals("ab", offer?.sha256)
    }

    @Test
    fun updateChecker_rejectsInvalidManifest() {
        assertNull(UpdateChecker.parse("""{"versionName":"","versionCode":0}"""))
    }

    @Test
    fun githubReleaseUrl_matchesRepo() {
        assertEquals(
            "https://github.com/makaydestek/Makay-Lanucher/releases/download/1.0.8/MakayLauncher-1.0.8.apk",
            HttpGet.apkReleaseUrl("1.0.8"),
        )
    }

    @Test
    fun favoriteOrder_movesAndRoundtrips() {
        val keys = listOf("a/1", "b/2", "c/3")
        assertEquals(listOf("b/2", "a/1", "c/3"), FavoriteOrder.move(keys, "a/1", 1))
        assertEquals(keys, FavoriteOrder.decode(FavoriteOrder.encode(keys)))
    }

    @Test
    fun apkHasher_parsesSidecarAndMatches() {
        assertEquals("abc123", ApkHasher.parseSidecar("ABC123  MakayLauncher-1.0.9.apk\n"))
        assertTrue(ApkHasher.matches("a".repeat(64), "A".repeat(64)))
        assertFalse(ApkHasher.matches("a".repeat(64), "b".repeat(64)))
        val file = File.createTempFile("makay", ".apk")
        file.writeBytes(byteArrayOf(1, 2, 3, 4))
        val hash = ApkHasher.sha256(file)
        assertEquals(64, hash.length)
        assertTrue(ApkHasher.matches(hash, hash))
        file.delete()
    }

    @Test
    fun workspaceEngine_rejectsOverlapAndMakesFolder() {
        val a = WorkspaceItem(id = "a", type = WorkspaceType.APP, page = 0, cellX = 0, cellY = 0, appKey = "pkg/a")
        val b = WorkspaceItem(id = "b", type = WorkspaceType.APP, page = 0, cellX = 1, cellY = 0, appKey = "pkg/b")
        assertFalse(WorkspaceEngine.fits(listOf(a), 0, 0, 0, 2, 1, "b", 4, 5))
        val merged = WorkspaceEngine.drop(listOf(a, b), "b", 0, 0, 0, 4, 5)
        assertEquals(1, merged.size)
        assertEquals(WorkspaceType.FOLDER, merged.first().type)
        assertEquals(2, merged.first().members.size)
        val free = WorkspaceEngine.firstFree(listOf(a), 0, 4, 5, 1, 1)
        assertEquals(1 to 0, free)
    }

    @Test
    fun workspaceEngine_reflowMovesOverlappingItemToFreeCell() {
        val first = WorkspaceItem(id = "first", type = WorkspaceType.APP, page = 0, cellX = 0, cellY = 0, appKey = "pkg/first")
        val second = WorkspaceItem(id = "second", type = WorkspaceType.APP, page = 0, cellX = 0, cellY = 0, appKey = "pkg/second")

        val result = WorkspaceEngine.reflow(listOf(first, second), columns = 4, rows = 5)

        assertEquals(2, result.size)
        assertEquals(0 to 0, result.first { it.id == "first" }.cellX to result.first { it.id == "first" }.cellY)
        assertEquals(1 to 0, result.first { it.id == "second" }.cellX to result.first { it.id == "second" }.cellY)
    }

    @Test
    fun workspaceEngine_reflowClampsItemSpanToGridSize() {
        val oversized = WorkspaceItem(
            id = "widget",
            type = WorkspaceType.WIDGET,
            page = 0,
            cellX = 0,
            cellY = 0,
            spanX = 9,
            spanY = 9,
        )

        val result = WorkspaceEngine.reflow(listOf(oversized), columns = 4, rows = 5).single()

        assertEquals(4, result.spanX)
        assertEquals(5, result.spanY)
    }
}
