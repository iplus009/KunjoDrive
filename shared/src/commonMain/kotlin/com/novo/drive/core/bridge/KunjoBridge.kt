package com.novo.drive.core.bridge

import kotlinx.coroutines.flow.StateFlow

/**
 * KunjoBridge: Central fault-isolated mediator connecting all subsystems.
 * Ensures zero tight-coupling: any failure in media, files or UI never halts cloud transfers.
 */
interface KunjoBridge {
    val transfer: TransferBridge
    val media: MediaBridge
    val security: SecurityBridge
    val storage: StorageBridge
}

interface TransferBridge {
    val activeUploadCount: StateFlow<Int>
    val activeDownloadCount: StateFlow<Int>
    val isNetworkAvailable: Boolean
    fun retryFailedTransfers()
    fun pauseAll()
    fun resumeAll()
}

interface MediaBridge {
    val currentAudio: StateFlow<AudioPlaybackState?>
    val isMiniPlayerVisible: StateFlow<Boolean>
    val audioBoostMultiplier: StateFlow<Float> // Up to 2.0x (200%)
    val audioSessionId: StateFlow<Int> // Hardware audio session ID for DSP/Equalizer
    val equalizerPreset: StateFlow<Int>
    val isShuffleEnabled: StateFlow<Boolean>
    val repeatMode: StateFlow<Int> // 0: off, 1: all, 2: one
    val playbackSpeed: StateFlow<Float>
    val queue: StateFlow<List<AudioTrackItem>>
    
    fun playAudio(fileId: String, title: String, artist: String? = null, streamUri: String? = null)
    fun playQueue(items: List<AudioTrackItem>, startIndex: Int = 0)
    fun pauseAudio()
    fun resumeAudio()
    fun seekAudio(positionMs: Long)
    fun stopAudio()
    fun skipNext()
    fun skipPrevious()
    fun toggleShuffle()
    fun cycleRepeatMode()
    fun setAudioBoost(multiplier: Float)
    fun setPlaybackVolume(volume: Float) {}
    fun applyEqualizerPreset(presetIndex: Int) {}
    fun setPlaybackSpeed(speed: Float) {}
    fun hideMiniPlayer()
}

data class AudioTrackItem(
    val fileId: String,
    val title: String,
    val artist: String? = null,
    val sizeBytes: Long = 0L,
    val mimeType: String? = null
)

data class AudioPlaybackState(
    val fileId: String,
    val title: String,
    val artist: String?,
    val durationMs: Long,
    val currentPositionMs: Long,
    val isPlaying: Boolean,
    val streamUri: String? = null,
    val sizeBytes: Long = 0L
)

interface SecurityBridge {
    val isVaultLocked: StateFlow<Boolean>
    val isGhostVaultActive: StateFlow<Boolean>
    fun verifyVaultKey(key: String): Boolean
    fun isAppTampered(): Boolean
}

interface StorageBridge {
    val usedBytes: StateFlow<Long>
    val totalQuotaBytes: StateFlow<Long>
    fun refreshStorageUsage()
}

