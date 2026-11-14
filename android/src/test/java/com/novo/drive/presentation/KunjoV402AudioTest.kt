package com.novo.drive.presentation

import com.novo.drive.core.bridge.AudioPlaybackState
import com.novo.drive.core.bridge.AudioTrackItem
import com.novo.drive.core.bridge.KunjoBridgeImpl
import com.novo.drive.domain.model.AppLanguage
import com.novo.drive.domain.model.FileCategory
import com.novo.drive.presentation.gallery.GalleryTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * KunjoV402AudioTest: Comprehensive Unit Verification for Kunjo v4.0.2.
 * Verifies audio queue progression, repeat cycling, shuffle, audio boost,
 * and language resolution integrity.
 */
class KunjoV402AudioTest {

    @Test
    fun testAudioBridgeQueueProgression() {
        val bridge = KunjoBridgeImpl()
        val tracks = listOf(
            AudioTrackItem(fileId = "track_1", title = "Song A", artist = "Artist A", sizeBytes = 5_000_000L),
            AudioTrackItem(fileId = "track_2", title = "Song B", artist = "Artist B", sizeBytes = 6_000_000L),
            AudioTrackItem(fileId = "track_3", title = "Song C", artist = "Artist C", sizeBytes = 7_000_000L)
        )

        bridge.media.playQueue(tracks, 0)
        val initial = bridge.media.currentAudio.value
        assertNotNull(initial)
        assertEquals("track_1", initial?.fileId)
        assertEquals("Song A", initial?.title)
        assertTrue(initial?.isPlaying == true)

        // Advance to next
        bridge.media.skipNext()
        assertEquals("track_2", bridge.media.currentAudio.value?.fileId)

        // Advance to next again
        bridge.media.skipNext()
        assertEquals("track_3", bridge.media.currentAudio.value?.fileId)

        // Loop around
        bridge.media.skipNext()
        assertEquals("track_1", bridge.media.currentAudio.value?.fileId)

        // Go back
        bridge.media.skipPrevious()
        assertEquals("track_3", bridge.media.currentAudio.value?.fileId)
    }

    @Test
    fun testShuffleAndRepeatModes() {
        val bridge = KunjoBridgeImpl()
        assertFalse(bridge.media.isShuffleEnabled.value)
        assertEquals(0, bridge.media.repeatMode.value)

        bridge.media.toggleShuffle()
        assertTrue(bridge.media.isShuffleEnabled.value)

        bridge.media.toggleShuffle()
        assertFalse(bridge.media.isShuffleEnabled.value)

        // Cycle repeat: 0 -> 1 -> 2 -> 0
        bridge.media.cycleRepeatMode()
        assertEquals(1, bridge.media.repeatMode.value) // Repeat All
        bridge.media.cycleRepeatMode()
        assertEquals(2, bridge.media.repeatMode.value) // Repeat One
        bridge.media.cycleRepeatMode()
        assertEquals(0, bridge.media.repeatMode.value) // Off
    }

    @Test
    fun testAudioBoostClamping() {
        val bridge = KunjoBridgeImpl()
        assertEquals(1.0f, bridge.media.audioBoostMultiplier.value, 0.001f)

        bridge.media.setAudioBoost(1.5f)
        assertEquals(1.5f, bridge.media.audioBoostMultiplier.value, 0.001f)

        // Above max 2.0x
        bridge.media.setAudioBoost(2.8f)
        assertEquals(2.0f, bridge.media.audioBoostMultiplier.value, 0.001f)

        // Below min 1.0x
        bridge.media.setAudioBoost(0.5f)
        assertEquals(1.0f, bridge.media.audioBoostMultiplier.value, 0.001f)
    }

    @Test
    fun testAudioTimeFormatting() {
        fun formatMs(ms: Long): String {
            val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
            val minutes = totalSeconds / 60L
            val seconds = totalSeconds % 60L
            return "$minutes:${seconds.toString().padStart(2, '0')}"
        }

        assertEquals("0:00", formatMs(0L))
        assertEquals("0:42", formatMs(42_000L))
        assertEquals("3:42", formatMs(222_000L))
        assertEquals("10:05", formatMs(605_000L))
    }

    @Test
    fun testGalleryTabsEnumeration() {
        val tabs = GalleryTab.entries
        assertEquals(4, tabs.size)
        assertEquals(GalleryTab.ALL, tabs[0])
        assertEquals(GalleryTab.PHOTOS, tabs[1])
        assertEquals(GalleryTab.VIDEOS, tabs[2])
        assertEquals(GalleryTab.ALBUMS, tabs[3])
    }

    @Test
    fun testAppLanguageCodeMapping() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("en"))
        assertEquals(AppLanguage.BENGALI, AppLanguage.fromCode("bn"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromCode("system"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode(null))
    }
}

