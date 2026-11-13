package com.novo.drive.core.media

import android.content.Context
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.novo.drive.core.bridge.AudioPlaybackState
import com.novo.drive.core.bridge.AudioTrackItem
import com.novo.drive.core.bridge.MediaBridge
import com.novo.drive.core.common.SafeLog
import com.novo.drive.core.dispatchers.DispatcherProvider
import com.novo.drive.data.local.dao.FilePartDao
import com.novo.drive.domain.model.DriveFile
import com.novo.drive.domain.repository.FileRepository
import com.novo.drive.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

/**
 * AndroidIplusAudioPlayer: Hardware-accelerated audio engine for Kunjo / iPlus Drive.
 * Connects ExoPlayer with Telegram streaming data sources, local storage, hardware Equalizer +
 * LoudnessEnhancer DSP, volume fading, and Sub-Account stream policy checks.
 */
@UnstableApi
class AndroidIplusAudioPlayer(
    private val context: Context,
    private val fileRepository: FileRepository,
    private val filePartDao: FilePartDao,
    private val dataSourceFactory: TelegramDataSourceFactory,
    private val settingsRepository: SettingsRepository,
    private val dispatchers: DispatcherProvider
) : MediaBridge {

    private val playerScope = CoroutineScope(SupervisorJob() + dispatchers.main)

    private val _currentAudio = MutableStateFlow<AudioPlaybackState?>(null)
    override val currentAudio: StateFlow<AudioPlaybackState?> = _currentAudio.asStateFlow()

    private val _isMiniPlayerVisible = MutableStateFlow(false)
    override val isMiniPlayerVisible: StateFlow<Boolean> = _isMiniPlayerVisible.asStateFlow()

    private val _audioBoostMultiplier = MutableStateFlow(1.0f)
    override val audioBoostMultiplier: StateFlow<Float> = _audioBoostMultiplier.asStateFlow()

    private val _audioSessionId = MutableStateFlow(0)
    override val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()

    private val _equalizerPreset = MutableStateFlow(0)
    override val equalizerPreset: StateFlow<Int> = _equalizerPreset.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    override val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(0)
    override val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    override val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _queue = MutableStateFlow<List<AudioTrackItem>>(emptyList())
    override val queue: StateFlow<List<AudioTrackItem>> = _queue.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null
    private var progressTickerJob: Job? = null

    init {
        playerScope.launch {
            initPlayer()
        }
    }

    private fun initPlayer() {
        if (exoPlayer != null) return
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setSkipSilenceEnabled(true)
            .build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _currentAudio.value = _currentAudio.value?.copy(isPlaying = isPlaying)
                if (isPlaying) {
                    startTicker()
                } else {
                    stopTicker()
                }
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != 0) {
                    _audioSessionId.value = audioSessionId
                    setupLoudnessEnhancer(audioSessionId)
                    setupEqualizer(audioSessionId)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        val duration = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
                        if (duration > 0L) {
                            _currentAudio.value = _currentAudio.value?.copy(durationMs = duration)
                        }
                    }
                    Player.STATE_ENDED -> {
                        when (_repeatMode.value) {
                            2 -> {
                                exoPlayer?.seekTo(0L)
                                exoPlayer?.play()
                            }
                            else -> {
                                skipNext()
                            }
                        }
                    }
                    else -> Unit
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                SafeLog.e("AndroidIplusAudioPlayer", "ExoPlayer error: ${error.message}", error)
                _currentAudio.value = _currentAudio.value?.copy(isPlaying = false)
            }
        })

        exoPlayer = player
    }

    private fun setupLoudnessEnhancer(sessionId: Int) {
        runCatching {
            loudnessEnhancer?.release()
            loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                val boostGain = ((_audioBoostMultiplier.value - 1.0f) * 1000).toInt().coerceAtLeast(0)
                setTargetGain(boostGain)
                enabled = boostGain > 0
            }
        }
    }

    private fun setupEqualizer(sessionId: Int) {
        runCatching {
            equalizer?.release()
            equalizer = Equalizer(0, sessionId).apply {
                enabled = true
                applyPresetInternal(this, _equalizerPreset.value)
            }
        }
    }

    private fun applyPresetInternal(eq: Equalizer, presetIndex: Int) {
        val count = eq.numberOfPresets.toInt()
        if (count > 0 && presetIndex in 0 until count) {
            eq.usePreset(presetIndex.toShort())
            return
        }
        val bands = eq.numberOfBands.toInt()
        if (bands <= 0) return
        val range = eq.bandLevelRange
        val minLvl = range[0].toInt()
        val maxLvl = range[1].toInt()
        val curve = when (presetIndex) {
            1 -> floatArrayOf(0.85f, 0.7f, 0.5f, 0.5f, 0.55f) // Bass boost
            2 -> floatArrayOf(0.42f, 0.55f, 0.78f, 0.74f, 0.52f) // Vocal
            3 -> floatArrayOf(0.76f, 0.60f, 0.45f, 0.65f, 0.80f) // Rock
            4 -> floatArrayOf(0.60f, 0.72f, 0.64f, 0.68f, 0.62f) // Pop
            else -> floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f) // Flat
        }
        for (band in 0 until bands) {
            val weight = curve[band.coerceAtMost(curve.lastIndex)]
            val target = (minLvl + (maxLvl - minLvl) * weight).toInt().coerceIn(minLvl, maxLvl)
            eq.setBandLevel(band.toShort(), target.toShort())
        }
    }

    private fun startTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = playerScope.launch {
            while (isActive) {
                val player = exoPlayer
                if (player != null && player.isPlaying) {
                    val currentPos = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration.coerceAtLeast(0L)
                    _currentAudio.value = _currentAudio.value?.copy(
                        currentPositionMs = currentPos,
                        durationMs = if (dur > 0L) dur else (_currentAudio.value?.durationMs ?: 0L),
                        isPlaying = true
                    )
                }
                delay(250L)
            }
        }
    }

    private fun stopTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = null
    }

    override fun playAudio(fileId: String, title: String, artist: String?, streamUri: String?) {
        playerScope.launch {
            _isMiniPlayerVisible.value = true
            val file = withContext(dispatchers.io) {
                fileRepository.getFile(fileId)
            }
            playTrackInternal(fileId, title, artist, file, streamUri)
        }
    }

    override fun playQueue(items: List<AudioTrackItem>, startIndex: Int) {
        _queue.value = items
        val track = items.getOrNull(startIndex) ?: return
        playAudio(track.fileId, track.title, track.artist)
    }

    private suspend fun playTrackInternal(
        fileId: String,
        title: String,
        artist: String?,
        file: DriveFile?,
        fallbackStreamUri: String?
    ) {
        val prefs = withContext(dispatchers.io) { settingsRepository.preferences.first() }
        val localPath = file?.localPath
        val hasLocalFile = localPath != null && (localPath.startsWith("content://") || File(localPath).exists())
        val sizeBytes = file?.sizeBytes ?: 0L
        if (!hasLocalFile && prefs.kunjoAccountType.equals("SUB", ignoreCase = true) && prefs.kunjoMaxStreamSizeMb > 0) {
            val limitBytes = prefs.kunjoMaxStreamSizeMb.toLong() * 1024L * 1024L
            if (sizeBytes > limitBytes) {
                withContext(dispatchers.main) {
                    exoPlayer?.pause()
                    _currentAudio.value = _currentAudio.value?.copy(isPlaying = false)
                    stopTicker()
                }
                SafeLog.w("AndroidIplusAudioPlayer", "Blocked SUB stream for $fileId exceeding ${prefs.kunjoMaxStreamSizeMb} MB limit")
                return
            }
        }

        withContext(dispatchers.main) {
            initPlayer()
            val player = exoPlayer ?: return@withContext
            player.volume = 1.0f

            val initialDuration = file?.durationMs?.takeIf { it > 0 } ?: 0L

            _currentAudio.value = AudioPlaybackState(
                fileId = fileId,
                title = title,
                artist = artist,
                durationMs = initialDuration,
                currentPositionMs = 0L,
                isPlaying = true,
                streamUri = fallbackStreamUri,
                sizeBytes = sizeBytes
            )

            try {
                val remoteFileId = file?.remoteFileId
                when {
                    hasLocalFile -> {
                        val uri = if (localPath.startsWith("content://")) {
                            Uri.parse(localPath)
                        } else {
                            Uri.fromFile(File(localPath))
                        }
                        player.setMediaItem(MediaItem.fromUri(uri))
                    }
                    file?.hasRemoteCopy == true && remoteFileId != null -> {
                        val parts = withContext(dispatchers.io) {
                            filePartDao.partsOf(file.id)
                        }
                        val factory = if (parts.isNotEmpty()) {
                            val mediaParts = parts.mapNotNull { p ->
                                p.remoteFileId?.let { MediaPart(it, p.plainOffset, p.plainSize) }
                            }
                            dataSourceFactory.createParted(
                                parts = mediaParts,
                                encrypted = file.isEncrypted
                            )
                        } else {
                            dataSourceFactory.create(remoteFileId)
                        }
                        val source = ProgressiveMediaSource.Factory(factory)
                            .createMediaSource(MediaItem.fromUri("telegram://stream/$fileId"))
                        player.setMediaSource(source)
                    }
                    fallbackStreamUri != null -> {
                        player.setMediaItem(MediaItem.fromUri(Uri.parse(fallbackStreamUri)))
                    }
                    else -> {
                        SafeLog.w("AndroidIplusAudioPlayer", "No playable media found for file $fileId")
                        _currentAudio.value = _currentAudio.value?.copy(isPlaying = false)
                        return@withContext
                    }
                }
                player.prepare()
                player.playWhenReady = true
                startTicker()
            } catch (e: Exception) {
                SafeLog.e("AndroidIplusAudioPlayer", "Error preparing playback: ${e.message}", e)
                _currentAudio.value = _currentAudio.value?.copy(isPlaying = false)
            }
        }
    }

    override fun pauseAudio() {
        playerScope.launch(dispatchers.main) {
            exoPlayer?.pause()
            _currentAudio.value = _currentAudio.value?.copy(isPlaying = false)
            stopTicker()
        }
    }

    override fun resumeAudio() {
        playerScope.launch(dispatchers.main) {
            exoPlayer?.volume = 1.0f
            exoPlayer?.play()
            _currentAudio.value = _currentAudio.value?.copy(isPlaying = true)
            startTicker()
        }
    }

    override fun seekAudio(positionMs: Long) {
        playerScope.launch(dispatchers.main) {
            val validPos = positionMs.coerceAtLeast(0L)
            exoPlayer?.seekTo(validPos)
            _currentAudio.value = _currentAudio.value?.copy(currentPositionMs = validPos)
        }
    }

    override fun stopAudio() {
        playerScope.launch(dispatchers.main) {
            exoPlayer?.stop()
            _currentAudio.value = null
            _isMiniPlayerVisible.value = false
            stopTicker()
        }
    }

    override fun skipNext() {
        val q = _queue.value
        if (q.isEmpty()) return
        val current = _currentAudio.value
        val currentIndex = q.indexOfFirst { it.fileId == current?.fileId }

        val nextIndex = if (_isShuffleEnabled.value && q.size > 1) {
            var r: Int
            do {
                r = Random.nextInt(q.size)
            } while (r == currentIndex)
            r
        } else {
            (currentIndex + 1) % q.size
        }

        if (currentIndex == q.size - 1 && _repeatMode.value == 0 && !_isShuffleEnabled.value) {
            pauseAudio()
            return
        }

        val nextTrack = q[nextIndex]
        playAudio(nextTrack.fileId, nextTrack.title, nextTrack.artist)
    }

    override fun skipPrevious() {
        val q = _queue.value
        val current = _currentAudio.value

        if ((current?.currentPositionMs ?: 0L) > 3000L) {
            seekAudio(0L)
            return
        }

        if (q.isEmpty()) return
        val currentIndex = q.indexOfFirst { it.fileId == current?.fileId }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else q.size - 1
        val prevTrack = q[prevIndex]
        playAudio(prevTrack.fileId, prevTrack.title, prevTrack.artist)
    }

    override fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
    }

    override fun cycleRepeatMode() {
        _repeatMode.value = (_repeatMode.value + 1) % 3
    }

    override fun setAudioBoost(multiplier: Float) {
        val clamped = multiplier.coerceIn(1.0f, 2.0f)
        _audioBoostMultiplier.value = clamped
        val gainMb = ((clamped - 1.0f) * 1000).toInt().coerceAtLeast(0)
        runCatching {
            loudnessEnhancer?.setTargetGain(gainMb)
            loudnessEnhancer?.enabled = gainMb > 0
        }
    }

    override fun setPlaybackVolume(volume: Float) {
        playerScope.launch(dispatchers.main) {
            exoPlayer?.volume = volume.coerceIn(0f, 1f)
        }
    }

    override fun applyEqualizerPreset(presetIndex: Int) {
        val clamped = presetIndex.coerceAtLeast(0)
        _equalizerPreset.value = clamped
        playerScope.launch(dispatchers.main) {
            val eq = equalizer ?: return@launch
            runCatching { applyPresetInternal(eq, clamped) }
        }
    }

    override fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 3.0f)
        _playbackSpeed.value = clamped
        playerScope.launch(dispatchers.main) {
            exoPlayer?.setPlaybackSpeed(clamped)
        }
    }

    override fun hideMiniPlayer() {
        _isMiniPlayerVisible.value = false
    }

    fun release() {
        playerScope.launch(dispatchers.main) {
            stopTicker()
            runCatching { loudnessEnhancer?.release() }
            runCatching { equalizer?.release() }
            runCatching { exoPlayer?.release() }
            loudnessEnhancer = null
            equalizer = null
            exoPlayer = null
        }
    }
}

