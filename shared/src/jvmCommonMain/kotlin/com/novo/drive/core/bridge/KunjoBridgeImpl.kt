package com.novo.drive.core.bridge

import com.novo.drive.core.security.NativeSecurityBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class KunjoBridgeImpl(
    customMediaBridge: MediaBridge? = null,
    customSecurityBridge: SecurityBridge? = null,
    customTransferBridge: TransferBridge? = null,
    customStorageBridge: StorageBridge? = null
) : KunjoBridge {

    private val bridgeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val transfer: TransferBridge = customTransferBridge ?: DefaultTransferBridge()
    override val media: MediaBridge = customMediaBridge ?: DefaultMediaBridge()
    override val security: SecurityBridge = customSecurityBridge ?: DefaultSecurityBridge()
    override val storage: StorageBridge = customStorageBridge ?: DefaultStorageBridge()

    private class DefaultTransferBridge : TransferBridge {
        private val _uploads = MutableStateFlow(0)
        override val activeUploadCount: StateFlow<Int> = _uploads.asStateFlow()

        private val _downloads = MutableStateFlow(0)
        override val activeDownloadCount: StateFlow<Int> = _downloads.asStateFlow()

        override val isNetworkAvailable: Boolean = true
        override fun retryFailedTransfers() = Unit
        override fun pauseAll() = Unit
        override fun resumeAll() = Unit
    }

    private class DefaultMediaBridge : MediaBridge {
        private val _current = MutableStateFlow<AudioPlaybackState?>(null)
        override val currentAudio: StateFlow<AudioPlaybackState?> = _current.asStateFlow()

        private val _miniVisible = MutableStateFlow(false)
        override val isMiniPlayerVisible: StateFlow<Boolean> = _miniVisible.asStateFlow()

        private val _boost = MutableStateFlow(1.0f)
        override val audioBoostMultiplier: StateFlow<Float> = _boost.asStateFlow()

        private val _audioSessionId = MutableStateFlow(0)
        override val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()

        private val _equalizerPreset = MutableStateFlow(0)
        override val equalizerPreset: StateFlow<Int> = _equalizerPreset.asStateFlow()

        private val _shuffle = MutableStateFlow(false)
        override val isShuffleEnabled: StateFlow<Boolean> = _shuffle.asStateFlow()

        private val _repeat = MutableStateFlow(0)
        override val repeatMode: StateFlow<Int> = _repeat.asStateFlow()

        private val _speed = MutableStateFlow(1.0f)
        override val playbackSpeed: StateFlow<Float> = _speed.asStateFlow()

        private val _queue = MutableStateFlow<List<AudioTrackItem>>(emptyList())
        override val queue: StateFlow<List<AudioTrackItem>> = _queue.asStateFlow()

        override fun playAudio(fileId: String, title: String, artist: String?, streamUri: String?) {
            _current.value = AudioPlaybackState(
                fileId = fileId,
                title = title,
                artist = artist,
                durationMs = 0L,
                currentPositionMs = 0L,
                isPlaying = true,
                streamUri = streamUri
            )
            _miniVisible.value = true
        }

        override fun playQueue(items: List<AudioTrackItem>, startIndex: Int) {
            _queue.value = items
            val track = items.getOrNull(startIndex) ?: return
            playAudio(track.fileId, track.title, track.artist)
        }

        override fun pauseAudio() {
            _current.value = _current.value?.copy(isPlaying = false)
        }

        override fun resumeAudio() {
            _current.value = _current.value?.copy(isPlaying = true)
        }

        override fun seekAudio(positionMs: Long) {
            _current.value = _current.value?.copy(currentPositionMs = positionMs)
        }

        override fun stopAudio() {
            _current.value = null
            _miniVisible.value = false
        }

        override fun skipNext() {
            val q = _queue.value
            val cur = _current.value ?: return
            val idx = q.indexOfFirst { it.fileId == cur.fileId }
            if (idx != -1 && q.isNotEmpty()) {
                val nextIdx = (idx + 1) % q.size
                val next = q[nextIdx]
                playAudio(next.fileId, next.title, next.artist)
            }
        }

        override fun skipPrevious() {
            val q = _queue.value
            val cur = _current.value ?: return
            val idx = q.indexOfFirst { it.fileId == cur.fileId }
            if (idx != -1 && q.isNotEmpty()) {
                val prevIdx = (idx - 1 + q.size) % q.size
                val prev = q[prevIdx]
                playAudio(prev.fileId, prev.title, prev.artist)
            }
        }

        override fun toggleShuffle() {
            _shuffle.value = !_shuffle.value
        }

        override fun cycleRepeatMode() {
            _repeat.value = (_repeat.value + 1) % 3
        }

        override fun setAudioBoost(multiplier: Float) {
            _boost.value = multiplier.coerceIn(1.0f, 2.0f)
        }

        override fun applyEqualizerPreset(presetIndex: Int) {
            _equalizerPreset.value = presetIndex.coerceAtLeast(0)
        }

        override fun setPlaybackSpeed(speed: Float) {
            _speed.value = speed.coerceIn(0.25f, 3.0f)
        }

        override fun hideMiniPlayer() {
            _miniVisible.value = false
        }
    }

    private class DefaultSecurityBridge : SecurityBridge {
        private val _locked = MutableStateFlow(true)
        override val isVaultLocked: StateFlow<Boolean> = _locked.asStateFlow()

        private val _ghostActive = MutableStateFlow(false)
        override val isGhostVaultActive: StateFlow<Boolean> = _ghostActive.asStateFlow()

        // Fail closed when no real SecurityBridge delegate is bound
        override fun verifyVaultKey(key: String): Boolean = false
        override fun isAppTampered(): Boolean = !NativeSecurityBridge.isRuntimeIntegrityValid()
    }

    private class DefaultStorageBridge : StorageBridge {
        private val _used = MutableStateFlow(0L)
        override val usedBytes: StateFlow<Long> = _used.asStateFlow()

        private val _quota = MutableStateFlow(0L)
        override val totalQuotaBytes: StateFlow<Long> = _quota.asStateFlow()

        override fun refreshStorageUsage() = Unit
    }
}

