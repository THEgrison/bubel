package fr.russki.app.data.online

import fr.russki.app.data.online.api.RemoteJson
import fr.russki.app.data.online.download.PathSafety
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PathSafetyTest {

    @Test
    fun rejectsTraversalAndAbsolutePaths() {
        assertFalse(PathSafety.isSafeRelativePath("../etc/passwd"))
        assertFalse(PathSafety.isSafeRelativePath("/etc/passwd"))
        assertFalse(PathSafety.isSafeRelativePath("a/../../b"))
        assertFalse(PathSafety.isSafeRelativePath("a/b/../c"))
        assertFalse(PathSafety.isSafeAssetId("../x"))
        assertFalse(PathSafety.isSafeAssetId("a/b"))
        assertTrue(PathSafety.isSafeRelativePath("ogg/travel-001.ogg"))
        assertTrue(PathSafety.isSafeAssetId("audio-travel-001"))
    }

    @Test
    fun resolveUnder_blocksEscape() {
        val root = File(System.getProperty("java.io.tmpdir"), "russki-path-safety").apply {
            deleteRecursively()
            mkdirs()
        }
        assertNull(PathSafety.resolveUnder(root, "../escape.bin"))
        assertNotNull(PathSafety.resolveUnder(root, "ogg/ok.ogg"))
    }

    @Test
    fun remoteJson_rejectsTraversalInManifest() {
        val raw = """
            {
              "packId":"evil",
              "version":"1",
              "titleFr":"t",
              "totalBytes":5,
              "assets":[{
                "id":"a1",
                "relativePath":"../../evil.ogg",
                "byteSize":5,
                "sha256":"2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                "downloadPath":"/a"
              }]
            }
        """.trimIndent()
        val parsed = RemoteJson.parseManifest(raw)
        assertTrue(parsed is OnlineResult.Err)
        assertEquals(
            OnlineError.INVALID_PAYLOAD,
            (parsed as OnlineResult.Err).error.code,
        )
    }
}
