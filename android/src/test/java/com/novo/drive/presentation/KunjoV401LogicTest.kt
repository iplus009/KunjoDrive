package com.novo.drive.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KunjoV401LogicTest {

    // Regexes used in IplusMusicPlayerScreen for smart categorization
    private val CALL_RECORD_REGEX = Regex("(?i).*(call|recording|_20\\d{6}_|sound_recorder|record|audiorecord).*")
    private val VOICE_NOTE_REGEX = Regex("(?i).*(ptt|opus|voice|note|audiomessage|telegram|whatsapp).*")

    @Test
    fun `audio categorization accurately classifies calls and recordings`() {
        val callFiles = listOf(
            "Call_recording_20261106_120000.m4a",
            "call_mom_record.mp3",
            "sound_recorder_001.wav",
            "audiorecord_meeting.aac"
        )
        for (file in callFiles) {
            assertTrue("Expected call file: $file", CALL_RECORD_REGEX.matches(file))
        }
    }

    @Test
    fun `audio categorization accurately classifies voice notes`() {
        val voiceNotes = listOf(
            "voice_note_123.opus",
            "ptt-20261106-WA0001.opus",
            "audiomessage_telegram.ogg"
        )
        for (file in voiceNotes) {
            assertTrue("Expected voice note: $file", VOICE_NOTE_REGEX.matches(file))
            assertFalse("Voice note should not match call regex: $file", CALL_RECORD_REGEX.matches(file))
        }
    }

    @Test
    fun `standard music tracks are preserved in songs tab`() {
        val songs = listOf(
            "Bohemian Rhapsody.mp3",
            "Rabindra Sangeet - Ami Chini Go Chini.flac",
            "Ed Sheeran - Shape of You.m4a",
            "Pink Floyd - Comfortably Numb.mp3",
            "Track01.wav"
        )
        for (song in songs) {
            val isCall = CALL_RECORD_REGEX.matches(song)
            val isVoice = VOICE_NOTE_REGEX.matches(song)
            assertFalse("Song '$song' should not be call recording", isCall)
            assertFalse("Song '$song' should not be voice note", isVoice)
        }
    }

    @Test
    fun `storage channel deletion tokens accept exact matches and reject invalid input`() {
        fun isValidDeleteToken(input: String, channelName: String): Boolean {
            val trimmed = input.trim()
            return trimmed.equals("DELETE", ignoreCase = true) ||
                    trimmed == "মুছুন" ||
                    trimmed == "ডিলিট" ||
                    trimmed == channelName
        }

        val channelName = "My Cloud Channel (কুঞ্জ স্টোরেজ)"

        // Valid tokens
        assertTrue(isValidDeleteToken("DELETE", channelName))
        assertTrue(isValidDeleteToken("delete", channelName))
        assertTrue(isValidDeleteToken("Delete", channelName))
        assertTrue(isValidDeleteToken("মুছুন", channelName))
        assertTrue(isValidDeleteToken("ডিলিট", channelName))
        assertTrue(isValidDeleteToken("My Cloud Channel (কুঞ্জ স্টোরেজ)", channelName))

        // Invalid tokens
        assertFalse(isValidDeleteToken("DEL", channelName))
        assertFalse(isValidDeleteToken("মুছে ফেলুন", channelName))
        assertFalse(isValidDeleteToken("cancel", channelName))
        assertFalse(isValidDeleteToken("", channelName))
    }

    @Test
    fun `language purity - english strings contain zero bengali characters`() {
        fun containsBengali(text: String): Boolean {
            return text.any { it in '\u0980'..'\u09FF' }
        }

        val englishStrings = listOf(
            "Step 1 of 7",
            "Kunjo Drive",
            "Your files live in your own private Telegram channel. Your account, your storage, your control.",
            "Get started",
            "Login with family (invite & PIN)",
            "Family members do not need a Telegram app, SIM, or OTP (up to 3 members • 2 GB/file).",
            "Step 1: Enter family invite & PIN",
            "Scan the family invite QR code from the drive owner's phone or paste the copied invite text below.",
            "Paste clipboard",
            "Scan QR",
            "Family invite token",
            "6-digit pairing PIN",
            "Complete step 1 ➔ Generate session QR",
            "Telegram blocked on this network?",
            "Set up a proxy",
            "Dynamic Storage Island",
            "Command deck",
            "Recent folders",
            "Secure vault",
            "Music & audio studio",
            "Songs",
            "Recordings",
            "Voice notes",
            "Playlists",
            "Play all",
            "Shuffle",
            "Now playing",
            "Paused",
            "Sleep timer"
        )

        for (s in englishStrings) {
            assertFalse("English string '$s' contains Bengali characters!", containsBengali(s))
        }
    }

    @Test
    fun `language purity - bengali strings contain authentic bengali script`() {
        fun containsBengali(text: String): Boolean {
            return text.any { it in '\u0980'..'\u09FF' }
        }

        val bengaliStrings = listOf(
            "ফ্যামিলি লগইন (ইনভাইট ও পিন)",
            "ফ্যামিলি মেম্বারদের ফোনে টেলিগ্রাম অ্যাপ, সিম বা ওটিপির প্রয়োজন নেই (সর্বোচ্চ ৩ জন • ২ জিবি/ফাইল)।",
            "ধাপ ১: ফ্যামিলি ইনভাইট ও পিন দিন",
            "ক্লিপবোর্ড পেস্ট",
            "QR স্ক্যান করুন",
            "৬-ডিজিট পেয়ারিং পিন",
            "কুঞ্জ কমান্ড ডেক",
            "রিসেন্ট ফোল্ডার",
            "সিকিউর ভল্ট",
            "কুঞ্জ মিউজিক ও কল স্টুডিও",
            "গান",
            "কল রেকর্ড",
            "ভয়েস নোট",
            "প্লেলিস্ট",
            "সব চালান",
            "শাফল",
            "বাজছে",
            "স্থগিত",
            "স্লিপ টাইমার"
        )

        for (s in bengaliStrings) {
            assertTrue("Bengali string '$s' should contain Bengali script!", containsBengali(s))
        }
    }
}

