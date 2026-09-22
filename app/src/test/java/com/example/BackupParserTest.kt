package com.example

import com.example.data.backup.BackupParser
import com.example.data.model.Show
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupParserTest {

    @Test
    fun testLegacyArrayBackupStillParses() {
        val payload = BackupParser.parse(
            """[{"title":"Legacy Show","status":"Returning Series","watchedEpisodes":"1,2"}]"""
        )

        assertEquals(1, payload.shows.size)
        assertEquals("Legacy Show", payload.shows.single().title)
        assertEquals(emptyList<com.example.data.model.Setting>(), payload.settings)
    }

    @Test
    fun testUnsupportedVersionIsRejectedBeforeRestore() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupParser.parse(
                """{"version":999,"shows":[{"title":"Test"}],"settings":[]}"""
            )
        }
    }

    @Test
    fun testApiKeyIsNeverIncludedInParsedSettings() {
        val payload = BackupParser.parse(
            """{"version":2,"shows":[{"title":"Test"}],"settings":[{"key":"tmdb_key","value":"secret"},{"key":"display_mode","value":"dark"}]}"""
        )

        assertFalse(payload.settings.any { it.key == "tmdb_key" })
        assertEquals("display_mode", payload.settings.single().key)
    }

    @Test
    fun testBlankShowTitleIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupParser.parse(
                """{"version":2,"shows":[{"title":""}],"settings":[]}"""
            )
        }
    }

    @Test
    fun testExportIsVersionedAndExcludesApiKey() {
        val json = BackupParser.serialize(
            listOf(Show(title = "Test")),
            listOf(
                com.example.data.model.Setting("tmdb_key", "secret"),
                com.example.data.model.Setting("display_mode", "dark")
            )
        )

        assertTrue(json.contains("\"version\":2"))
        assertFalse(json.contains("secret"))
        assertTrue(json.contains("display_mode"))
    }
}
